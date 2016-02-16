<%@include file="/apps/solrpoc/global.jsp"%>

<%-- initialize the sidekick in any mode that's not disabled (publish) --%>
<c:if test="${wcmMode != 'DISABLED'}">
    <cq:include script="/libs/wcm/core/components/init/init.jsp"/>
</c:if>

<cq:includeClientLib css="solrpoc.components.footer"/>


