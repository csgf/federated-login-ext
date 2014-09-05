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
package com.liferay.portal.util;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.servlet.filters.saml.SAMLFilter;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;

/**
 *
 * @author Marco Fargetta <marco.fargetta@ct.infn.it>
 */
public class LDAPUtil {

    private String rootSearch;
    private InitialLdapContext ctx;
    private static Log _log = LogFactoryUtil.getLog(SAMLFilter.class);

    public LDAPUtil(String ldapURL, String rootSearch) {
        this.rootSearch = rootSearch;

        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ldapURL);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");

        try {

            this.ctx = new InitialLdapContext(env, null);
        } catch (NamingException ex) {
            _log.error(ex);
        }
    }

    public String getUserAttribute(String userFilter, String searchFilter, String attribute) {

        StringBuilder filter = new StringBuilder("(&");

        if (userFilter.startsWith("(")) {
            filter.append(userFilter);
        } else {
            filter.append("(").append(userFilter).append(")");
        }

        if (searchFilter.startsWith("(")) {
            filter.append(searchFilter);
        } else {
            filter.append("(").append(searchFilter).append(")");
        }

        filter.append(")");

        String fields[] = new String[1];
        fields[0] = attribute;
        try {
            NamingEnumeration<SearchResult> users = ctx.search(rootSearch, filter.toString(), new SearchControls(SearchControls.SUBTREE_SCOPE, 100, 0, fields, false, false));
            while (users.hasMore()) {
                SearchResult user = (SearchResult) users.next();
                if(user.getAttributes().get(attribute)!=null){
                    return (String) user.getAttributes().get(attribute).get();
                }
            }

        } catch (NamingException ex) {
            _log.error(ex);
        }

        return null;

    }
}
