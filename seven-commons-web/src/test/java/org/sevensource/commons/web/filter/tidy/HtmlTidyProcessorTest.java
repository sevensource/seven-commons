package org.sevensource.commons.web.filter.tidy;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.HashSet;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.sevensource.commons.web.filter.tidy.HtmlTidyProcessor.TidyProcessorFormatter;
import org.sevensource.commons.web.filter.tidy.HtmlTidyProcessor.TidyProcessorOption;
import org.xmlunit.matchers.EvaluateXPathMatcher;
import org.xmlunit.matchers.HasXPathMatcher;

public class HtmlTidyProcessorTest {

	private final static String SIMPLE_TEST_FILE = "src/test/resources/tidy_test_simple.html";
	private final static String STYLE_TEST_FILE = "src/test/resources/tidy_test_style.html";
	private final static String SCRIPT_TEST_FILE = "src/test/resources/tidy_test_script.html";
	private final static String SCRIPT_TEST_MINIFY_FILE = "src/test/resources/tidy_test_script_minify.html";

	@Test
	public void processor_works() throws IOException {
		HtmlTidyProcessor p = new HtmlTidyProcessor(new HashSet<>(), TidyProcessorFormatter.NONE);
		InputStream is = new FileInputStream(SIMPLE_TEST_FILE);
		InputStream processed = p.process(is);
		boolean result = IOUtils.contentEquals(processed, new FileInputStream(SIMPLE_TEST_FILE));

		assertThat(result, is(true));
	}

	@Test
	public void compact_works() throws IOException {
		HtmlTidyProcessor p = new HtmlTidyProcessor(new HashSet<>(), TidyProcessorFormatter.COMPACT);
		InputStream is = new FileInputStream(SIMPLE_TEST_FILE);
		InputStream processed = p.process(is);
		String result = IOUtils.toString(processed, StandardCharsets.UTF_8);

		assertThat(result, containsString("<html><head></head>"));
		assertThat(result, containsString("<div>test </div>"));
	}

	@Test
	public void comment_removal_works() throws IOException {
		HtmlTidyProcessor p = new HtmlTidyProcessor(EnumSet.of(TidyProcessorOption.REMOVE_COMMENTS), TidyProcessorFormatter.NONE);
		InputStream is = new FileInputStream(SIMPLE_TEST_FILE);
		InputStream processed = p.process(is);
		String result = IOUtils.toString(processed, StandardCharsets.UTF_8);

		assertThat(result, not(containsString("<!--")));
	}

	@Test
	public void style_relocation_works() throws IOException {
		HtmlTidyProcessor p = new HtmlTidyProcessor(EnumSet.of(TidyProcessorOption.RELOCATE_STYLES_TO_HEAD), TidyProcessorFormatter.NONE);
		InputStream is = new FileInputStream(STYLE_TEST_FILE);
		InputStream processed = p.process(is);
		String result = IOUtils.toString(processed, StandardCharsets.UTF_8);

		assertThat(result, HasXPathMatcher.hasXPath("/html/head/style[1]"));
		assertThat(result, HasXPathMatcher.hasXPath("/html/head/style[2]"));
		assertThat(result, not(HasXPathMatcher.hasXPath("/html/body/style[1]")));
	}

	@Test
	public void style_relocation_and_deduplication_works() throws IOException {
		HtmlTidyProcessor p = new HtmlTidyProcessor(EnumSet.of(TidyProcessorOption.RELOCATE_STYLES_TO_HEAD, TidyProcessorOption.REMOVE_DUPLICATE_STYLES), TidyProcessorFormatter.NONE);
		InputStream is = new FileInputStream(STYLE_TEST_FILE);
		InputStream processed = p.process(is);
		String result = IOUtils.toString(processed, StandardCharsets.UTF_8);

		assertThat(result, HasXPathMatcher.hasXPath("/html/head/style[1]"));
		assertThat(result, not(HasXPathMatcher.hasXPath("/html/head/style[2]")));
		assertThat(result, not(HasXPathMatcher.hasXPath("/html/body/style[1]")));
	}

	@Test
	public void stylesheet_relocation_and_deduplication_works() throws IOException {
		HtmlTidyProcessor p = new HtmlTidyProcessor(EnumSet.of(TidyProcessorOption.RELOCATE_STYLESHEETS, TidyProcessorOption.REMOVE_DUPLICATE_STYLES), TidyProcessorFormatter.NONE);
		InputStream is = new FileInputStream(STYLE_TEST_FILE);
		InputStream processed = p.process(is);
		String result = IOUtils.toString(processed, StandardCharsets.UTF_8);

		assertThat(result, HasXPathMatcher.hasXPath("/html/head/link[1]"));
		assertThat(result, HasXPathMatcher.hasXPath("/html/head/link[2]"));
		assertThat(result, HasXPathMatcher.hasXPath("/html/head/link[3]"));
		assertThat(result, not(HasXPathMatcher.hasXPath("/html/head/link[4]")));

		assertThat(result, HasXPathMatcher.hasXPath("/html/body/link[1]"));
		assertThat(result, HasXPathMatcher.hasXPath("/html/body/link[2]"));
		assertThat(result, HasXPathMatcher.hasXPath("/html/body/link[3]"));
		assertThat(result, HasXPathMatcher.hasXPath("/html/body/link[4]"));
		assertThat(result, not(HasXPathMatcher.hasXPath("/html/body/link[5]")));
	}

	@Test
	public void script_relocation_and_deduplication_works() throws IOException {
		HtmlTidyProcessor p = new HtmlTidyProcessor(EnumSet.of(TidyProcessorOption.RELOCATE_SCRIPTS, TidyProcessorOption.REMOVE_DUPLICATE_SCRIPTS), TidyProcessorFormatter.NONE);
		InputStream is = new FileInputStream(SCRIPT_TEST_FILE);
		InputStream processed = p.process(is);
		String result = IOUtils.toString(processed, StandardCharsets.UTF_8);

		assertThat(result, HasXPathMatcher.hasXPath("/html/head/script[1]"));
		assertThat(result, HasXPathMatcher.hasXPath("/html/head/script[2]"));
		assertThat(result, not(HasXPathMatcher.hasXPath("/html/head/script[3]")));

		assertThat(result, HasXPathMatcher.hasXPath("/html/body/script[1]"));
		assertThat(result, HasXPathMatcher.hasXPath("/html/body/script[2]"));
		assertThat(result, not(HasXPathMatcher.hasXPath("/html/body/script[3]")));
	}

	@Test
	public void script_compact_works() throws IOException {
		HtmlTidyProcessor p = new HtmlTidyProcessor(EnumSet.of(TidyProcessorOption.MINIFY_SCRIPTS), TidyProcessorFormatter.NONE);
		InputStream is = new FileInputStream(SCRIPT_TEST_MINIFY_FILE);
		InputStream processed = p.process(is);
		String result = IOUtils.toString(processed, StandardCharsets.UTF_8);

		System.out.println(result);


		assertThat(result, HasXPathMatcher.hasXPath("/html/body/script[1]"));
		assertThat(result, HasXPathMatcher.hasXPath("/html/body/script[2]"));
		assertThat(result, not(HasXPathMatcher.hasXPath("/html/body/script[3]")));

		assertThat(result, EvaluateXPathMatcher.hasXPath("/html/body/script[1]/text()", equalTo("var wow=1;")));
		assertThat(result, EvaluateXPathMatcher.hasXPath("/html/body/script[2]/text()", equalTo("function test(){wow=\"aha   \";}")));
	}
}
