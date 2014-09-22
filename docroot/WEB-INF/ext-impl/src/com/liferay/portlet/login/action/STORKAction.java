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
 * *********************************************************************
 */
package com.liferay.portlet.login.action;

import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.util.FedPropsKeys;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.security.auth.PrincipalException;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.struts.PortletAction;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.FedPropsValues;
import com.liferay.portal.util.FedWebKeys;
import com.liferay.portal.util.LDAPUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.PrefsPropsUtil;
import com.liferay.portal.util.STORKUtil;
import com.liferay.portlet.ActionResponseImpl;
import com.liferay.portlet.login.action.stork.STORKException;
import eu.stork.peps.auth.commons.IPersonalAttributeList;
import eu.stork.peps.auth.commons.PEPSUtil;
import eu.stork.peps.auth.commons.PersonalAttribute;
import eu.stork.peps.auth.commons.PersonalAttributeList;
import eu.stork.peps.auth.commons.STORKAuthnRequest;
import eu.stork.peps.auth.commons.STORKAuthnResponse;
import eu.stork.peps.auth.engine.STORKSAMLEngine;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.PortletURL;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.servlet.http.HttpSession;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 *
 * @author Marco Fargetta <marco.fargetta@ct.infn.it>
 */
public class STORKAction extends PortletAction {

    private static Log _log = LogFactoryUtil.getLog(STORKAction.class);
    private static final long serialVersionUID = 3660074009157921579L;

    @Override
    public ActionForward render(ActionMapping mapping, ActionForm form, PortletConfig portletConfig, RenderRequest renderRequest, RenderResponse renderResponse) throws Exception {
        ThemeDisplay themeDisplay = (ThemeDisplay) renderRequest.getAttribute(
                WebKeys.THEME_DISPLAY);

        _log.debug("Selected STORK Authentication");
        if (!STORKUtil.isEnabled(themeDisplay.getCompanyId())) {
            _log.warn("STORK Authentication with STORK disabled");
            return mapping.findForward("portlet.login.login");
        }

        renderResponse.setTitle(themeDisplay.translate("stork"));
        _log.debug("Setting the forward: " + getForward(renderRequest, "portlet.login.stork.country"));
        return mapping.findForward(getForward(renderRequest, "portlet.login.stork.country"));
    }

    @Override
    public void processAction(ActionMapping mapping, ActionForm form, PortletConfig portletConfig, ActionRequest actionRequest, ActionResponse actionResponse) throws Exception {
        ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(
                WebKeys.THEME_DISPLAY);
        long companyId = themeDisplay.getCompanyId();

        if (!STORKUtil.isEnabled(companyId)) {
            throw new PrincipalException();
        }

        String storkMandatoryAttr = PrefsPropsUtil.getString(companyId, FedPropsKeys.STORK_AUTH_LOCAL_SEARCH_FILTER, FedPropsValues.STORK_AUTH_LOCAL_SEARCH_FILTER);
        Map<String, String> storkUserMapping = getAttrMap(PrefsPropsUtil.getString(companyId, FedPropsKeys.STORK_USER_MAPPING));

        if (ParamUtil.getString(actionRequest, "StorkAction", "none").equals("login")) {
            byte[] decSamlToken = PEPSUtil.decodeSAMLToken(ParamUtil.getString(actionRequest, "SAMLResponse"));
            STORKSAMLEngine storkEngine = STORKSAMLEngine.getInstance("SP");
            STORKAuthnResponse authnResponse = null;
            try {
                authnResponse = storkEngine.validateSTORKAuthnResponse(decSamlToken, PortalUtil.getHttpServletRequest(actionRequest).getRemoteHost());
            } catch (Exception ex) {
                _log.error("Could not validate token for Saml Response");
                _log.error(ex);
                setForward(actionRequest, "portlet.login.stork.error");
                return;
            }
            _log.debug("Authentication response status: "+authnResponse.getStatusCode()+"(reason: "+authnResponse.getSubStatusCode()+") and this is a fail: "+authnResponse.isFail());
            if (!authnResponse.isFail()) {
                Map<String, PersonalAttribute> mPersAttr = createPersonalAttributeMap(authnResponse.getPersonalAttributeList().values());

                User user = null;
                if (storkMandatoryAttr.equals("screenName")) {
                    _log.debug("Finding user using the " + storkUserMapping.get("screenName"));
                    if (mPersAttr.containsKey(storkUserMapping.get("screenName"))) {
                        Iterator<String> pa = mPersAttr.get(storkUserMapping.get("screenName")).getValue().iterator();

                        while (pa.hasNext() && user == null) {
                            String screenName = pa.next();
                            try {
                                user = UserLocalServiceUtil.getUserByScreenName(companyId, screenName);
                            } catch (NoSuchUserException nse) {
                                _log.info("User screenName: " + screenName + " is not registered");
                            }
                        }
                    } else {
                        actionResponse.sendRedirect(PrefsPropsUtil.getString(companyId, FedPropsKeys.STORK_AUTH_PAGE_MISS_ATTRIBUTE, FedPropsValues.STORK_AUTH_PAGE_MISS_ATTRIBUTE));
                        _log.info("Stork authentication miss the matching attribute. Impossible to identify users");
                        return;
                    }
                }
                if (storkMandatoryAttr.equals("uuid")) {
                    _log.debug("Finding user using the " + storkUserMapping.get("uuid"));
                    if (mPersAttr.containsKey(storkUserMapping.get("uuid"))) {
                        Iterator<String> pa = mPersAttr.get(storkUserMapping.get("uuid")).getValue().iterator();

                        while (pa.hasNext() && user == null) {
                            String uuid = pa.next();
                            try {
                                user = UserLocalServiceUtil.getUserByUuid(uuid);
                            } catch (NoSuchUserException nse) {
                                _log.info("User uuid: " + uuid + " is not registered");
                            }
                        }
                    } else {
                        actionResponse.sendRedirect(PrefsPropsUtil.getString(companyId, FedPropsKeys.STORK_AUTH_PAGE_MISS_ATTRIBUTE, FedPropsValues.STORK_AUTH_PAGE_MISS_ATTRIBUTE));
                        _log.info("Stork authentication miss the matching attribute. Impossible to identify users");
                        return;
                    }

                }
                if (storkMandatoryAttr.equals("emailAddress")) {
                    _log.debug("Finding user using the " + storkUserMapping.get("emailAddress"));
                    if (mPersAttr.containsKey(storkUserMapping.get("emailAddress"))) {
                        Iterator<String> pa = mPersAttr.get(storkUserMapping.get("emailAddress")).getValue().iterator();

                        while (pa.hasNext() && user == null) {
                            Pattern pat = Pattern.compile("[\\w\\-]([\\.\\w\\-])+@([\\w\\-]+\\.)+[a-zA-Z]{2,4}");
                            Matcher mailMatch;

                            mailMatch = pat.matcher(pa.next());
                            while (mailMatch.find() && user == null) {
                                if (Validator.isNotNull(mailMatch.group())) {
                                    try {
                                        user = UserLocalServiceUtil.getUserByEmailAddress(companyId, mailMatch.group());
                                    } catch (NoSuchUserException nse) {
                                        _log.info("Mail: " + mailMatch.group() + " is not registered");
                                    }
                                }
                            }

                        }
                    } else {
                        actionResponse.sendRedirect(PrefsPropsUtil.getString(companyId, FedPropsKeys.STORK_AUTH_PAGE_MISS_ATTRIBUTE, FedPropsValues.STORK_AUTH_PAGE_MISS_ATTRIBUTE));
                        _log.info("Stork authentication miss the matching attribute. Impossible to identify users");
                        return;
                    }

                }

                if (user == null && PrefsPropsUtil.getBoolean(companyId, FedPropsKeys.STORK_AUTH_LDLAP_CHECK, FedPropsValues.STORK_AUTH_LDLAP_CHECK)) {
                    _log.debug("User not found, check on LDAP");
//                    user=getUserFromLdap();


                    String originalLdapFilter = PrefsPropsUtil.getString(themeDisplay.getCompanyId(), FedPropsKeys.STORK_AUTH_LDAP_SEARCH_FILTER, FedPropsValues.STORK_AUTH_LDAP_SEARCH_FILTER);
                    List<String> lstLdapFilter= null;
                    try{
                        lstLdapFilter = generateFilters(companyId, mPersAttr.get(storkUserMapping.get("screenName")), mPersAttr.get(storkUserMapping.get("emailAddress")), mPersAttr.get(storkUserMapping.get("firstName")), mPersAttr.get(storkUserMapping.get("lastName")), originalLdapFilter);
                    }
                    catch(STORKException se){
                        _log.error(se.getMessage());
                        actionResponse.sendRedirect(PrefsPropsUtil.getString(companyId, FedPropsKeys.STORK_AUTH_PAGE_MISS_ATTRIBUTE, FedPropsValues.STORK_AUTH_PAGE_MISS_ATTRIBUTE));
                        return;
                    }
                    String[] idLDAPS = PrefsPropsUtil.getStringArray(companyId, "ldap.server.ids", ",");
                    String idLDAP;
                    int idLDAPCounter = 0;
                    while (user == null && idLDAPCounter < idLDAPS.length) {
                        idLDAP= idLDAPS[idLDAPCounter++];

                        String mailMap = null;
                        String userMaps[] = PrefsPropsUtil.getString(companyId, PropsKeys.LDAP_USER_MAPPINGS + "." + idLDAP).split("\n");
                        int mIndex = 0;
                        while (mailMap == null && mIndex < userMaps.length) {
                            String map = userMaps[mIndex++];
                            if (map.indexOf("=") == -1 || map.split("=").length != 2) {
                                continue;
                            }
                            String[] sMap = map.split("=");
                            if (sMap[0].equals("emailAddress")) {
                                mailMap = sMap[1];
                            }
                        }

                        if (mailMap == null) {
                            _log.warn("LDAP server configured without the mail map");
                            continue;
                        }



                        LDAPUtil samlLdapUtil = new LDAPUtil(
                                PrefsPropsUtil.getString(companyId, PropsKeys.LDAP_BASE_PROVIDER_URL + "." + idLDAP),
                                PrefsPropsUtil.getString(companyId, PropsKeys.LDAP_BASE_DN + "." + idLDAP));

                        Iterator<String> ldapFilter= lstLdapFilter.iterator();
                        
                        while(ldapFilter.hasNext() && user==null){
                            String mail = samlLdapUtil.getUserAttribute(
                                    PrefsPropsUtil.getString(companyId, PropsKeys.LDAP_IMPORT_USER_SEARCH_FILTER + "." + idLDAP),
                                    ldapFilter.next(),
                                    mailMap);

                            if (mail != null) {
                                try {
                                    user = UserLocalServiceUtil.getUserByEmailAddress(companyId, mail);
                                } catch (NoSuchUserException nse) {
                                    _log.debug("Mail: " + mail + " found in LDAP but it is not registered");
                                }
                            }
                        }
                    }

                }




                if (user == null) {
                    _log.info("Impossible to find a user with the current attributes");
                    actionResponse.sendRedirect(PrefsPropsUtil.getString(themeDisplay.getCompanyId(), FedPropsKeys.STORK_AUTH_PAGE_MISS_USER, FedPropsValues.STORK_AUTH_PAGE_MISS_USER));
                    return;
                }


                HttpSession session = PortalUtil.getHttpServletRequest(actionRequest).getSession();
                session.setAttribute(FedWebKeys.STORK_ID_LOGIN, new Long(user.getUserId()));

                sendRedirect(actionRequest, actionResponse, PortalUtil.getPortalURL(actionRequest) + themeDisplay.getURLSignIn());
            } else {
                setForward(actionRequest, "portlet.login.stork.notAuth");
            }

        } else {

            if (Validator.isNull(ParamUtil.getString(actionRequest, "citizenCountry"))) {
                SessionErrors.add(actionRequest, "missUserCountry");
                return;
            }
            STORKAuthnRequest authnRequest = new STORKAuthnRequest();

            authnRequest.setCitizenCountryCode(ParamUtil.getString(actionRequest, "citizenCountry"));

            authnRequest.setIssuer(PrefsPropsUtil.getString(themeDisplay.getCompanyId(), FedPropsKeys.STORK_SP_NAME));

            authnRequest.setDestination(PrefsPropsUtil.getString(themeDisplay.getCompanyId(), FedPropsKeys.STORK_SPEPS_URL));

            authnRequest.setProviderName(PrefsPropsUtil.getString(themeDisplay.getCompanyId(), FedPropsKeys.STORK_SP_NAME));

            authnRequest.setQaa(PrefsPropsUtil.getInteger(themeDisplay.getCompanyId(), FedPropsKeys.STORK_SP_QAALEVEL));

            ActionResponseImpl actionResponseImpl = (ActionResponseImpl) actionResponse;
            PortletURL portletURL = actionResponseImpl.createActionURL();
            portletURL.setParameter("struts_action", "/login/stork");
            portletURL.setParameter("StorkAction", "login");
            portletURL.setParameter("saveLastPath", "0");


            authnRequest.setAssertionConsumerServiceURL(portletURL.toString());
            _log.debug("STORK Return url: " + portletURL.toString());

            authnRequest.setSpSector(PrefsPropsUtil.getString(themeDisplay.getCompanyId(), FedPropsKeys.STORK_SP_SECTOR));

            authnRequest.setSpInstitution(PrefsPropsUtil.getString(themeDisplay.getCompanyId(), FedPropsKeys.STORK_SP_NAME));

            authnRequest.setSpApplication(PrefsPropsUtil.getString(themeDisplay.getCompanyId(), FedPropsKeys.STORK_SP_APLICATION));

            authnRequest.setSpCountry(PrefsPropsUtil.getString(themeDisplay.getCompanyId(), FedPropsKeys.STORK_SP_COUNTRY));

            authnRequest.setSPID(PrefsPropsUtil.getString(themeDisplay.getCompanyId(), FedPropsKeys.STORK_SP_NAME));


            IPersonalAttributeList pAttList = new PersonalAttributeList();

            boolean eIdentifier=false;
            if (storkUserMapping != null) {

                for (String attrMap : storkUserMapping.keySet()) {
                    PersonalAttribute attr = new PersonalAttribute();
                    attr.setName(storkUserMapping.get(attrMap));
                    if (attrMap.equals(storkMandatoryAttr) && storkUserMapping.get(attrMap).equals("eIdentifier")) {
                        attr.setIsRequired(true);
                        eIdentifier=true;
                        _log.debug("Attribute " + attrMap + " mapped in " + storkUserMapping.get(attrMap) + " is required");
                    } else {
                        if (attrMap.equals(storkMandatoryAttr)) {
                            attr.setIsRequired(true);
                            _log.debug("Attribute " + attrMap + " mapped in " + storkUserMapping.get(attrMap) + " is required");
                        } else {
                            attr.setIsRequired(false);
                            _log.debug("Attribute " + attrMap + " mapped in " + storkUserMapping.get(attrMap) + " is not required");

                        }
                    }
                    pAttList.add(attr);
                }
                if(!eIdentifier){
                    pAttList.add(new PersonalAttribute("eIdentifier",true,null,null));
                }
            }

            authnRequest.setPersonalAttributeList(pAttList);

            byte token[] = null;
            try {

                STORKSAMLEngine storkEngine = STORKSAMLEngine.getInstance("SP");
                token = storkEngine.generateSTORKAuthnRequest(authnRequest).getTokenSaml();

            } catch (Exception ex) {
                _log.error("Impossible to create the SAML token");
                _log.error(ex);
                setForward(actionRequest, "portlet.login.stork.error");
            }

            if (token != null) {
                actionResponse.setRenderParameter("SAMLToken", PEPSUtil.encodeSAMLToken(token));
                actionResponse.setRenderParameter("CCountry", ParamUtil.getString(actionRequest, "citizenCountry"));
                actionResponse.setRenderParameter("PEPSUrl", PrefsPropsUtil.getString(themeDisplay.getCompanyId(), FedPropsKeys.STORK_SPEPS_URL));
                setForward(actionRequest, "portlet.login.stork.peps");
            }
        }
    }

    /**
     *
     * @param map The key is the system attribute reference name and the value
     * is the STORK attribute name
     * @return
     */
    private Map<String, String> getAttrMap(String map) {
        if (map == null) {
            return null;
        }

        Map<String, String> attrMap = new HashMap<String, String>();

        for (String attr : map.split("\n")) {
            if (attr.indexOf("=") == -1) {
                continue;
            }

            String strAttrMapA[] = attr.split("=");
            if (strAttrMapA.length != 2) {
                continue;
            }

            attrMap.put(strAttrMapA[0], strAttrMapA[1]);
        }
        return attrMap;

    }

    private Map<String, PersonalAttribute> createPersonalAttributeMap(Collection<PersonalAttribute> values) {
        Map<String, PersonalAttribute> attrMap = new HashMap<String, PersonalAttribute>();

        for (PersonalAttribute pa : values) {
            attrMap.put(pa.getName(), pa);
        }

        return attrMap;
    }

    private List<String> generateFilters(long companyId, PersonalAttribute screnName, PersonalAttribute mail, PersonalAttribute firstName, PersonalAttribute lastName, String originalLdapFilter) throws STORKException{
        ArrayList<String> filters= new ArrayList<String>();
        
        String ldapFilter = originalLdapFilter.replaceAll("@company_id@", Long.toString(companyId));

        if(ldapFilter.contains("@screen_name@")){
            if (screnName==null || screnName.isEmptyValue()){
                throw new STORKException("Impossible to generate the LDAP Filter, screenname attribute missed");
            }
            else{
                for(String sName: screnName.getValue()){
                    filters.add(ldapFilter.replaceAll("@screen_name@", sName));
                }
            }
        }
        
        if(ldapFilter.contains("@first_name@")){
            if (firstName==null || firstName.isEmptyValue()){
                throw new STORKException("Impossible to generate the LDAP Filter, firstname attribute missed");
            }
            else{
                List<String> tmpFilters= filters;
                filters= new ArrayList<String>();

                for(String fName: firstName.getValue()){
                    for(String tmpFil: tmpFilters){
                        filters.add(tmpFil.replaceAll("@first_name@", fName));
                    }
                }
            }
        }
        if(ldapFilter.contains("@last_name@")){
            if(lastName==null || lastName.isEmptyValue()){
                throw new STORKException("Impossible to generate the LDAP Filter, lastname attribute missed");
            }
            else{
                List<String> tmpFilters= filters;
                filters= new ArrayList<String>();

                for(String lName: lastName.getValue()){
                    for(String tmpFil: tmpFilters){
                        filters.add(tmpFil.replaceAll("@last_name@", lName));
                    }
                }
            }
        }
        
        if(ldapFilter.contains("@email_address@")){
            if(mail==null || mail.isEmptyValue()){
                throw new STORKException("Impossible to generate the LDAP Filter, emailaddress attribute missed");
            }
            else{


                List<String> tmpFilters= filters;
                filters= new ArrayList<String>();

                for(String uMail: mail.getValue()){
                    Pattern pat = Pattern.compile("[\\w\\-]([\\.\\w\\-])+@([\\w\\-]+\\.)+[a-zA-Z]{2,4}");
                    Matcher mailMatch;
                    mailMatch = pat.matcher(uMail);

                    while (mailMatch.find()) {
                        for(String tmpFil: tmpFilters){
                            filters.add(tmpFil.replaceAll("@email_address@", mailMatch.group()));
                        }
                    }
                }
            }
        }
        return filters;
    }
}
