package com.solrpoc.core.solr.service.impl

import com.day.cq.replication.Replicator
import com.day.cq.tagging.Tag
import com.day.cq.tagging.TagManager
import com.solrpoc.core.solr.service.ContentExtractor
import com.solrpoc.core.solr.service.FieldMapBean
import com.solrpoc.core.util.TextUtil
import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang3.text.WordUtils
import org.apache.felix.scr.annotations.*
import org.apache.sling.api.resource.Resource
import org.apache.sling.api.resource.ResourceResolver
import org.apache.sling.api.resource.ValueMap
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.osgi.service.component.ComponentContext

import javax.jcr.Node
import javax.jcr.PropertyType

/**
 * This service implementation is used for exposing Page content as Solr formmated XML.
 * @CompileStatic is not applied as the code makes use of dynamic variables (type MarkupBuilder)
 * that show up as error with static compile check.
 */
//@CompileStatic
@Slf4j
@Component(label = 'Solrpoc - Content extractor service for Solr indexing',
        description = 'Extracts page content by formatting these into corresponding Solr XML format', metatype = true, immediate = true, policy = ConfigurationPolicy.REQUIRE)
@Service(ContentExtractor)
class ContentExtractorImpl implements ContentExtractor {

    @Reference
    Replicator replicator
    /** This constant is used to separate a resource type from its property mappings. */
    private static final String RESOURCE_SEPARATOR = '\\@'

    /** This constant is used to separate individual property mappings for a resource type. */
    private static final String PROPERTIES_SEPARATOR = ';'

    /** This constant is used to separate the mapping between JCR property and solr field. */
    private static final String PROPERTY_SEPARATOR = '='

    /** Variable for fetching the JCR-solr field mapping values. */
    public static final String METADATA_MAPPING_VALUE = 'metadata.mapping.values'

    /** Solr date-field format. Solr only accepts dates in UTC format */
    private static final String SOLR_DATE_FIELD_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.sss'Z'"

    /** poc Template main parsys name */
    private static final String MAIN_CONTENT_PAR = 'main-content-section'



    @Property(name = 'metadata.mapping.values',
            label = 'Component Indexed values',
            description = '''Enter the component resource types their property mapping separated by '@' in the format <resourceType>@<solr-field-1>=<jcr-property-1>;<solr-field-2>=<jcr-property-2>''',
            propertyPrivate = false,
            cardinality = Integer.MAX_VALUE)
    private Map<String, FieldMapBean> configMap = new HashMap<String, FieldMapBean>()

    @Activate
    protected void activate(ComponentContext componentContext) {
        Dictionary properties = componentContext.getProperties()
        log.debug 'Activate method | extract metadata mappings from service configurations'

        (properties.get(METADATA_MAPPING_VALUE)).each { String obj ->
            //obj example [poc/components/pages/generalPage@pagetitle=jcr:title;pagedescription=jcr:description]
            String[] parts = obj.split(RESOURCE_SEPARATOR)
            if (parts.length == 2) {
                String resourcetype = parts[0].trim() // example poc/components/pages/generalPage
                String[] mappingArray = parts[1].trim().split(PROPERTIES_SEPARATOR) // example pagetitle=jcr:title;pagedescription=jcr:description

                Map<String, String> jcrSolrMap = [:]

                mappingArray.each { String val ->
                    String[] propertyArray = val.split(PROPERTY_SEPARATOR)

                    if (propertyArray.length == 2) {
                        jcrSolrMap.put(propertyArray[1], propertyArray[0])

                        log.debug "Resourcetype = ${resourcetype}, jcr-property = ${propertyArray[1]}, solr-field = ${propertyArray[0]}"
                    }
                }

                configMap.put(resourcetype, new FieldMapBean(resourcetype, jcrSolrMap))
            }
        }
    }

    @Modified
    protected void modified(ComponentContext componentContext) {
        log.debug 'Modified method called. Delegating to activate method'
        activate(componentContext)
    }

    String extractContent(Resource resource) {
        extractContent(resource, false)
    }

    String extractContent(Resource resource, boolean recursive) {
        StringWriter writer = new StringWriter()
        MarkupBuilder xml = new MarkupBuilder(writer)
        xml.mkp.xmlDeclaration(version: '1.0', encoding: 'UTF-8')
        xml.add() {
            extractXml(resource, recursive, xml)
        }
        return writer.toString()
    }

    private void extractXml(Resource resource, boolean recursive, MarkupBuilder xml) {

        if (resource) {
            Resource contentResource = resource.getChild('jcr:content')

            if (!contentResource) {
                log.error('Cannot extract XML content for invalid content resource under ${resource}')
                return
            }

            ValueMap pageProperties = contentResource.adaptTo(ValueMap) ?: ValueMap.EMPTY

            if (!pageProperties)
                log.trace("No properties for resource ${resource}")

            String pageResourceType = pageProperties.get(ResourceResolver.PROPERTY_RESOURCE_TYPE, '')
            String lastReplicationAction = pageProperties?.get('cq:lastReplicationAction', '')

            log.trace("Last replication action for ${resource}: ${lastReplicationAction}")

            if (!configMap.containsKey(pageResourceType))
                log.trace("Resource type ${pageResourceType} for ${resource} is not accepted. Accepted resource types are: ${configMap.toMapString()}")

            if (lastReplicationAction.equals('Activate') && configMap.containsKey(pageResourceType)) {
                xml.doc() {
                    field(name: 'id', resource.path)
                    extractXmlFromResource(contentResource, xml)
                }
            }
            if (recursive) {
                resource?.listChildren().each { Resource child ->
                    extractXml(child, recursive, xml)
                }
                log.trace "Completed XML Content extraction of ${resource.path}"
            }
        } else {
            log.error('Cannot extract XML content for invalid resource')
        }
    }

    private void extractXmlFromResource(Resource resource, MarkupBuilder xml) {
        traverseProperties(resource, xml)

        resource.listChildren().each { Resource childResource ->
            extractXmlFromResource(childResource, xml)
        }
    }

    private void traverseProperties(Resource resource, MarkupBuilder xml) {
        String resourcetype = resource.getResourceType()
        log.trace "ResourceType = ${resourcetype} and Resource path : ${resource.path}"
        FieldMapBean fieldMapBean
        fieldMapBean = configMap.get(resourcetype)


        if (fieldMapBean) {
            Map<String, String> fieldMap = fieldMapBean.fieldMap as Map

            fieldMap.each { String key, String value ->
                log.trace "FieldMapBean ${key}: ${value}"
                Node node = resource.adaptTo(Node)
                ValueMap valueMap = resource.adaptTo(ValueMap)
                if (node.hasProperty(key)) {
                    javax.jcr.Property property = node.getProperty(key)
                    extractProperty(key, value, resource.resourceResolver, property, valueMap, xml)
                } else if (key.equals(MAIN_CONTENT_PAR) && node.hasNode(MAIN_CONTENT_PAR)) { //handles poc courses because courses are made up of multiple RTEs
                    log.trace "Found ${MAIN_CONTENT_PAR} @ ${node.path}"
                    Node mainPar = node.getNode(MAIN_CONTENT_PAR)

                    String content = getMainParContent(mainPar)

                    if (content) {
                        log.trace "Course description ${key}: ${content}"
                        xml.field(name: value, TextUtil.removeHTML(content))
                    }
                } else if (key.equals('jcr:created')) {
                    Node parent = node.getParent()
                    if (parent.hasProperty(key)) {
                        javax.jcr.Property property = parent.getProperty(key)
                        valueMap = resource.getParent().adaptTo(ValueMap)
                        extractProperty(key, value, resource.parent.resourceResolver, property, valueMap, xml)
                    }
                }
            }
        }
    }


    private void extractProperty(String key, String value, ResourceResolver resourceResolver, javax.jcr.Property property, ValueMap valueMap, MarkupBuilder xml) {
        int type = property.getType()
        //Iterate over properties and based on type (either of String, Boolean, Date, Long, Double, Decimal) extract content to be indexed.
        if ((type == PropertyType.STRING || type == PropertyType.BOOLEAN || type == PropertyType.LONG || type == PropertyType.DOUBLE || type == PropertyType.DECIMAL)) {

            // If value is multi-valued then generate as many 'field' tags
            if (property.isMultiple()) {
                log.trace "${PropertyType.nameFromValue(type)} ${key} property is Multiple"
                valueMap.get(key, String[]).each { String val ->
                    if (val) {
                        String tval = TextUtil.removeHTML(val)
                        if (key.equals('cq:tags')) {
                            TagManager tagManager = resourceResolver.adaptTo(TagManager)
                            Tag pocTag = tagManager.resolve(tval)
                            String tagInSolr = StringUtils.isNotBlank(pocTag.title) ? pocTag.title : WordUtils.capitalize(pocTag.name)

                            tval = tval.replaceAll(':', '/')
                            if (tval.startsWith('poc')) {
                                String[] tag = tval.split('/')
                                log.info("tag: ${tag[0]}/${tag[1]}")
                                if (tag)
                                    xml.field(name: value, tagInSolr)
                            }
                        }
                        /************* Changes done to incorporate Solr Search ************************/
                        if (!(key.equals('cq:tags')))
                            xml.field(name: value, TextUtil.removeHTML(tval))
                    }
                }
            } else {
                if (valueMap.get(key, String)) {
                    log.trace "${PropertyType.nameFromValue(type)} ${key} property is Singular. Value = ${valueMap.get(key, String)}"
                    xml.field(name: value, TextUtil.removeHTML(valueMap.get(key, String)))
                }
            }
        } else if (type == PropertyType.DATE) {
            // Date handling
            if (valueMap.get(key, Calendar)) {
                log.trace "Date '${key}' property. Value = ${valueMap.get(key, Date).toString()}"
                Calendar contentDate = valueMap.get(key, Calendar)

                // Convert to UTC as Solr only accepts Dates in UTC
                DateTime dateTime = new DateTime(contentDate, DateTimeZone.UTC)
                xml.field(name: value, dateTime.toString(SOLR_DATE_FIELD_FORMAT))
            }
        }

    }

    /**
     * Recursive function to traverse the main content parsys for Solr POC and retrieve text.
     * @param node
     * @return
     */
    private String getMainParContent(Node node) {
        log.trace("Diving into mainParsys, current node: ${node.path}")
        String content = ''
        if (node.hasProperty('text')) {
            content += " ${node.getProperty('text').getString()}"
        } else if (node.hasProperty('body')) {
            content += " ${node.getProperty('body').getString()}"
        }

        if (node.hasNodes()) {
            node.getNodes().each { n ->
                content += getMainParContent(n)
            }
        }
        return content
    }
}
