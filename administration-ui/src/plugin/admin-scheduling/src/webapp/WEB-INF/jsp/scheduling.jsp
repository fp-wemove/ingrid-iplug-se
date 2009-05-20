<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<%@ include file="/WEB-INF/jsp/include.jsp" %>
<html>
<head>
<title>Admin Welcome</title>
<!-- Source File -->
<link rel="stylesheet" type="text/css" href="css/yui/build/reset-fonts-grids/reset-fonts-grids.css" />

 
<link rel="stylesheet" type="text/css" href="css/yui/build/tabview/assets/skins/sam/tabview.css">
<link rel="stylesheet" type="text/css" href="css/yui/build/button/assets/skins/sam/button.css">

 
<script type="text/javascript" src="css/yui/build/yahoo-dom-event/yahoo-dom-event.js" ></script>
<script type="text/javascript" src="css/yui/build/element/element-min.js" ></script>
<script type="text/javascript" src="css/yui/build/tabview/tabview-min.js" ></script>
<script type="text/javascript" src="css/yui/build/button/button-min.js" ></script>
 
<link rel="stylesheet" type="text/css" href="css/yui/build/menu/assets/skins/sam/menu.css" />
<script type="text/javascript" src="css/yui/build/container/container_core-min.js"></script>
<script type="text/javascript" src="css/yui/build/menu/menu-min.js"></script>


</head>
<body class="yui-skin-sam">
	<div id="doc" class="yui-t1">
		<div id="hd">header</div>
			<div id="bd">
				<div id="yui-main">
					<div class="yui-b">
						<div>
							<p>
								Current Pattern: ${savedPattern}
							</p>
							<p>
								Current Crawl Data: ${savedCrawlData}
							</p>
							<form action="delete.html" method="post">
								<input type="submit" value="Delete">
							</form>
						</div>
						<div id="schedulingTabs" class="yui-navset">
						    <ul class="yui-nav">
						        <li class="selected"><a href="#tab1"><em>Daily</em></a></li>
						        <li><a href="#tab2"><em>Weekly</em></a></li>
						        <li><a href="#tab3"><em>Monthly</em></a></li>
						        <li><a href="#tab4"><em>Advanced</em></a></li>
						    </ul>            
						    <div class="yui-content">
						        <div id="tab1">
						        	<p>
						        		<form:form action="daily.html" method="post" commandName="clockCommand">
						        			<fieldset>
						        				<legend>Time</legend>
							        			<form:label path="hour" >Hour</form:label>
							        			<form:select path="hour">
							        				<form:options items="${hours}"/>
							        			</form:select>
							        			<form:label path="minute" >Minute</form:label>
							        			<form:select path="minute">
							        				<form:options items="${minutes}"/>
							        			</form:select>
							        			<form:label path="period" >Period</form:label>
							        			<form:select path="period">
							        				<form:options items="${periods}"/>	
							        			</form:select>
						        			</fieldset>
						        			<fieldset>
						        				<legend>Crawl Parameters</legend>
						        				<form:label path="depth" >Crawl Depth</form:label>
							        			<form:select path="depth">
							        				<form:options items="${depths}"/>
							        			</form:select>
						        				<form:label path="topn" >TopN</form:label>
							        			<form:input path="topn" />
						        			</fieldset>
						        			<input type="submit" value="Save" />
						        		</form:form>
						        	</p>
						        </div>
						        <div id="tab2">
						        	<p>
						        	
						        		<form:form action="weekly.html" method="post" commandName="weeklyCommand">
						        			<fieldset>
						        				<legend>Time</legend>
							        			<form:label path="hour" >Hour</form:label>
							        			<form:select path="hour">
							        				<form:options items="${hours}"/>
							        			</form:select>
							        			<form:label path="minute" >Minute</form:label>
							        			<form:select path="minute">
							        				<form:options items="${minutes}"/>
							        			</form:select>
							        			<form:label path="period" >Period</form:label>
							        			<form:select path="period">
							        				<form:options items="${periods}"/>	
							        			</form:select>
						        			</fieldset>
						        			<fieldset>
						        				<legend>Day's</legend>
						        				<form:checkboxes items="${days}" path="days"/>
						        			</fieldset>
						        			<fieldset>
						        				<legend>Crawl Parameters</legend>
						        				<form:label path="depth" >Crawl Depth</form:label>
							        			<form:select path="depth">
							        				<form:options items="${depths}"/>
							        			</form:select>
						        				<form:label path="topn" >TopN</form:label>
							        			<form:input path="topn" />
						        			</fieldset>
						        			<input type="submit" value="Save" />
						        		</form:form>
						        		
						        	</p>
						        </div>
						        <div id="tab3">
						        	<p>
						        	
										<script type="text/javascript">
										(function () {
									    	var Button = YAHOO.widget.Button;


									    	YAHOO.util.Event.onContentReady("checkboxButtons", function () {
									    		<c:forEach items="${month}" var="dayOfMonth">
										            var oCheckButton_${dayOfMonth} = new Button("daysOfMonth_${dayOfMonth}", { label:"${dayOfMonth}" });
									            </c:forEach>
									        });

										  }());
										</script>

										<form:form action="monthly.html" method="post" commandName="monthlyCommand">
						        			<fieldset>
						        				<legend>Time</legend>
							        			<form:label path="hour" >Hour</form:label>
							        			<form:select path="hour">
							        				<form:options items="${hours}"/>
							        			</form:select>
							        			<form:label path="minute" >Minute</form:label>
							        			<form:select path="minute">
							        				<form:options items="${minutes}"/>
							        			</form:select>
							        			<form:label path="period" >Period</form:label>
							        			<form:select path="period">
							        				<form:options items="${periods}"/>	
							        			</form:select>
						        			</fieldset>
        									<fieldset id="checkboxButtons">
        										<legend>Days of Month</legend>
        										<c:forEach items="${month}" var="dayOfMonth">
        											<input name="daysOfMonth" type="checkbox" id="daysOfMonth_${dayOfMonth}" value="${dayOfMonth}">
        										</c:forEach>
        									</fieldset>
						        			<fieldset>
						        				<legend>Crawl Parameters</legend>
						        				<form:label path="depth" >Crawl Depth</form:label>
							        			<form:select path="depth">
							        				<form:options items="${depths}"/>
							        			</form:select>
						        				<form:label path="topn" >TopN</form:label>
							        			<form:input path="topn" />
						        			</fieldset>
										    <div>
										        <input type="reset" name="resetbutton" value="Reset Form">
										        <input type="submit" name="submitbutton" value="Submit Form">
										    </div>
										
										</form:form>																        		
						        	</p>
						        </div>
						        <div id="tab4">
						        	<p>
							        	<form:form action="advanced.html" method="post" commandName="advancedCommand">
							        		<fieldset>
						        			<legend>Pattern</legend>
							        			<form:label path="pattern" >Pattern</form:label>
							        			<form:input path="pattern" />
							        		</fieldset>
						        			<fieldset>
						        				<legend>Crawl Parameters</legend>
						        				<form:label path="depth" >Crawl Depth</form:label>
							        			<form:select path="depth">
							        				<form:options items="${depths}"/>
							        			</form:select>
						        				<form:label path="topn" >TopN</form:label>
							        			<form:input path="topn" />
						        			</fieldset>
						        			<input type="submit" value="Save" />
						        		</form:form>
						        	</p>
						        </div>
						    </div>
						</div>
						<script  type="text/javascript">
						(function() {
						    var tabView = new YAHOO.widget.TabView('schedulingTabs');
						})();
						</script>
					</div>
				</div>
				<div class="yui-b">
						<script type="text/javascript">
						    YAHOO.util.Event.onContentReady("leftNav", function () {
						        var oMenu = new YAHOO.widget.Menu("leftNav", { 
						                                                position: "static", 
						                                                hidedelay:  750, 
						                                                lazyload: true });
						        oMenu.render();            
						    });
						</script>					
						
						<div id="leftNav" class="yuimenu">
					    <div class="bd">
					        <h6>General Instances</h6>
					        <ul>
								<c:forEach items="${rootContexts}" var="rootContext">
									<li class="yuimenuitem"><a class="yuimenuitemlabel" href="${rootContext}">${rootContext}</a></li>
								</c:forEach>
					        </ul>
					        <h6 class="first-of-type">Instances</h6>
					        <ul class="first-of-type">
								<c:forEach items="${navigations}" var="navigation">
									<li class="yuimenuitem"><a class="yuimenuitemlabel" href="${navigation}">${navigation}</a></li>
								</c:forEach>
					        </ul>
					    </div>
					</div>
				</div>
			</div>
		<div id="ft">footer</div>
	</div>
</body>
</html>