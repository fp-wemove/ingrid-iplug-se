<%@ include file="/WEB-INF/jsp/base/include.jsp" %><%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@page import="de.ingrid.admin.security.IngridPrincipal"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="de">
<head>
<title><fmt:message key="DatabaseConfig.main.title"/></title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
<meta name="description" content="" />
<meta name="keywords" content="" />
<meta name="author" content="wemove digital solutions" />
<meta name="copyright" content="wemove digital solutions GmbH" />
<link rel="StyleSheet" href="../css/base/portal_u.css" type="text/css" media="all" />

</head>
<body>
    <div id="header">
        <img src="../images/base/logo.gif" width="168" height="60" alt="Portal U" />
        <h1><fmt:message key="DatabaseConfig.main.configuration"/></h1>
        <%
          java.security.Principal  principal = request.getUserPrincipal();
          if(principal != null && !(principal instanceof IngridPrincipal.SuperAdmin)) {
        %>
            <div id="language"><a href="../base/auth/logout.html"><fmt:message key="DatabaseConfig.main.logout"/></a></div>
        <%
          }
        %>
    </div>
    <div id="help"><a href="#">[?]</a></div>

    <c:set var="active" value="dbParams" scope="request"/>
    <c:import url="../base/subNavi.jsp"></c:import>

    <div id="contentBox" class="contentMiddle">
        <h1 id="head">Database Parameter</h1>
        <div class="controls">
            <a href="../base/extras.html">Zur&uuml;ck</a>
            <a href="../base/welcome.html">Abbrechen</a>
            <a href="#" onclick="document.getElementById('seSettings').submit();">Weiter</a>
        </div>
        <div class="controls cBottom">
            <a href="../base/extras.html">Zur&uuml;ck</a>
            <a href="../base/welcome.html">Abbrechen</a>
            <a href="#" onclick="document.getElementById('seSettings').submit();">Weiter</a>
        </div>
        <div id="content">
            <form:form id="seSettings" method="post" action="dbParams.html">
                <table id="konfigForm">
                    <br />
                    <tr>
                        <td colspan="2"><h3>Einstellungen f�r das SE-iPlug:</h3></td>
                    </tr>
                    <tr>
                        <td class="leftCol">Datenbankpfad</td>
                        <td>
                            <input type="text" name="dataBasePath" value="${dataBasePath}" />
                            <form:errors path="dataBasePath" cssClass="error" element="div" />
                            <br />
                            Dies ist der Pfad, wo die dateibasierte Datenbank abgelegt werden soll, in der sich die gepflegten URLs befinden.
                            <p style="color: gray;">(Bsp: database)</p>
                        </td>
                    </tr>
                    <tr>
                        <td class="leftCol">Instanzenpfad</td>
                        <td>
                            <input type="text" name="instancePath" value="${instancePath}" />
                            <form:errors path="instancePath" cssClass="error" element="div" />
                            <br />
                            Dieser Pfad gibt an, wo die Einstellungen und Indexe der gesammelten Webseiten abgelegt werden sollen.
                            <p style="color: gray;">(Bsp: instances)</p>
                        </td>
                    </tr>
                    <tr>
                        <td class="leftCol">ElasticSearch Port</td>
                        <td>
                            <input type="text" name="elasticSearchPort" value="${elasticSearchPort}" />
                            <form:errors path="elasticSearchPort" cssClass="error" element="div" />
                            <br />
                            Dieser Port wird f�r die Kommunikation mit dem Index verwendet.
                            <p style="color: gray;">(Bsp: 3000)</p>
                        </td>
                    </tr>
                </table>
            </form:form>
        </div>
    </div>

    <div id="footer" style="height:100px; width:90%"></div>
</body>
</html>

