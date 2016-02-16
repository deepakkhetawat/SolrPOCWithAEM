package com.solrpoc.core.solr.service
import com.solrpoc.core.solr.model.SlingSolrSearchRequest
import com.solrpoc.core.solr.model.SolrSearchResults
import org.apache.sling.api.SlingHttpServletRequest

interface ResultConfiguration {


    SolrSearchResults getSearchResults(SlingSolrSearchRequest request)

    SolrSearchResults getSearchResults(SlingHttpServletRequest request)

}
