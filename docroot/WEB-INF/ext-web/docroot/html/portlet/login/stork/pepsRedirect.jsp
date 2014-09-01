<%-- 

/**************************************************************************
Copyright (c) 2011: 
Istituto Nazionale di Fisica Nucleare (INFN), Italy
Consorzio COMETA (COMETA), Italy

See http://www.infn.it and and http://www.consorzio-cometa.it for details on the
copyright holders.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
****************************************************************************/


    Document   : pepsRedirect
    Created on : 28-Aug-2014, 16:04:02
    Author     : Marco Fargetta <marco.fargetta@ct.infn.it>
--%>

<%@ include file="/html/portlet/login/init.jsp" %>

<h3><liferay-ui:message key="stork-manual-redirect" /></h3>
<form action="<%= request.getParameter("PEPSUrl") %>" method="post" name="fm" id="<portlet:namespace/>pepsform">
    <input name="SAMLRequest" type="hidden" value="<%= request.getParameter("SAMLToken") %>" />
    <input name="country" type="hidden" value="<%= request.getParameter("CCountry") %>" />
    <input type="submit" value="Sign In"/>
</form>
    
<aui:script use="aui-base">
    A.one('#<portlet:namespace/>pepsform').submit();
</aui:script>