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
    private static Properties configs;
    private static ArrayList<Country> countries;
    private static ArrayList<PersonalAttribute> attributeList;
    private static String spId;
    private static String providerName;
    private static String qaa;
    private static String returnUrl;
    private String SAMLRequest;
    private String pepsUrl;
    private String citizenCountry;
    private String euromap;

    @Override
    public ActionForward render(ActionMapping mapping, ActionForm form, PortletConfig portletConfig, RenderRequest renderRequest, RenderResponse renderResponse) throws Exception {
        ThemeDisplay themeDisplay = (ThemeDisplay) renderRequest.getAttribute(
                WebKeys.THEME_DISPLAY);

        if (!STORKUtil.isEnabled(themeDisplay.getCompanyId())) {
            return mapping.findForward("portlet.login.login");
        }

        renderResponse.setTitle(themeDisplay.translate("stork"));


        try{
            populate();
            return mapping.findForward("portlet.login.stork.country");
        }
        catch(Exception ex){
            _log.error("Impossible to read STORK Properties");
        }
        
        return mapping.findForward("portlet.login.login");
        

    }

    private void populate() throws Exception{
        //Loading sp.properties
//        try {
//            configs = SPUtil.loadConfigs(Constants.SP_PROPERTIES);
//        } catch (IOException ex) {
//            _log.error(ex);
//            throw new STORKException("Could not load configuration file ");
//        }
//
//        countries = new ArrayList<Country>();
//
//        // Using the country list from the STORK Control File (xml)
//        if ((Boolean.valueOf(configs.getProperty(Constants.SP_VERSIONCONTROL)).booleanValue())) {
//            ManageSTORKInfo versioninfo = new ManageSTORKInfo(configs.getProperty(Constants.SP_ENVIRONMENT));
//            List<Country> countrynames = versioninfo.processSTORKInfo(configs.getProperty(Constants.SP_VERSIONINFOFILE));
//            for (Iterator<Country> iter = countrynames.iterator(); iter.hasNext();) {
//                Country c = (Country) iter.next();
//                countries.add(c);
//            }
//            setPepsUrl(versioninfo.getPepsURL());
//        } // Using the country list from sp.properties
//        else {
//            int numCountries = Integer.parseInt(configs.getProperty(Constants.COUNTRY_NUMBER));
//            for (int i = 1; i <= numCountries; i++) {
//                Country country = new Country(configs.getProperty("country" + i + ".name"), configs.getProperty("country" + i + ".name"));
//                countries.add(country);
//            }
//            setPepsUrl(configs.getProperty(Constants.SPEPS_URL));
//        }
//        setEuromap(configs.getProperty(Constants.SP_EUROMAP));
//        return "populate";
    }
}
