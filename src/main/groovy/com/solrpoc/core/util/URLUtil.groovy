package com.solrpoc.core.util
import com.day.cq.commons.Externalizer
import com.day.cq.wcm.api.Page
import groovy.util.logging.Slf4j
import org.apache.sling.api.SlingHttpServletRequest
import org.apache.sling.api.resource.ResourceResolver
/**
 * Utilities around URL handling
 */
@Slf4j
public final class URLUtil {
    private URLUtil() {}

    /**
     * Retrieve the absolute URL for a Sling Request. also adds .html to links to pages
     * @param request the SlingHttpServletRequest to use in acquiring the absolute URL
     * @param url the URL to make absolute
     * @return the absolute form of the provided URL
     */
    public static String getAbsoluteUrl(SlingHttpServletRequest request, String url) {

        ResourceResolver rr = request.getResourceResolver();

        if (url == null)
            url = ''
        // if the url is already absolute, do nothing
        if (isAbsoluteUrl(url))
            return url

        boolean isPage = rr.getResource(url)?.adaptTo(Page) != null

        Externalizer externalizer = rr.adaptTo(Externalizer)
        String absoluteUrl = externalizer.absoluteLink(request, request.getScheme(), url)

        // if the mapped url comes out to be the site root, then don't add the .html
        if (isPage) {
            String mappedUrl = rr.map(url)
            if (mappedUrl.length() > 1)
                absoluteUrl += '.html'
        }
        absoluteUrl
    }

    /**
     * Retrieve the state of the provided URL being an absolute URL
     * @param url URL to test being an absolute URL
     * @return state of the URL being absolute
     */
    public static boolean isAbsoluteUrl(String url) {
        // being absolute is identified by having a scheme (which is separated by :// from the rest of the content)
        (url) ? url.contains('://') : false
    }
}
