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

import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.FedPropsKeys;

/**
 * @author Marco Fargetta <marco.fargetta@ct.infn.it>
 */
public class FedPropsValues extends PropsValues {

	public static final boolean SAML_AUTH_ENABLED = GetterUtil.getBoolean(PropsUtil.get(FedPropsKeys.SAML_AUTH_ENABLED), false);
	public static final boolean SAML_AUTH_LDLAP_CHECK = GetterUtil.getBoolean(PropsUtil.get(FedPropsKeys.SAML_AUTH_LDLAP_CHECK), true);
	public static final String SAML_AUTH_PAGE_MISS_USER = GetterUtil.getString(PropsUtil.get(FedPropsKeys.SAML_AUTH_PAGE_MISS_USER), "/not_authorised");
	public static final String SAML_AUTH_PAGE_MISS_ATTRIBUTE = GetterUtil.getString(PropsUtil.get(FedPropsKeys.SAML_AUTH_PAGE_MISS_ATTRIBUTE), "/attributes_missed");
	public static final String SAML_AUTH_LDAP_SEARCH_FILTER = GetterUtil.getString(PropsUtil.get(FedPropsKeys.SAML_AUTH_LDAP_SEARCH_FILTER));
	public static final boolean SAML_USER_CREATE = GetterUtil.getBoolean(PropsUtil.get(FedPropsKeys.SAML_USER_CREATE), false);
	public static final String SAML_USER_MAPPING = GetterUtil.getString(PropsUtil.get(FedPropsKeys.SAML_USER_MAPPING));
	public static final String SAML_AUTH_PAGE_PROTECTED = GetterUtil.getString(PropsUtil.get(FedPropsKeys.SAML_AUTH_PAGE_PROTECTED), "/saml/login");

	public static final boolean STORK_AUTH_ENABLED = GetterUtil.getBoolean(PropsUtil.get(FedPropsKeys.STORK_AUTH_ENABLED),false);
	public static final boolean STORK_USER_CREATE = GetterUtil.getBoolean(PropsUtil.get(FedPropsKeys.STORK_USER_CREATE), false);
        
	public static final boolean COMPANY_SECURITY_LOCAL_LOGIN = GetterUtil.getBoolean(PropsUtil.get(FedPropsKeys.COMPANY_SECURITY_LOCAL_LOGIN), true);

}
