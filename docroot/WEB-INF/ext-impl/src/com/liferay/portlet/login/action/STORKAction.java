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
package com.liferay.portlet.login.action;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.util.FedPropsKeys;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.struts.PortletAction;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.FedPropsValues;
import com.liferay.portal.util.PrefsPropsUtil;
import com.liferay.portal.util.STORKUtil;
import com.liferay.portlet.ActionResponseImpl;
import eu.stork.peps.auth.commons.IPersonalAttributeList;
import eu.stork.peps.auth.commons.PEPSUtil;
import eu.stork.peps.auth.commons.PersonalAttribute;
import eu.stork.peps.auth.commons.PersonalAttributeList;
import eu.stork.peps.auth.commons.STORKAuthnRequest;
import eu.stork.peps.auth.engine.STORKSAMLEngine;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.PortletURL;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
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
        _log.debug("Setting the forward: "+getForward(renderRequest,"portlet.login.stork.country"));
        return mapping.findForward(getForward(renderRequest,"portlet.login.stork.country"));
    }

    @Override
    public void processAction(ActionMapping mapping, ActionForm form, PortletConfig portletConfig, ActionRequest actionRequest, ActionResponse actionResponse) throws Exception {
        if(Validator.isNull(ParamUtil.getString(actionRequest, "citizenCountry"))){
            SessionErrors.add(actionRequest, "missUserCountry");
            return;
        }
        
        ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(
                        WebKeys.THEME_DISPLAY);
        
        STORKAuthnRequest authnRequest= new STORKAuthnRequest();

        authnRequest.setCitizenCountryCode(ParamUtil.getString(actionRequest, "citizenCountry"));
        
        authnRequest.setIssuer(PrefsPropsUtil.getString(themeDisplay.getCompanyId(), FedPropsKeys.STORK_SP_NAME));

        authnRequest.setDestination(PrefsPropsUtil.getString(themeDisplay.getCompanyId(), FedPropsKeys.STORK_SPEPS_URL));
        
        authnRequest.setProviderName(PrefsPropsUtil.getString(themeDisplay.getCompanyId(), FedPropsKeys.STORK_SP_NAME));

        authnRequest.setQaa(PrefsPropsUtil.getInteger(themeDisplay.getCompanyId(), FedPropsKeys.STORK_SP_QAALEVEL));

        ActionResponseImpl actionResponseImpl = (ActionResponseImpl) actionResponse;
        PortletURL portletURL = actionResponseImpl.createActionURL();
        portletURL.setParameter("struts_action", "/login/open_id");
        portletURL.setParameter("StorkAction", "login");
        portletURL.setParameter("saveLastPath", "0");

        
        authnRequest.setAssertionConsumerServiceURL(portletURL.toString());
        
        authnRequest.setSpSector(PrefsPropsUtil.getString(themeDisplay.getCompanyId(), FedPropsKeys.STORK_SP_SECTOR));
        
        authnRequest.setSpInstitution(PrefsPropsUtil.getString(themeDisplay.getCompanyId(), FedPropsKeys.STORK_SP_NAME));
        
        authnRequest.setSpApplication(PrefsPropsUtil.getString(themeDisplay.getCompanyId(), FedPropsKeys.STORK_SP_APLICATION));
        
        authnRequest.setSpCountry(PrefsPropsUtil.getString(themeDisplay.getCompanyId(), FedPropsKeys.STORK_SP_COUNTRY));
        
        authnRequest.setSPID(PrefsPropsUtil.getString(themeDisplay.getCompanyId(), FedPropsKeys.STORK_SP_NAME));

        
        String storkUserMapping = PrefsPropsUtil.getString(themeDisplay.getCompanyId(), FedPropsKeys.STORK_USER_MAPPING);
        IPersonalAttributeList pAttList = new PersonalAttributeList();
        String storkMandatoryAttr = PrefsPropsUtil.getString(themeDisplay.getCompanyId(), FedPropsKeys.STORK_AUTH_LOCAL_SEARCH_FILTER, FedPropsValues.STORK_AUTH_LOCAL_SEARCH_FILTER);
        
        if (storkUserMapping != null) {

            for (String attrMap: storkUserMapping.split("\n")) {
                if (attrMap.indexOf("=") == -1) {
                    continue;
                }

                String strAttrMapA[]= attrMap.split("=");
                if (strAttrMapA.length != 2) {
                    continue;
                }
                PersonalAttribute attr= new PersonalAttribute();
                attr.setName(strAttrMapA[1]);
                if(storkMandatoryAttr.equals(strAttrMapA[0])){
                    attr.setIsRequired(true);
                }
                else{
                    attr.setIsRequired(false);
                }
                pAttList.add(attr);
            }
        }
        
        authnRequest.setPersonalAttributeList(pAttList);

        byte token[]=null;
        try{

            STORKSAMLEngine storkEngine= STORKSAMLEngine.getInstance("SP");
            token= storkEngine.generateSTORKAuthnRequest(authnRequest).getTokenSaml();
            
        }
        catch(Exception ex){
            _log.error("Impossible to create the SAML token");
            _log.error(ex);
             setForward(actionRequest, "portlet.login.stork.error");
        }
        
        if(token!=null){
            actionResponse.setRenderParameter("SAMLToken", PEPSUtil.encodeSAMLToken(token));
            actionResponse.setRenderParameter("CCountry", ParamUtil.getString(actionRequest, "citizenCountry"));
            actionResponse.setRenderParameter("PEPSUrl", PrefsPropsUtil.getString(themeDisplay.getCompanyId(), FedPropsKeys.STORK_SPEPS_URL));
            setForward(actionRequest, "portlet.login.stork.peps");
        }
    }


}
