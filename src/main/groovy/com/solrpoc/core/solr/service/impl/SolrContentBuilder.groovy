package com.solrpoc.core.solr.service.impl

import com.day.cq.dam.api.DamConstants
import com.day.cq.replication.*
import com.day.cq.wcm.api.NameConstants
import com.solrpoc.core.solr.service.ContentExtractor
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.felix.scr.annotations.Component
import org.apache.felix.scr.annotations.Property
import org.apache.felix.scr.annotations.Reference
import org.apache.felix.scr.annotations.Service
import org.apache.jackrabbit.JcrConstants
import org.apache.sling.api.resource.*
import org.apache.sling.jcr.resource.JcrResourceConstants

import javax.jcr.Session
import javax.servlet.ServletException

@CompileStatic
@Slf4j
@Component(label = 'POC Solr Replication Content Builder', description = 'Service to serialize replicated Page content over to Solr server',
    immediate = true, enabled = true, metatype = true)
@Service(ContentBuilder)
@Property(name = 'name', value = 'POCSolrReplication', propertyPrivate = true)
class SolrContentBuilder implements ContentBuilder {

    private static final String NAME = 'POCSolrReplication'

    public static final String TITLE = 'POC Solr XML Content Serializer'

    @Reference
    ContentExtractor contentExtractor

    @Reference
    ResourceResolverFactory resourceResolverFactory


    @Override
    public ReplicationContent create(Session session, ReplicationAction repAction,
                                     ReplicationContentFactory repContentFactory) throws ReplicationException {

        String path = repAction.getPath()
        Map map = new HashMap()
        map.put(JcrResourceConstants.AUTHENTICATION_INFO_SESSION, session)
        ResourceResolver resourceResolver = resourceResolverFactory.getResourceResolver(map)

        Resource resource = resourceResolver.resolve(path)
        log.debug "Resource type = ${resource.getResourceType()}"

        if (resource && !ResourceUtil.isNonExistingResource(resource)) {
            ValueMap properties = resource.adaptTo(ValueMap)
            String primaryType = properties.get(JcrConstants.JCR_PRIMARYTYPE, '')
            if (NameConstants.NT_PAGE.equals(primaryType) || (DamConstants.NT_DAM_ASSET.equals(primaryType) && resource.name.contains('.pdf'))) {
                return replicationEvents(resource, repAction.getType(), repContentFactory)
            }
        }
        else {
            // Means resource no more exists. This could happen as a result of a 'Move' operation. The 'old, moved' reference needs to be deleted from Solr.
            return deleteIndexByPath(path, repContentFactory)
        }
        return generateEmptyData(repContentFactory)
    }


    private ReplicationContent replicationEvents(Resource resource, ReplicationActionType repActionType, ReplicationContentFactory factory) throws ServletException, IOException {

        if (repActionType.equals(ReplicationActionType.ACTIVATE)) {
            log.info "Replication Type = ${repActionType.getName()}. Creating/Updating document index id = ${resource.path} at Solr server"
            return createTempReplicationContent(factory, contentExtractor.extractContent(resource))
        }
        else if (repActionType.equals(ReplicationActionType.DEACTIVATE) || repActionType.equals(ReplicationActionType.DELETE)) {
            log.info "Replication Type = ${repActionType.getName()}. Deleting document index id = ${resource.path} from Solr server"
            return deleteIndexByResource(resource, factory)
        }

        return generateEmptyData(factory)
    }

    /**
     * Method to create temporary replication content
     *
     * @param factory
     * @param xmlData
     * @return ReplicationContent
     * @throws IOException
     */
    private ReplicationContent createTempReplicationContent(ReplicationContentFactory factory, String xmlData) throws IOException {
        ReplicationContent repCont
        log.info("Published content: ${xmlData}")
        File tempFile = File.createTempFile("xmlData-${UUID.randomUUID()}", '.xml')
        try {
            tempFile.write(xmlData, 'UTF-8')
            repCont = factory.create('text/xml', tempFile, true)
        }
        finally {
            tempFile.delete()
        }
        repCont
    }


    private ReplicationContent deleteIndexByResource(Resource resource, ReplicationContentFactory factory) {
        return deleteIndexByPath(resource.getPath(), factory)
    }


    private ReplicationContent deleteIndexByPath(String resourcePath, ReplicationContentFactory factory) {
        log.debug "Deleting index for path = ${resourcePath}"
        return createTempReplicationContent(factory, "<delete><id>${resourcePath}</id></delete>")
    }


    private ReplicationContent generateEmptyData(ReplicationContentFactory factory) {
        return createTempReplicationContent(factory, "<add></add>")
    }


    @Override
    public String getName() {
        return NAME
    }


    @Override
    public String getTitle() {
        return TITLE
    }


    @Override
    public ReplicationContent create(Session session, ReplicationAction repAction,
                                     ReplicationContentFactory repContentFactory, Map<String, Object> parameters) throws ReplicationException {

        //do nothing with Map parameters and call default
        return create(session, repAction, repContentFactory)
    }
}
