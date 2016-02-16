<%@include file="/apps/solrpoc/global.jsp" %>
<%@page session="false" %>
<c:set var="searchPagePath" value="${properties.searchPagePath}"/>
<c:set var="paginationResults" value="${not empty properties.paginationResults ? properties.paginationResults : '15'}"
       scope="request"/>

<c:choose>
    <c:when test="${empty searchPagePath}">
        <c:if test="${isEditMode}">
            <p class="edit-links">Please Configure search page path.</p>
        </c:if>
    </c:when>

    <c:otherwise>
        <form class="navbar-form" action="${searchPagePath}.html" accept-charset="utf-8" role="search">
            <div class="input-group">
                <label class="sr-only" for="searchBar">Search </label>
                <input type="text" name="query" title="Search" idr="ltr" class="form-control search-input"
                       value="${param.query}" id="searchBar">
                <input type="hidden" name="results_per_page" value="${paginationResults}">
                <input type="hidden" name="page_number" value="1">
                <input type="hidden" name="_charset_" value="utf-8">
            </div>
        </form>
    </c:otherwise>
</c:choose>
