package com.solrpoc.core.solr.model

import groovy.util.logging.Slf4j


@Slf4j
class SolrSearchResults {

    List<SolrSearchResult> searchResults;
    long totalResults;
    int totalTabs
    List<Facet> tagsSolrFacetList
}


