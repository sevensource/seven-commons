package org.sevensource.commons.web.filter.tidy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.sevensource.commons.web.filter.tidy.HtmlTidyProcessor.TidyProcessorOption;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;

class StyleRelocator extends RelocatorSupport {

	private final boolean relocateStyleToHead;
	private final boolean relocateLinkedStylesheet;
	private final boolean removeDuplicates;

	StyleRelocator(Set<TidyProcessorOption> processorOptions) {
		this.relocateStyleToHead = processorOptions.contains(TidyProcessorOption.RELOCATE_STYLES_TO_HEAD);
		this.relocateLinkedStylesheet = processorOptions.contains(TidyProcessorOption.RELOCATE_STYLESHEETS);
		this.removeDuplicates = processorOptions.contains(TidyProcessorOption.REMOVE_DUPLICATE_STYLES);

	}

	@Override
	protected List<Element> getAllElements(Element body) {
		final List<Element> styles = new ArrayList<>();
		styles.addAll(body.getAllElements(HTMLElementName.STYLE));
		styles.addAll(body.getAllElements(HTMLElementName.LINK).stream()
				.filter(e -> "stylesheet".equalsIgnoreCase(e.getAttributeValue("rel")))
				.sorted((e1, e2) -> Integer.compare(e1.getRowColumnVector().getRow(), e2.getRowColumnVector().getRow()))
				.collect(Collectors.toList()));
		return styles;
	}

	@Override
	protected RelocatorContext buildContext(Element tag, Set<Integer> alreadySeen) {

		final RelocatorContext ctx = new RelocatorContext();

		if (HTMLElementName.STYLE.equalsIgnoreCase(tag.getName())) {
			ctx.contentHash = tag.getContent().toString().trim().hashCode();
			ctx.relocate = relocateStyleToHead;
			ctx.relocateTo = RelocateLocation.END_OF_HEAD;
		} else {
			final String href = tag.getAttributeValue("href");
			ctx.contentHash = href == null ? 0 : href.hashCode();
			ctx.relocate = relocateLinkedStylesheet;
			ctx.relocateTo = null;
		}

		ctx.removeDuplicates = this.removeDuplicates;
		ctx.removeAsyncDeferAttributes = true;

		return ctx;
	}
}
