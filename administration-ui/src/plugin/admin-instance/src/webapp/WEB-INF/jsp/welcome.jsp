<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
   "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ include file="/WEB-INF/jsp/include.jsp" %>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
<title>Create Nutch Instance</title>
<meta http-equiv="Content-type" content="text/html; charset=utf-8" />

</head>

<body>
<% 
System.out.println(application.getClass());
System.out.println(application.getAttribute("foo"));
%>
<form action="welcome.html" method="post">
	<input type="submit" value="Senden"/>
</form> 
</body>
</html>
