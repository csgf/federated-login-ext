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
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.security.auth.PrincipalException;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.struts.PortletAction;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.FedPropsValues;
import com.liferay.portal.util.FedWebKeys;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.PrefsPropsUtil;
import com.liferay.portal.util.STORKUtil;
import com.liferay.portlet.ActionResponseImpl;
import eu.stork.peps.auth.commons.IPersonalAttributeList;
import eu.stork.peps.auth.commons.PEPSUtil;
import eu.stork.peps.auth.commons.PersonalAttribute;
import eu.stork.peps.auth.commons.PersonalAttributeList;
import eu.stork.peps.auth.commons.STORKAuthnRequest;
import eu.stork.peps.auth.commons.STORKAuthnResponse;
import eu.stork.peps.auth.engine.STORKSAMLEngine;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
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

        if (!STORKUtil.isEnabled(themeDisplay.getCompanyId())) {
            throw new PrincipalException();
        }

        String storkMandatoryAttr = PrefsPropsUtil.getString(themeDisplay.getCompanyId(), FedPropsKeys.STORK_AUTH_LOCAL_SEARCH_FILTER, FedPropsValues.STORK_AUTH_LOCAL_SEARCH_FILTER);
        Map<String, String> storkUserMapping = getAttrMap(PrefsPropsUtil.getString(themeDisplay.getCompanyId(), FedPropsKeys.STORK_USER_MAPPING));

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
            if (!authnResponse.isFail()) {
                Map<String, PersonalAttribute> mPersAttr = createPersonalAttributeMap(authnResponse.getPersonalAttributeList().values());

                User user=null;
                if(storkMandatoryAttr.equals("screenName")){
                    _log.debug("Finding user using the "+storkUserMapping.get("screenName"));
                    if(mPersAttr.containsKey(storkUserMapping.get("screenName"))){
                        Iterator<String> pa= mPersAttr.get(storkUserMapping.get("screenName")).getValue().iterator();

                        while(pa.hasNext() && user==null){
                            String screenName=pa.next();
                            try{
                                user= UserLocalServiceUtil.getUserByScreenName(themeDisplay.getCompanyId(), screenName);
                            }
                            catch(NoSuchUserException nse){
                                _log.info("User screenName: "+screenName+" is not registered");
                            }
                        }
                    }
                    else{
                        actionResponse.sendRedirect(PrefsPropsUtil.getString(themeDisplay.getCompanyId(), FedPropsKeys.STORK_AUTH_PAGE_MISS_ATTRIBUTE, FedPropsValues.STORK_AUTH_PAGE_MISS_ATTRIBUTE));
                        _log.info("Stork authentication miss the matching attribute. Impossible to identify users");
                        return;
                    }
                }
                if(storkMandatoryAttr.equals("uuid")){
                    _log.debug("Finding user using the "+storkUserMapping.get("uuid"));
                    if(mPersAttr.containsKey(storkUserMapping.get("uuid"))){
                        Iterator<String> pa= mPersAttr.get(storkUserMapping.get("uuid")).getValue().iterator();

                        while(pa.hasNext() && user==null){
                            String uuid=pa.next();
                            try{
                                user= UserLocalServiceUtil.getUserByUuid(uuid);
                            }
                            catch(NoSuchUserException nse){
                                _log.info("User uuid: "+uuid+" is not registered");
                            }
                        }
                    }
                    else{
                        actionResponse.sendRedirect(PrefsPropsUtil.getString(themeDisplay.getCompanyId(), FedPropsKeys.STORK_AUTH_PAGE_MISS_ATTRIBUTE, FedPropsValues.STORK_AUTH_PAGE_MISS_ATTRIBUTE));
                        _log.info("Stork authentication miss the matching attribute. Impossible to identify users");
                        return;
                    }
                    
                }
                if(storkMandatoryAttr.equals("emailAddress")){
                    _log.debug("Finding user using the "+storkUserMapping.get("emailAddress"));
                     if(mPersAttr.containsKey(storkUserMapping.get("emailAddress"))){
                        Iterator<String> pa= mPersAttr.get(storkUserMapping.get("emailAddress")).getValue().iterator();

                        while(pa.hasNext() && user==null){
                            Pattern pat = Pattern.compile("[\\w\\-]([\\.\\w\\-])+@([\\w\\-]+\\.)+[a-zA-Z]{2,4}");
                            Matcher mailMatch;

                            mailMatch= pat.matcher(pa.next());
                            while(mailMatch.find() && user==null){
                                if (Validator.isNotNull(mailMatch.group())) {
                                    try{
                                        user = UserLocalServiceUtil.getUserByEmailAddress(themeDisplay.getCompanyId(), mailMatch.group());
                                    }
                                    catch(NoSuchUserException nse){
                                        _log.info("Mail: "+mailMatch.group()+" is not registered");
                                    }
                                }
                            }

                        }
                    }
                    else{
                        actionResponse.sendRedirect(PrefsPropsUtil.getString(themeDisplay.getCompanyId(), FedPropsKeys.STORK_AUTH_PAGE_MISS_ATTRIBUTE, FedPropsValues.STORK_AUTH_PAGE_MISS_ATTRIBUTE));
                        _log.info("Stork authentication miss the matching attribute. Impossible to identify users");
                        return;
                    }
                    
                }
                
                if(user==null && PrefsPropsUtil.getBoolean(themeDisplay.getCompanyId(), FedPropsKeys.STORK_AUTH_LDLAP_CHECK, FedPropsValues.STORK_AUTH_LDLAP_CHECK)){
                    
                }
                
                
                
                
                if (user==null) {
                    _log.info("Impossible to find a user with the current attributes");
                    actionResponse.sendRedirect(PrefsPropsUtil.getString(themeDisplay.getCompanyId(), FedPropsKeys.STORK_AUTH_PAGE_MISS_USER, FedPropsValues.STORK_AUTH_PAGE_MISS_USER));
                    return;
                }
                
                
                HttpSession session= PortalUtil.getHttpServletRequest(actionRequest).getSession();
                session.setAttribute(FedWebKeys.STORK_ID_LOGIN, new Long(user.getUserId()));
                
                sendRedirect(actionRequest, actionResponse, PortalUtil.getPortalURL(actionRequest)+themeDisplay.getURLSignIn());
            } else {
                setForward(actionRequest, "portlet.login.stork.error");
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

            if (storkUserMapping != null) {

                for (String attrMap : storkUserMapping.keySet()) {
                    PersonalAttribute attr = new PersonalAttribute();
                    attr.setName(storkUserMapping.get(attrMap));
                    if (attrMap.equals(storkMandatoryAttr)) {
                        attr.setIsRequired(true);
                        _log.debug("Attribute " + attrMap + " mapped in " + storkUserMapping.get(attrMap) + " is required");
                    } else {
                        if (attrMap.equals(storkMandatoryAttr)) {
                            attr.setIsRequired(true);
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
                    }
                    pAttList.add(attr);
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
        
        for(PersonalAttribute pa: values){
            attrMap.put(pa.getName(), pa);
        }
        
        return attrMap;
    }
}
