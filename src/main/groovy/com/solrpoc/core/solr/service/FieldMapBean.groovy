package com.solrpoc.core.solr.service

import groovy.transform.CompileStatic

@CompileStatic
public class FieldMapBean {

    private final String resourceType

    /**
     * A map of jcr-property : solr-field e.g. "jcr:title":"title", "jcr:description":"description"
     */
    private final Map<String, String> fieldMap

    FieldMapBean (String resourceType, Map<String, String> fieldMap) {
        this.resourceType = resourceType
        this.fieldMap = fieldMap
    }
}
