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
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.struts.PortletAction;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.STORKUtil;
import com.liferay.portlet.login.action.stork.Country;
import eu.stork.peps.auth.commons.PersonalAttribute;
import java.util.ArrayList;
import java.util.Properties;
import javax.portlet.PortletConfig;
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

        return mapping.findForward("portlet.login.stork.country");
    }
   

}
