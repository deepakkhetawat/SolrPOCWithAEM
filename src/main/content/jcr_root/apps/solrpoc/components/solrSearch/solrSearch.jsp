<%@include file="/apps/solrpoc/global.jsp" %>

<%@ page import="com.solrpoc.core.solr.service.ResultConfiguration" %>
<% ResultConfiguration resultConfiguration = sling.getService(ResultConfiguration.class); %>

<c:set var="results" value="<%= resultConfiguration.getSearchResultsSolr(slingRequest)%>"/>
<c:set var="tagsSolrFacetList" value="${results.tagsSolrFacetList}"/>

<c:set var="searchResultSize" value="${results.totalResults}"/>

<div>
    <form role="search" class="searchSection">
        <div class="searchBar">
            <label class="sr-only" for="solrSearchQuery">Search </label>
            <input autocomplete="off" type="text" name="query" title="Search" placeholder="Search..."
                   id="solr-search-query"
                   value="${param.query}"/>
        </div>
        <div class="searchButton">
            <input type="button" value="Search" class="solr-search-button" title="Search"/>
        </div>
        <div class="solr-results-clear-button" title="Clear Results">&nbsp;</div>
    </form>

    <div>
        <div id="solr-results-info">About ${searchResultSize} results</div>
    </div>
</div>


<div id="searchResults">
    <c:choose>
        <c:when test="${not empty results.searchResults}">
            <c:forEach var="i" begin="0" end="${fn:length(results.searchResults) -1}">
                <div class="resultRow">

                    <a target="_self" href="${results.searchResults[i].id}.html">
                        ${results.searchResults[i].title}
                    </a>

                    <%--   <div> ${solrpoc:getAbsoluteUrl(slingRequest, results.searchResults[i].id)} </div>--%>
                </div>
            </c:forEach>
        </c:when>

        <c:otherwise>
            <p>
                Your search - <b>${param.query}</b> - did not match any documents.
            </p>
        </c:otherwise>
    </c:choose>


    <c:if test="${searchResultSize > paginationResults}">
        <div class="pagination">
            <c:forEach var="index" begin="1" end="${results.totalTabs+1}">
                <c:choose>
                    <c:when test="${not empty param.page_number and param.page_number eq index}">
                        <a class="page-index current-page-index">${index}</a>
                    </c:when>

                    <c:otherwise>
                        <a class="page-index">${index}</a>
                    </c:otherwise>
                </c:choose>
            </c:forEach>
        </div>
    </c:if>

    <div>
        <c:if test="${not empty tagsSolrFacetList}">
            <ul>
                Tags :
                <c:forEach var="facet" items="${tagsSolrFacetList}">
                    <li>
                        <c:if test="${not empty facet.facetTitle}">
                            <a href="${currentPage.path}.html?query=${facet.facetQuery}">
                                ${facet.facetTitle} (${facet.facetCount})
                            </a>
                        </c:if>
                    </li>
                </c:forEach>
            </ul>
        </c:if>
    </div>

</div>
