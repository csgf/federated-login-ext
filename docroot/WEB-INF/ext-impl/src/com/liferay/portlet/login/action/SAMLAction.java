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

/**
 * @author Marco Fargetta <marco.fargetta@ct.infn.it>
 */
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.User;
import com.liferay.portal.security.auth.PrincipalException;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.struts.PortletAction;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.FedWebKeys;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.PropsValues;
import com.liferay.portal.util.SAMLUtil;
import com.liferay.portal.util.WebKeys;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;


import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * @author Marco Fargetta
 */
public class SAMLAction extends PortletAction {

    private static Log _log = LogFactoryUtil.getLog(SAMLAction.class);

    @Override
    public void processAction(
            ActionMapping actionMapping, ActionForm actionForm,
            PortletConfig portletConfig, ActionRequest actionRequest,
            ActionResponse actionResponse)
            throws Exception {

        _log.debug("SAMLAction Invoked");
        ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(
                WebKeys.THEME_DISPLAY);

        if (!SAMLUtil.isEnabled(themeDisplay.getCompanyId())) {
            throw new PrincipalException();
        }

        if (actionRequest.getRemoteUser() != null) {
            actionResponse.sendRedirect(themeDisplay.getPathMain());

            return;
        }

        HttpServletRequest request = PortalUtil.getHttpServletRequest(
                actionRequest);
        HttpSession session = request.getSession();

        if(session.getAttribute(FedWebKeys.SAML_ID_LOGIN)==null)
            return;


        String redirect = ParamUtil.getString(actionRequest, "redirect");

        if (Validator.isNotNull(redirect)) {
            redirect = PortalUtil.escapeRedirect(redirect);

            if (!redirect.startsWith(Http.HTTP)) {
                redirect = getCompleteRedirectURL(request, redirect);
            }

            actionResponse.sendRedirect(redirect);
        } else {
            boolean doActionAfterLogin = ParamUtil.getBoolean(
                    actionRequest, "doActionAfterLogin");

            if (doActionAfterLogin) {
                return;
            } else {
                actionResponse.sendRedirect(themeDisplay.getPathMain());
            }
        }

    }

    @Override
    public ActionForward render(
            ActionMapping actionMapping, ActionForm actionForm,
            PortletConfig portletConfig, RenderRequest renderRequest,
            RenderResponse renderResponse)
            throws Exception {

        ThemeDisplay themeDisplay = (ThemeDisplay) renderRequest.getAttribute(
                WebKeys.THEME_DISPLAY);

        return actionMapping.findForward("portlet.login.login");
    }

    protected String getCompleteRedirectURL(
            HttpServletRequest request, String redirect) {

        HttpSession session = request.getSession();

        Boolean httpsInitial = (Boolean) session.getAttribute(
                WebKeys.HTTPS_INITIAL);

        String portalURL = null;

        if (PropsValues.COMPANY_SECURITY_AUTH_REQUIRES_HTTPS
                && !PropsValues.SESSION_ENABLE_PHISHING_PROTECTION
                && (httpsInitial != null) && !httpsInitial.booleanValue()) {

            portalURL = PortalUtil.getPortalURL(request, false);
        } else {
            portalURL = PortalUtil.getPortalURL(request);
        }

        return portalURL.concat(redirect);
    }
}