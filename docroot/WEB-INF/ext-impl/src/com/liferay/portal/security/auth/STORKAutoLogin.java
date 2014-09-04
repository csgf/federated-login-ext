/***********************************************************************
 *  Copyright (c) 2011: 
 *  Istituto Nazionale di Fisica Nucleare (INFN), Italy
 *  Consorzio COMETA (COMETA), Italy
 * 
 *  See http://www.infn.it and and http://www.consorzio-cometa.it for details on
 *  the copyright holders.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 ***********************************************************************/

package com.liferay.portal.security.auth;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.util.FedWebKeys;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.SAMLUtil;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 * @author Marco Fargetta <marco.fargetta@ct.infn.it>
 */
public class STORKAutoLogin implements AutoLogin {

    private static Log _log = LogFactoryUtil.getLog(STORKAutoLogin.class);

    @Override
    public String[] login(HttpServletRequest request, HttpServletResponse response) throws AutoLoginException {
        String[] credentials = null;

        try {
            long companyId = PortalUtil.getCompanyId(request);

            if (!SAMLUtil.isEnabled(companyId)) {
                return credentials;
            }

            HttpSession session = request.getSession();

            Long userId = (Long) session.getAttribute(FedWebKeys.STORK_ID_LOGIN);

            if (userId == null) {
                return credentials;
            }

            session.removeAttribute(FedWebKeys.STORK_ID_LOGIN);

            User user = UserLocalServiceUtil.getUserById(userId);

            credentials = new String[3];

            credentials[0] = String.valueOf(user.getUserId());
            credentials[1] = user.getPassword();
            credentials[2] = Boolean.TRUE.toString();
        } catch (Exception e) {
            _log.error(e, e);
        }

        return credentials;

    }

}
