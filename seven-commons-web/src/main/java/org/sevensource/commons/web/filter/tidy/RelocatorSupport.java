package org.sevensource.commons.web.filter.tidy;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.htmlparser.jericho.Attribute;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.OutputDocument;
import net.htmlparser.jericho.Source;

abstract class RelocatorSupport {
		
	private static final Set<String> deferAndAsyncAttrs = new HashSet<>(Arrays.asList("defer", "async"));
	
	RelocatorSupport() {
	}
	
	class RelocatorContext {
		int contentHash;
		boolean removeDuplicates;
		boolean relocate;
		RelocateLocation relocateTo;
		boolean removeAsyncDeferAttributes;
		boolean hasAsyncOrDefer;
	}
	
	enum RelocateLocation {
		END_OF_HEAD,
		END_OF_BODY;
	}
	
	void relocate(Source source, OutputDocument outputDocument) {
		final Element head = source.getFirstElement(HTMLElementName.HEAD);
		final Element body = source.getFirstElement(HTMLElementName.BODY); 
		
		if (body == null || head == null) {
			return;
		}
		
		final List<Element> styles = getAllElements(body);
		final Set<Integer> seen = new HashSet<>();
		
		for (Element startTag : styles) {
			
			final RelocatorContext ctx = buildContext(startTag, seen);
			
			final boolean alreadySeen = ctx.removeDuplicates && seen.contains(ctx.contentHash);			
			seen.add(ctx.contentHash);
			
			CharSequence outTag = startTag;
			
			if(! alreadySeen) {
				if(ctx.relocate && ctx.relocateTo == null) {
					buildContextForAsyncAndDefer(startTag, ctx);
				}
				
				if(ctx.removeAsyncDeferAttributes && ctx.hasAsyncOrDefer) {
					outTag = removeAttributesFromTag(startTag, deferAndAsyncAttrs);
				}
			}
			
			if(alreadySeen) {
				outputDocument.remove(startTag);
			} else if(ctx.relocateTo == RelocateLocation.END_OF_HEAD) {
				outputDocument.insert(head.getEndTag().getBegin() - 1, outTag);
				outputDocument.remove(startTag);
			} else if(ctx.relocateTo == RelocateLocation.END_OF_BODY) {
				outputDocument.insert(body.getEndTag().getBegin() - 1, outTag);
				outputDocument.remove(startTag);
			}
		}
	}
	
	
	private static CharSequence removeAttributesFromTag(Element tag, Set<String> attrs) {
		final Map<String, String> attributesMap = new LinkedHashMap<>();
		tag.getAttributes().populateMap(attributesMap, false);
		
		final Iterator<String> it = attributesMap.keySet().iterator();
		while(it.hasNext()) {
			final String attr = it.next().toLowerCase();
			if(attrs.contains(attr)) {
				it.remove();
			}
		}
		
		final OutputDocument tmpOutDoc = new OutputDocument(tag);
		tmpOutDoc.replace(tag.getAttributes(), attributesMap);
		final StringBuilder sb = new StringBuilder(tag.getEnd() - tag.getBegin());
		
		try {
			tmpOutDoc.appendTo(sb);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		return sb;
	}
	
	private static void buildContextForAsyncAndDefer(Element tag, RelocatorContext ctx) {

		final Attribute asyncAttribute = tag.getAttributes().get("async");
		final boolean hasAsync = asyncAttribute != null;
		final Attribute deferAttribute = tag.getAttributes().get("defer");
		final boolean hasDefer = deferAttribute != null;
		
		ctx.hasAsyncOrDefer = hasAsync || hasDefer;
		
		if(ctx.hasAsyncOrDefer) {
			ctx.relocateTo = RelocateLocation.END_OF_BODY;
		} else {
			ctx.relocateTo = RelocateLocation.END_OF_HEAD;
		}
	}
	
	protected abstract List<Element> getAllElements(Element body);
	protected abstract RelocatorContext buildContext(Element tag, Set<Integer> alreadySeen);
}
