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
package com.liferay.portal.util;

/**
 * @author Marco Fargetta <marco.fargetta@ct.infn.it>
 */
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.FedPropsKeys;

/**
 * @author Jorge Ferrer
 */
public class SAMLUtil {
    private static Log _log = LogFactoryUtil.getLog(SAMLUtil.class);
    private static SAMLUtil _instance = new SAMLUtil();

    public static boolean isEnabled(long companyId) throws SystemException {
        return PrefsPropsUtil.getBoolean(
                companyId, FedPropsKeys.SAML_AUTH_ENABLED,
                FedPropsValues.SAML_AUTH_ENABLED);
    }

    public static String getAuthURL(long companyId) throws SystemException {
        return PrefsPropsUtil.getString(companyId, FedPropsKeys.SAML_AUTH_PAGE_PROTECTED, FedPropsValues.SAML_AUTH_PAGE_PROTECTED);
    }

    public static String getAuthExitURL(long companyId) throws SystemException {
        return PrefsPropsUtil.getString(companyId, FedPropsKeys.SAML_AUTH_PAGE_EXIT, FedPropsValues.SAML_AUTH_PAGE_EXIT);
    }

}