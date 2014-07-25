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

package com.liferay.portal.kernel.util;

/**
 * @author Marco Fargetta <marco.fargetta@ct.infn.it>
 */
public interface FedPropsKeys extends PropsKeys {

	public static final String SAML_AUTH_ENABLED = "saml.auth.enabled";
	public static final String SAML_AUTH_LDLAP_CHECK = "saml.auth.ldap.check";
	public static final String SAML_AUTH_PAGE_MISS_USER = "saml.auth.page.miss.user";
	public static final String SAML_AUTH_PAGE_MISS_ATTRIBUTE = "saml.auth.page.miss.attribute";
	public static final String SAML_AUTH_LDAP_SEARCH_FILTER = "saml.auth.ldap.search.filter";
	public static final String SAML_AUTH_LOCAL_SEARCH_FILTER = "saml.auth.local.search.filter";
	public static final String SAML_USER_CREATE = "saml.auth.user.create";
	public static final String SAML_USER_MAPPING = "saml.auth.user.mapping";
	public static final String SAML_AUTH_PAGE_PROTECTED = "saml.auth.page.protected";
	public static final String SAML_AUTH_PAGE_EXIT = "saml.auth.page.exit";
        
	public static final String STORK_AUTH_ENABLED = "stork.auth.enabled";
	public static final String STORK_AUTH_LDLAP_CHECK = "stork.auth.ldap.check";
	public static final String STORK_AUTH_PAGE_MISS_USER = "stork.auth.page.miss.user";
	public static final String STORK_AUTH_PAGE_MISS_ATTRIBUTE = "stork.auth.page.miss.attribute";
	public static final String STORK_AUTH_LDAP_SEARCH_FILTER = "stork.auth.ldap.search.filter";
	public static final String STORK_AUTH_LOCAL_SEARCH_FILTER = "stork.auth.local.search.filter";
	public static final String STORK_USER_CREATE = "stork.auth.user.create";
        
	public static final String COMPANY_SECURITY_LOCAL_LOGIN = "company.security.local.login";
}
