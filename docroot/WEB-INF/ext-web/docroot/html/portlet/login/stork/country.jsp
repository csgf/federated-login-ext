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


    Document   : country
    Created on : 05-Aug-2014, 16:50:12
    Author     : Marco Fargetta <marco.fargetta@ct.infn.it>
--%>
<%@ include file="/html/portlet/login/init.jsp" %>

<portlet:actionURL var="storkURL">
        <portlet:param name="saveLastPath" value="0" />
        <portlet:param name="struts_action" value="/login/stork" />
</portlet:actionURL>


<aui:form action="<%= storkURL %>" method="post" name="fm">

        <aui:fieldset>
                <aui:input cssClass="openid-login" name="storkCountry" type="text" />

                <aui:button-row>
                        <aui:button type="submit" value="sign-in" />
                </aui:button-row>
        </aui:fieldset>
</aui:form>


<div id="margen">
        <c:if test="<%= euromap %>">
                <liferay-util:include page="selectMapCountry.jsp"/>
        </c:if>
        <c:else>
                <liferay-util:include page="selectListCountry.jsp"/>
        </c:else>
</div>

<liferay-util:include page="/html/portlet/login/navigation.jsp" />
