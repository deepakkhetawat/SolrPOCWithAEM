package com.solrpoc.core.solr.service.impl

import com.solrpoc.core.solr.model.Facet
import com.solrpoc.core.solr.model.SlingSolrSearchRequest
import com.solrpoc.core.solr.model.SolrSearchResult
import com.solrpoc.core.solr.model.SolrSearchResults
import com.solrpoc.core.solr.service.ResultConfiguration
import groovy.util.logging.Slf4j
import org.apache.felix.scr.annotations.*
import org.apache.sling.api.SlingHttpServletRequest
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.SolrServerException
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.client.solrj.response.FacetField
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrDocumentList
import org.apache.solr.common.SolrException
import org.apache.solr.common.params.CommonParams
import org.osgi.service.component.ComponentContext

@Slf4j
@Component(label = 'Solrpoc - Search results configuration service for Solr results',
        description = 'Configure fields that need to be extracted from Solr server search results.', metatype = true, immediate = true, policy = ConfigurationPolicy.REQUIRE)
@Service(ResultConfiguration)
class ResultConfigurationImpl implements ResultConfiguration {


    public static final String SOLR_SEARCH_COLLECTION_NAME = 'solr.search.collection.name'

    public static final String SOLR_SEARCH_TITLE = "title:"
    public static final String SOLR_SEARCH_CREATED_DATE= "date_created:"
    public static final String OR_CONDITION = " OR "
    public static final String AND_CONDITION = " AND "
    public static final String SOLR_SEARCH_DESCRIPTION = "description:"
    public static final String SOLR_SEARCH_CONTENT = "content:"
    List<Facet> tagsSolrFacetList = []



    @Property(name = 'solr.search.collection.name',
            label = 'Solr Core Name',
            description = 'Name of Solr Core')
    private String solrPOCCore

    @Activate
    protected void activate(ComponentContext componentContext) {
        Dictionary properties = componentContext.getProperties()
        log.debug 'Activate method | extract search results field mappings from service configurations'

        solrPOCCore = properties.get(SOLR_SEARCH_COLLECTION_NAME)

    }

    @Modified
    protected void modified(ComponentContext componentContext) {
        log.debug 'Modified method called. Delegating to activate method'
        activate(componentContext)
    }


    private QueryResponse getQueryResponse(SlingSolrSearchRequest request) {

        if (!request) {
            log.error('request is null')
            throw new IllegalArgumentException('request is null')
        }
        // build the solr query
        SolrQuery queryParams = prepareQuery(request)
        SolrClient solrClient = getSolrClient()
        log.info('solrClient--------------------------- ' + solrClient)
        QueryResponse queryResponse = null
        try {
            log.info('queryParams--------------------------- ' + queryParams)
            queryResponse = solrClient.query(queryParams)
        }
        catch (SolrServerException solrException) {
            log.error(solrException.getMessage())
        }
        catch (SolrException solrException) {
            log.error(solrException.getMessage())
        }
        return queryResponse
    }

    @Override
    SolrSearchResults getSearchResults(SlingSolrSearchRequest request) {
        SolrSearchResults solrSearchResults = new SolrSearchResults()
        QueryResponse queryResponse = getQueryResponse(request)

        SolrDocumentList solrDocumentList = queryResponse?.results
        List<SolrSearchResult> solrResults = new ArrayList<SolrSearchResult>()
        if (solrDocumentList) {
            for (SolrDocument solrDocument : solrDocumentList) {
                String id = solrDocument.get("id")
                String tags = solrDocument.get("tags")
                String title = solrDocument.get("title")
                String description = solrDocument.get("description")
                String content = solrDocument.get("content")
                String date_created = solrDocument.get("date_created")

                solrResults.add(new SolrSearchResult(id: id, title: title, description: description, content: content, date_created: date_created, tags: tags))
            }

            calculateFacetList(queryResponse.getFacetField("tags"), request.httpServletRequest)
            solrSearchResults.totalResults = solrDocumentList?.numFound ?: 0
            int results_per_page = request.resultsPerPage
            solrSearchResults.totalTabs = (int) solrSearchResults.totalResults / results_per_page
            solrSearchResults.tagsSolrFacetList = tagsSolrFacetList
        }
        solrSearchResults.searchResults = solrResults

        return solrSearchResults
    }

    /**
     * This method adds facet to tags facet list.
     * @param facetField
     * @param request
     */
  void calculateFacetList(FacetField facetField, SlingHttpServletRequest request) {

          tagsSolrFacetList?.clear()
        if (facetField.valueCount != 0) {
            List<FacetField.Count> facetFieldCounts = facetField.values

            facetFieldCounts?.each { FacetField.Count count ->
                Facet facet = getFacetPOJO(count, facetField.name, request)
                tagsSolrFacetList.add(facet)
            }
        }
    }

    /**
     * This method prepares facet POJO
     * @param request
     * @param count
     * @param facetFieldName
     * @return Facet
     */
     Facet getFacetPOJO(FacetField.Count count, String facetFieldName, SlingHttpServletRequest request) {

        String facetString = facetFieldName + ":" + count.name
        String facetQuery = request.getParameter("query") + AND_CONDITION + facetString

        String facetTitle = count.name

        return new Facet(facetTitle: facetTitle, facetCount: String.valueOf(count.getCount()),facetQuery: facetQuery)

    }

    @Override
    SolrSearchResults getSearchResults(SlingHttpServletRequest request) {

        SlingSolrSearchRequest slingRequest = new SlingSolrSearchRequest(request);
        String query = SOLR_SEARCH_TITLE + slingRequest.query + OR_CONDITION + SOLR_SEARCH_DESCRIPTION + slingRequest.query + OR_CONDITION + SOLR_SEARCH_CONTENT + slingRequest.query
        slingRequest.query = query
        return getSearchResults(slingRequest)
    }


    private SolrQuery prepareQuery(SlingSolrSearchRequest request) {
        if (!request) {
            log.warn('no cores defined, not able to perform any searches')
            return null
        }
        String pagePathQuery = "id:*" + AND_CONDITION + SOLR_SEARCH_CREATED_DATE + "[2016-02-01T23:59:59.999Z TO NOW]"
        String query = request.query
        log.info('query is---- ' + query)
        if (!query) {
            log.warn('no query specified, not able to perform any searches')
            return null
        }

        SolrQuery queryParams = new SolrQuery()
        queryParams.set(CommonParams.Q, query)
        queryParams.setFilterQueries(pagePathQuery)
        queryParams.set('')

        queryParams.setStart(request.resultsPerPage * (request.pageNumber - 1))
        queryParams.setRows(request.resultsPerPage)

        // make the query use and conditions instead of the default OR
        queryParams.set('q.op', 'AND')
        queryParams.set('defType', 'edismax')
        queryParams.set('lowercaseOperators', 'true')
        queryParams.setFacet(true);
        queryParams.setFacetMinCount(1)
        queryParams.addFacetField('tags')
        if (request.searchType == 'text-search')
            return queryParams
    }


    private SolrClient getSolrClient() {
        SolrClient solrClient = new HttpSolrClient("http://localhost:8983/solr/solrpoc")
        return solrClient
    }


}
