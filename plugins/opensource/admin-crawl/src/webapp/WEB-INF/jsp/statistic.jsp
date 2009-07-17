<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<%@ include file="/WEB-INF/jsp/includes/include.jsp" %>
<html>
<head>
	<title>Admin - Statistic</title>
	<link rel="stylesheet" type="text/css" href="${theme}/css/reset-fonts-grids.css" />
	<link rel="stylesheet" type="text/css" href="${theme}/js/yui/build/tabview/assets/skins/sam/tabview.css">
	<script type="text/javascript" src="${theme}/js/yui/build/yahoo-dom-event/yahoo-dom-event.js"></script>
	<script type="text/javascript" src="${theme}/js/yui/build/element/element-min.js"></script>
	<script type="text/javascript" src="${theme}/js/yui/build/connection/connection-min.js"></script>
	<script type="text/javascript" src="${theme}/js/yui/build/tabview/tabview-min.js"></script>
	<script type="text/javascript" src="${theme}/js/yui/build/json/json-min.js"></script> 
	<script type="text/javascript" src="${theme}/js/yui/build/charts/charts-min.js"></script> 
	<script type="text/javascript" src="${theme}/js/yui/build/datasource/datasource-min.js"></script>
	<link rel="stylesheet" type="text/css" href="${theme}/css/style.css" />
</head>
<body class="yui-skin-sam">
	<div id="doc2">
		<div id="hd">
			<%@ include file="/WEB-INF/jsp/includes/header.jsp" %>
		</div>
		
		<c:if test="${!empty instanceNavigation}">
		<div class="yui-navset nav">
		    <ul class="yui-nav">
				<c:forEach items="${instanceNavigation}" var="navigation">
					<c:choose>
						<c:when test="${navigation.name == selectedInstance}">
							<li class="selected"><a href="${navigation.link}"><em>${navigation.name}</em></a></li>
						</c:when>
						<c:otherwise>
							<li><a href="${navigation.link}"><em>${navigation.name}</em></a></li>
						</c:otherwise>
					</c:choose>
				</c:forEach>
		    </ul>
		</div>
		</c:if>
		
		<c:if test="${!empty componentNavigation}">
		<div id="subnav">
		    <ul>
				<c:forEach items="${componentNavigation}" var="navigation">
					<c:choose>
						<c:when test="${navigation.name == selectedComponent}">
							<li class="selected"><a href="${navigation.link}"><em>${navigation.name}</em></a></li>
						</c:when>
						<c:otherwise>
							<li><a href="${navigation.link}"><em>${navigation.name}</em></a></li>
						</c:otherwise>
					</c:choose>
				</c:forEach>
		    </ul>
		</div>
		</c:if>
		
		<div id="bd">
			<div id="yui-main">
				<div class="yui-b">
					<h3>URL Statistik Gesamt</h3>
					<div id="chart" style="height:150px">Unable to load Flash content.</div>

					<script type="text/javascript">
					
						YAHOO.widget.Chart.SWFURL = "${theme}/js/yui/build/charts/assets/charts.swf";
						
						//used to format x axis labels
						YAHOO.example.numberToCurrency = function( value )
						{
							return YAHOO.util.Number.format(Number(value), {thousandsSeparator: "."});
						}
					
						//manipulating the DOM causes problems in ie, so create after window fires "load"
						YAHOO.util.Event.addListener(window, "load", function()
						{
					
						//--- data
					
							YAHOO.example.stats =
							[
							<c:forEach items="${statistics}" var="statistic" begin="0" end="0">
								{ host: '${statistic.host}', crawlDbCount: ${statistic.crawldbCount}, segmentCount: ${statistic.segmentCount} },
							</c:forEach>	
							];
					
							var statsData = new YAHOO.util.DataSource( YAHOO.example.stats );
							statsData.responseType = YAHOO.util.DataSource.TYPE_JSARRAY;
							statsData.responseSchema = { fields: [ "host", "crawlDbCount", "segmentCount" ] };
					
						//--- chart
					
							var seriesDef =
							[
								{
									xField: "crawlDbCount",
									displayName: "URLs gesamt"
								},
								{
									xField: "segmentCount",
									displayName: "URLs gefetched"
								},
								
							];
					
							var numberAxis = new YAHOO.widget.NumericAxis();
							//currencyAxis.labelFunction = "YAHOO.example.numberToCurrency";
					
							var mychart = new YAHOO.widget.BarChart( "chart", statsData,
							{
								series: seriesDef,
								yField: "host",
								xAxis: numberAxis,
								style: {legend:{display: "top"}}
							});
					
						
						});
					
					</script>
					
					<div style="margin-top:25px"></div>
					<h3>Top HasteNichtGesehen</h3>
					
					<div id="chart2" style="height:1000px">Unable to load Flash content.</div>

					<script type="text/javascript">
					
						YAHOO.widget.Chart.SWFURL = "${theme}/js/yui/build/charts/assets/charts.swf";
						
						//used to format x axis labels
						YAHOO.example.numberToCurrency = function( value )
						{
							return YAHOO.util.Number.format(Number(value), {thousandsSeparator: "."});
						}
					
						//manipulating the DOM causes problems in ie, so create after window fires "load"
						YAHOO.util.Event.addListener(window, "load", function()
						{
					
						//--- data
					
							YAHOO.example.stats2 =
							[
							<c:forEach items="${statistics}" var="statistic" begin="1">
								{ host: '${statistic.host}', crawlDbCount: ${statistic.crawldbCount}, segmentCount: ${statistic.segmentCount} },
								
							</c:forEach>	
							];
					
							var statsData2 = new YAHOO.util.DataSource( YAHOO.example.stats2);
							statsData2.responseType = YAHOO.util.DataSource.TYPE_JSARRAY;
							statsData2.responseSchema = { fields: [ "host", "crawlDbCount", "segmentCount" ] };
					
						//--- chart
					
							var seriesDef =
							[
								{
									xField: "crawlDbCount",
									displayName: "URLs gesamt"
								},
								{
									xField: "segmentCount",
									displayName: "URLs gefetched"
								},
								
							];
					
							var numberAxis = new YAHOO.widget.NumericAxis();
							//currencyAxis.labelFunction = "YAHOO.example.numberToCurrency";
					
							var mychart = new YAHOO.widget.BarChart( "chart2", statsData2,
							{
								series: seriesDef,
								yField: "host",
								xAxis: numberAxis,
								style: {legend:{display: "top"}}
							});
					
						
						});
					
					</script>

				</div>	
		</div>
	</div>
	<div id="ft">
		<%@ include file="/WEB-INF/jsp/includes/footer.jsp" %>
	</div>
</div>
</body>
</html>
