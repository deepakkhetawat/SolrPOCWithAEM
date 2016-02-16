package com.solrpoc.core.solr.servlet

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import org.apache.felix.scr.annotations.sling.SlingServlet
import org.apache.felix.scr.annotations.Activate
import org.apache.felix.scr.annotations.Modified
import org.apache.felix.scr.annotations.Property
import org.apache.felix.scr.annotations.Reference
import org.apache.sling.api.SlingHttpServletRequest
import org.apache.sling.api.SlingHttpServletResponse
import org.apache.sling.api.resource.Resource
import org.apache.sling.api.resource.ResourceResolver
import org.apache.sling.api.servlets.SlingAllMethodsServlet
import org.apache.sling.commons.osgi.PropertiesUtil
import org.osgi.service.component.ComponentContext

import com.solrpoc.core.solr.service.ContentExtractor
import com.day.cq.wcm.api.Page
import com.day.cq.wcm.api.PageManager

@CompileStatic
@Slf4j
@SlingServlet(label = 'POC Content Servlet for Solr re-indexing', metatype = true, name = 'com.solrpoc.core.solr.servlet.ContentServlet',
        paths = ['/bin/solr/content', '/bin/solr/paths/pages', '/bin/solr/paths/assets'],
        extensions = 'xml')
public class ContentServlet extends SlingAllMethodsServlet {

    private static final String PATH_PAGES = "/bin/solr/paths/pages"

    @Reference
    ContentExtractor contentExtractor

    public static final String PROP_PARENT_PAGE_PATH = 'parent.page.paths'

    @Property(name = 'parent.page.paths',
            label = 'Parent Page Paths',
            description = 'Configure parent page paths. Child pages for the configured parent page paths would be returned in response.',
            propertyPrivate = false,
            cardinality = Integer.MAX_VALUE)
    private List<String> childPageList

    @Activate
    protected void activate(ComponentContext componentContext) {
        Dictionary properties = componentContext.getProperties()
        log.debug 'Activate method | Content Servlet - Creating List of configured parent pages'

        this.childPageList = properties.get(PROP_PARENT_PAGE_PATH) as List<String>
    }

    @Modified
    protected void modified(ComponentContext componentContext) {
        log.debug 'Modified method | Content Servlet. Delegating to activate method'
        activate(componentContext)
    }

    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {

        if (request.getRequestPathInfo().getResourcePath().equals(PATH_PAGES)) {
            log.debug 'Solr ContentServlet | Request path pages = ${PATH_PAGES}'
            response.setContentType('text/plain;charset=UTF-8')
            PageManager pageMgr = request.getResourceResolver().adaptTo(PageManager.class)
            StringBuffer str = new StringBuffer()

            childPageList.each { String parentPagePath ->
                Page page = pageMgr.getPage(parentPagePath)

                if (page) {
                    Iterator<Page> pageItr = page.listChildren() // Use a custom filter that respects hide-in-search property

                    pageItr.each { Page childPage ->
                        str.append(childPage.path).append(';')
                    }
                }
            }

            response.getWriter().write(str.toString())

        } else {

            response.setContentType('application/xml;charset=UTF-8')

            String path = request.getParameter('path')
            String recursive = request.getParameter('recursive')
            final boolean isRecursive = recursive && recursive.equalsIgnoreCase('true')

            log.debug "Get content for path = ${path} and recursive = ${recursive} "

            ResourceResolver resourceResolver = request.getResource().getResourceResolver()
            Resource resource = resourceResolver.resolve(path)

            if (resource) {
                response.getWriter().write(contentExtractor.extractContent(resource, isRecursive))
            } else {
                log.error "Error retrieving content for SOLR, Page at ${path} could not be found"
            }
        }
    }

}
