package com.solrpoc.core.solr.service

import org.apache.sling.api.resource.Resource

interface ContentExtractor {

    String extractContent(Resource resource)

    String extractContent(Resource resource, boolean recursive)
}
