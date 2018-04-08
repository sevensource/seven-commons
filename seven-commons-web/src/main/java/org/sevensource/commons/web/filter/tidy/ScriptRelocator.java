package org.sevensource.commons.web.filter.tidy;

import java.util.List;
import java.util.Set;

import org.sevensource.commons.web.filter.tidy.HtmlTidyProcessor.TidyProcessorOption;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;

class ScriptRelocator extends RelocatorSupport {
	
	private final boolean relocate;
	private final boolean removeDuplicates;
	
	ScriptRelocator(Set<TidyProcessorOption> processorOptions) {
		this.relocate = processorOptions.contains(TidyProcessorOption.RELOCATE_SCRIPTS);
		this.removeDuplicates = processorOptions.contains(TidyProcessorOption.REMOVE_DUPLICATE_SCRIPTS);		
	}

	@Override
	protected List<Element> getAllElements(Element body) {
		return body.getAllElements(HTMLElementName.SCRIPT);
	}

	@Override
	protected RelocatorContext buildContext(Element tag, Set<Integer> alreadySeen) {

		final RelocatorContext ctx = new RelocatorContext();
		
		final String src = tag.getAttributeValue("src");
		
		if (src == null || "".equals(src)) {
			ctx.contentHash = tag.getContent().toString().trim().hashCode();
		} else {
			ctx.contentHash = src.hashCode();
		}
		
		ctx.relocate = relocate;
		ctx.relocateTo = null;
		ctx.removeDuplicates = this.removeDuplicates;
		ctx.removeAsyncDeferAttributes = false;

		return ctx;
	}
}
