package org.sevensource.commons.web.filter.tidy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

import org.sevensource.commons.web.filter.minify.JSMin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.OutputDocument;
import net.htmlparser.jericho.Source;

class ScriptMinifier {

	private static final Logger logger = LoggerFactory.getLogger(ScriptMinifier.class);

	ScriptMinifier() {
	}

	void minify(Source source, OutputDocument outputDocument) {
		final List<Element> elements = source.getAllElements(HTMLElementName.SCRIPT);

		for(Element el : elements) {
			final String src = el.getAttributeValue("src");

			if (src == null || "".equals(src)) {
				final String content = el.getContent().toString();
				final String minified = minifyJs(content);

				final String result = buildReplacement(el, minified);
				outputDocument.replace(el, result);
			}
		}
	}

	private String minifyJs(String content) {
		try {
			final InputStream is = new ByteArrayInputStream(content.getBytes());
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			final JSMin jsMinAlt = new JSMin(is, os);
			jsMinAlt.jsmin();

			final String result = new String(os.toByteArray());
			return result;
		} catch(Exception e) {
			logger.error("Cannot minify javascript", e);
			return content;
		}
	}

	private static String buildReplacement(Element el, String body) {
		final StringBuilder sb = new StringBuilder();
		sb.append( el.getStartTag().toString() );
		sb.append(body);
		sb.append(el.getEndTag().toString());
		return sb.toString();
	}
}
