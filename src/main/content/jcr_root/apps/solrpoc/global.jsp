<%@include file="/libs/foundation/global.jsp" %>
<%-- Can't be defined as 'sling' as the 1.0 taglib already has that name --%>
<%@taglib prefix="sling13" uri="http://sling.apache.org/taglibs/sling" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%--<%@taglib prefix="solrpoc" uri="http://www.solrpoc.com/taglibs/core" %>--%>

<%@page session="false"
        import="com.day.cq.wcm.api.WCMMode" %>
<%
    WCMMode wcmMode = WCMMode.fromRequest(request);
%>

<c:set var="isEditMode" value="<%=(wcmMode == WCMMode.EDIT) || (wcmMode == WCMMode.DESIGN) %>"/>
<c:set var="wcmMode" value="<%= wcmMode %>"/>
