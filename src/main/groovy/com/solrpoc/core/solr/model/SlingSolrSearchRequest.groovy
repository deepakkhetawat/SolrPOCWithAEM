package com.solrpoc.core.solr.model

import groovy.util.logging.Slf4j
import org.apache.sling.api.SlingHttpServletRequest
import org.apache.sling.api.resource.ResourceResolver
import org.apache.solr.client.solrj.SolrQuery
/**
 * Implementation of the SolrSearchRequest from a SlingHttpServletRequest
 */
@Slf4j
class SlingSolrSearchRequest {

    final SlingHttpServletRequest httpServletRequest
    String query

    SlingSolrSearchRequest(SlingHttpServletRequest httpServletRequest) {
        this.httpServletRequest = httpServletRequest
    }

    String getQuery() {
        if(!query) {
            query = httpServletRequest.getParameter("query")
        }
        return query
    }

    String getSearchType() {
        String searchType = 'text-search'
        if(httpServletRequest.getRequestPathInfo().selectors.length>0)
            searchType = httpServletRequest.getRequestPathInfo().selectors[0]
        return searchType
    }


    int getResultsPerPage() {
        int resultsPerPage = 100
        if(httpServletRequest.getParameter("results_per_page"))
            resultsPerPage = Integer.parseInt(httpServletRequest.getParameter("results_per_page"))
        return resultsPerPage
    }

    int getPageNumber() {
        int pageNumber = 1
        if(httpServletRequest.getParameter("page_number"))
            pageNumber =Integer.parseInt(httpServletRequest.getParameter("page_number"))
        return pageNumber
    }


    ResourceResolver getResourceResolver(){
        resourceResolver
    }


    SolrQuery.ORDER getSortOrder() {
        String sortOrder = httpServletRequest.getParameter("sortOrder")
        SolrQuery.ORDER order
        if(sortOrder && sortOrder=="asc")
        {
            order =  SolrQuery.ORDER.asc
        }
        else if(sortOrder && sortOrder=="desc")
        {
            order =  SolrQuery.ORDER.desc
        }

        return order

    }
}
