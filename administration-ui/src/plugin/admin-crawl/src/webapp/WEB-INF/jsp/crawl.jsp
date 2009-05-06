<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<%@ include file="/WEB-INF/jsp/include.jsp" %>
<html>
<head>
<title>Admin Welcome</title>
<!-- Source File -->
<link rel="stylesheet" type="text/css" href="css/yui/build/reset-fonts-grids/reset-fonts-grids.css" />


</head>
<body class="yui-skin-sam">
	<div id="doc" class="yui-t1">
		<div id="hd">header</div>
			<div id="bd">
				<div id="yui-main">
					<div class="yui-b">
						<form:form action="index.html" method="post" modelAttribute="crawlCommand">
							<fieldset>
								<legend>Crawl Parameters</legend>
								<form:label path="depth">Depth</form:label>
								<form:select path="depth" items="${depths}"/>
								<form:label path="topn">Top Pages</form:label>
								<form:input path="topn"/>
								<input type="submit" value="Start">	
							</fieldset>
						</form:form>
					</div>
				</div>
				<div class="yui-b">
				Welcome Navigation
					<c:forEach items="${navigations}" var="navigation">
						<a href="${navigation}">${navigation}</a>
					</c:forEach>
				</div>
			</div>
		<div id="ft">footer</div>
	</div>
</body>
</html>