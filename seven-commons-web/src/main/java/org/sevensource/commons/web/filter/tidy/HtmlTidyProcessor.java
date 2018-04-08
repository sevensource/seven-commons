package org.sevensource.commons.web.filter.tidy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import org.sevensource.commons.web.util.FastByteArrayOutputStream;

import net.htmlparser.jericho.MicrosoftConditionalCommentTagTypes;
import net.htmlparser.jericho.OutputDocument;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.SourceCompactor;
import net.htmlparser.jericho.StartTag;
import net.htmlparser.jericho.StartTagType;
import net.htmlparser.jericho.WhiteSpaceRespectingSourceFormatter;

/**
 * A HTML processor, which
 * <ul>
 *   <li>Relocates &lt;style&gt; tags contained inside <i>body</i> into <i>head</i>
 *   <li>Relocates &lt;link rel=stylesheet&gt; tags contained inside <i>body</i>
 *   	<ul>
 *   		<li>if the async or defer attribute is set on <i>link</i> it is moved to the end of <i>body</i> and the attribute is removed
 *   		<li>otherwise into the documents <i>head</i>
 *   	</ul>
 *   <li>Relocates &lt;script&gt; tags contained inside <i>body</i>
 *      <ul>
 *   		<li>if the async or defer attribute is set on <i>script</i> it is moved to the end of <i>body</i>
 *   		<li>otherwise into the documents <i>head</i>
 *   	</ul>
 *   <li>Removes duplicated &lt;script&gt;, &lt;link rel=stylesheet&gt; and &lt;style&gt; tags
 *   <li>Removes HTML comments
 *   <li>compacts or beautifies the resulting document
 *
 * @see TidyProcessorOption
 * @see TidyProcessorFormatter
 *
 * @author pgaschuetz
 *
 */
public class HtmlTidyProcessor {


	public enum TidyProcessorOption {
		REMOVE_COMMENTS,
		RELOCATE_STYLES_TO_HEAD,
		RELOCATE_STYLESHEETS,
		REMOVE_DUPLICATE_STYLES,
		RELOCATE_SCRIPTS,
		REMOVE_DUPLICATE_SCRIPTS,
		MINIFY_SCRIPTS
	}

	public enum TidyProcessorFormatter {
		NONE, FORMAT, COMPACT;
	}

	private final Set<TidyProcessorOption> processorOptions;
	private final TidyProcessorFormatter processorFormatter;

	private final StyleRelocator styleRelocator;
	private final ScriptRelocator scriptRelocator;
	private final ScriptMinifier scriptMinifier = new ScriptMinifier();

	static {
		MicrosoftConditionalCommentTagTypes.register();
		DummyMicrosoftStartTag.INSTANCE.register();
	}

	public HtmlTidyProcessor(Set<TidyProcessorOption> processorOptions, TidyProcessorFormatter processorFormatter) {
		this.processorOptions = processorOptions;
		this.processorFormatter = processorFormatter;
		this.styleRelocator = new StyleRelocator(this.processorOptions);
		this.scriptRelocator = new ScriptRelocator(this.processorOptions);
	}


	public InputStream process(InputStream is) throws IOException {
		final Source source = new Source(is);
		return doProcess(source);
	}

	private InputStream doProcess(Source source) throws IOException {

		final OutputDocument outputDocument = new OutputDocument(source);

		if (processorOptions.contains(TidyProcessorOption.REMOVE_COMMENTS)) {
			removeComments(source, outputDocument);
		}

		if (processorOptions.contains(TidyProcessorOption.RELOCATE_STYLES_TO_HEAD) ||
				processorOptions.contains(TidyProcessorOption.RELOCATE_STYLESHEETS) ||
				processorOptions.contains(TidyProcessorOption.REMOVE_DUPLICATE_STYLES)) {
			styleRelocator.relocate(source, outputDocument);
		}

		if (processorOptions.contains(TidyProcessorOption.RELOCATE_SCRIPTS) ||
				processorOptions.contains(TidyProcessorOption.REMOVE_DUPLICATE_SCRIPTS)) {
			scriptRelocator.relocate(source, outputDocument);
		}

		if (processorOptions.contains(TidyProcessorOption.MINIFY_SCRIPTS)) {
			scriptMinifier.minify(source, outputDocument);
		}

		final int bufferSize = Math.max(source.getEnd() / 10, 1024);

		final FastByteArrayOutputStream os = new FastByteArrayOutputStream(bufferSize);
		final OutputStreamWriter writer = new OutputStreamWriter(os, StandardCharsets.UTF_8.name());
		outputDocument.writeTo(writer);
		writer.flush();

		final long estimatedSize = outputDocument.getEstimatedMaximumOutputLength();
		return format(os.getInputStream(), estimatedSize);
	}

	private InputStream format(InputStream is, long estimatedSize) throws IOException {
		if(processorFormatter == TidyProcessorFormatter.NONE) {
			return is;
		}

		int bufferSize;
		if(estimatedSize > Integer.MAX_VALUE || estimatedSize < 1) {
			bufferSize = 1024*4;
		} else {
			bufferSize = (int) estimatedSize / 10;
		}

		final Source source = new Source(is);
		final FastByteArrayOutputStream os = new FastByteArrayOutputStream(bufferSize);
		final OutputStreamWriter writer = new OutputStreamWriter(os, StandardCharsets.UTF_8.name());

		try {
			if (processorFormatter == TidyProcessorFormatter.FORMAT) {
				new WhiteSpaceRespectingSourceFormatter(source)
					.setIndentString(" ")
					.setTidyTags(true)
					.setCollapseWhiteSpace(true)
					.writeTo(writer);
			} else if (processorFormatter == TidyProcessorFormatter.COMPACT) {
				new SourceCompactor(source).writeTo(writer);
			} else {
				throw new IllegalArgumentException("Don't know how to handle processorFormatter " + processorFormatter);
			}

			writer.flush();
			return os.getInputStream();
		} finally {
			os.close();
			writer.close();
		}
	}

	/**
	 * removes all HTML comments from the document
	 *
	 * @param source
	 * @param outputDocument
	 */
	private final void removeComments(Source source, OutputDocument outputDocument) {
		final List<StartTag> commentStartTags = source.getAllStartTags(StartTagType.COMMENT);
		outputDocument.remove(commentStartTags);
	}

}