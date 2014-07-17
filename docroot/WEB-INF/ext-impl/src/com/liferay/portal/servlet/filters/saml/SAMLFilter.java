/**
 * *********************************************************************
 * Copyright (c) 2011: Istituto Nazionale di Fisica Nucleare (INFN), Italy
 * Consorzio COMETA (COMETA), Italy
 *
 * See http://www.infn.it and and http://www.consorzio-cometa.it for details on
 * the copyright holders.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 **********************************************************************
 */
package com.liferay.portal.servlet.filters.saml;

import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.servlet.filters.BasePortalFilter;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.FedPropsKeys;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.User;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.util.FedPropsValues;
import com.liferay.portal.util.FedWebKeys;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.PrefsPropsUtil;
import com.liferay.portal.util.SAMLUtil;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 * @author Marco Fargetta <marco.fargetta@ct.infn.it>
 */
public class SAMLFilter extends BasePortalFilter {

    private static Log _log = LogFactoryUtil.getLog(SAMLFilter.class);

    @Override
    protected void processFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws Exception {

        long companyId = PortalUtil.getCompanyId(request);
        _log.debug("Process for company: "+companyId);

        String logoutUrl = PrefsPropsUtil.getString(
                companyId, FedPropsKeys.SAML_AUTH_PAGE_EXIT,
                FedPropsValues.SAML_AUTH_PAGE_EXIT);

        String requestURI = GetterUtil.getString(request.getRequestURI());

        if (requestURI.endsWith("/portal/logout")) {
            _log.debug("Performing logout");
            HttpSession session = request.getSession();

            if (session.getAttribute(FedWebKeys.SAML_ID_LOGIN) != null) {
                session.invalidate();
                _log.debug("User send to SAML logoout");
                response.sendRedirect(logoutUrl);
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        _log.debug("Check if the url "+requestURI+" end with "+SAMLUtil.getAuthURL(companyId));
        if (requestURI.endsWith(SAMLUtil.getAuthURL(companyId))) {
            User user=null;
            try {
                String localAttributeMatch = PrefsPropsUtil.getString(companyId, FedPropsKeys.SAML_AUTH_LOCAL_SEARCH_FILTER, FedPropsValues.SAML_AUTH_LOCAL_SEARCH_FILTER);
                _log.debug("localAttributeMatch: "+localAttributeMatch);
                
                String samlUserMapping = PrefsPropsUtil.getString(companyId, FedPropsKeys.SAML_USER_MAPPING);
                _log.debug("samlUserMapping: "+samlUserMapping);

                
                String[] samlUserMappingArray;

                if (samlUserMapping != null) {
                    samlUserMappingArray = samlUserMapping.split("\n");

                    int i = 0;
                    while (i < samlUserMappingArray.length && user==null) {
                        if (samlUserMappingArray[i].indexOf("=") == -1) {
                            continue;
                        }

                        String mapping[] = samlUserMappingArray[i].split("=");

                        if (mapping.length != 2) {
                            continue;
                        }
                        _log.debug("Check for attribute: "+mapping[1]);

                        if (mapping[0].equals("screenName") && localAttributeMatch.equals("screenname")) {
                            if(request.getAttribute(mapping[1])!=null){
                                _log.debug("Checking for the mapping: "+mapping[0]);
                                user= UserLocalServiceUtil.getUserByScreenName(companyId, (String) request.getAttribute(mapping[1]));
                            }
                            else{
                                response.sendRedirect(PrefsPropsUtil.getString(companyId, FedPropsKeys.SAML_AUTH_PAGE_MISS_ATTRIBUTE, FedPropsValues.SAML_AUTH_PAGE_MISS_ATTRIBUTE));
                                return;
                            }
                        } else if (mapping[0].equals("uuid") && localAttributeMatch.equals("uuid")) {
                            if(request.getAttribute(mapping[1])!=null){
                                _log.debug("Checking for the mapping: "+mapping[0]);
                                user= UserLocalServiceUtil.getUserByUuid((String) request.getAttribute(mapping[1]));
                            }
                            else{
                                response.sendRedirect(PrefsPropsUtil.getString(companyId, FedPropsKeys.SAML_AUTH_PAGE_MISS_ATTRIBUTE, FedPropsValues.SAML_AUTH_PAGE_MISS_ATTRIBUTE));
                                return;
                            }
                        } else if (mapping[0].equals("emailAddress") && localAttributeMatch.equals("mail")) {
                            if(request.getAttribute(mapping[1])!=null){
                                _log.debug("Checking for the mapping: "+mapping[0]);

                                Pattern pat = Pattern.compile("[\\w\\-]([\\.\\w\\-])+@([\\w\\-]+\\.)+[a-zA-Z]{2,4}");
                                Matcher mailMatch;

                                mailMatch= pat.matcher((String) request.getAttribute(mapping[1]));
                                while(mailMatch.find() && user==null){
                                    if (Validator.isNotNull(mailMatch.group())) {
                                        try{
                                            user = UserLocalServiceUtil.getUserByEmailAddress(companyId, mailMatch.group());
                                        }
                                        catch(NoSuchUserException nse){
                                            _log.info("Mail: "+mailMatch.group()+" is not registered");
                                        }
                                    }
                                }
                            }
                            else{
                                response.sendRedirect(PrefsPropsUtil.getString(companyId, FedPropsKeys.SAML_AUTH_PAGE_MISS_ATTRIBUTE, FedPropsValues.SAML_AUTH_PAGE_MISS_ATTRIBUTE));
                                return;
                            }
                        }

                        mapping[1] = "";
                        i++;
                    }
                }

                if(user==null && PrefsPropsUtil.getBoolean(companyId, FedPropsKeys.SAML_AUTH_LDLAP_CHECK, FedPropsValues.SAML_AUTH_LDLAP_CHECK)){
                    _log.debug("User not found, check on LDAP");
//                    user=getUserFromLdap();
                }

                if (user==null) {
                    _log.info("Impossible to find a user with the current attributes");
                    response.sendRedirect(PrefsPropsUtil.getString(companyId, FedPropsKeys.SAML_AUTH_PAGE_MISS_USER, FedPropsValues.SAML_AUTH_PAGE_MISS_ATTRIBUTE));
                    return;
                }

                HttpSession session = request.getSession();
                session.setAttribute(FedWebKeys.SAML_ID_LOGIN, new Long(user.getUserId()));
            } catch (SystemException ex) {
                _log.error(ex);
            } catch (PortalException ex) {
                _log.error(ex);
            }


            String redirect = HttpUtil.decodeURL(ParamUtil.getString(request, "redirectSAML", "/"));
            _log.debug("Filter redirect to: " + redirect);

            response.sendRedirect(redirect);

        }

    }

    @Override
    protected Log getLog() {
        return _log;
    }

    @Override
    public boolean isFilterEnabled(HttpServletRequest request, HttpServletResponse response) {
        long companyId = PortalUtil.getCompanyId(request);

        boolean enabled = false;
        try {
            enabled = PrefsPropsUtil.getBoolean(
                    companyId, FedPropsKeys.SAML_AUTH_ENABLED,
                    FedPropsValues.SAML_AUTH_ENABLED);
        } catch (SystemException ex) {
            _log.error(ex);
        }

        _log.debug("SAML Filter enabled: "+enabled);
        return enabled;
    }
}
