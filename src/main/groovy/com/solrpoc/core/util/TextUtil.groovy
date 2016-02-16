package com.solrpoc.core.util
import groovy.transform.CompileStatic
import org.jsoup.Jsoup

@CompileStatic
class TextUtil {

	/**
	 * Remove HTML tags contained in the text content, leaving only the plain text behind.
	 * @param text the text content to remove HTML tags from.
	 * @return the text content without HTML tags
	 */
	public static String removeHTML(String text) {
		if (!text)
			return ''
		/* for now, let's just get the plain text without formatting it along the way,
		 * but do convert non breaking spaces into regular spaces. */
		Jsoup.parse(text).text().replaceAll('\u00A0', ' ')
	}

}
