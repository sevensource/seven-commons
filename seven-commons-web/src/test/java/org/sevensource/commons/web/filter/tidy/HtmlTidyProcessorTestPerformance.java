package org.sevensource.commons.web.filter.tidy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.sevensource.commons.web.filter.tidy.HtmlTidyProcessor.TidyProcessorFormatter;
import org.sevensource.commons.web.filter.tidy.HtmlTidyProcessor.TidyProcessorOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HtmlTidyProcessorTestPerformance {

	
	private static final Logger logger = LoggerFactory.getLogger(HtmlTidyProcessorTestPerformance.class);
	
	
	private final static String PERFORMANCE_COMPLEX_TEST_FILE = "src/test/resources/tidy_test_complex_performance.html";
	private final static String PERFORMANCE_SIMPLE_TEST_FILE = "src/test/resources/tidy_test_simple_performance.html";

	
	
	class PerfResult {
		long avg;
		long sizeIn;
		long sizeOut;
	}
	
	String format = "%-25s %-30s %-30s %-30s %-20s\n";
	
	String formatC1 = "%-8s (%6d byte)";
	String formatC2 = "%6d μs (%6d byte)";
	String formatC3 = "%02.2f%%";
	
	@Test
	public void performance() throws IOException {
		logger.info("");
		System.out.format(format, " ", "Compact", "Format", "NONE", "SizeDiff");
		
		doPerformance("complex", PERFORMANCE_COMPLEX_TEST_FILE);
		doPerformance("simple", PERFORMANCE_SIMPLE_TEST_FILE);
	}
	
	private void doPerformance(String name, String file) throws IOException {
		PerfResult resF = performance(file, TidyProcessorFormatter.FORMAT);
		PerfResult resN = performance(file, TidyProcessorFormatter.NONE);
		PerfResult resC = performance(file, TidyProcessorFormatter.COMPACT);
		
		System.out.format(format, String.format(formatC1, name, resC.sizeIn), 
				String.format(formatC2, resC.avg, resC.sizeOut),
				String.format(formatC2, resF.avg, resF.sizeOut),
				String.format(formatC2, resN.avg, resN.sizeOut),
				String.format(formatC3, ((double)(resN.sizeOut - resC.sizeOut)) / ((double)resC.sizeOut) * 100));
	}

	
	
	
	private PerfResult performance(String file, TidyProcessorFormatter formatter) throws IOException {
		Set<TidyProcessorOptions> options = EnumSet.allOf(TidyProcessorOptions.class);
		
		final HtmlTidyProcessor p = new HtmlTidyProcessor(options, formatter);

		PerfResult res = new PerfResult();
		
		InputStream sampleDocInputStream = new FileInputStream(file);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		res.sizeIn = IOUtils.copy(sampleDocInputStream, baos);
		
		byte[] data = baos.toByteArray();

		int samples = 1000;
		Instant startTime = Instant.now();
		
		for (int i = 0; i < samples; i++) {
			ByteArrayInputStream is = new ByteArrayInputStream(data);
			p.process(is);
		}
		
		long duration = startTime.until(Instant.now(), ChronoUnit.MICROS);
		long avg = duration / samples;
		
		
		res.avg = avg;
		
		ByteArrayInputStream is = new ByteArrayInputStream(data);
		InputStream processed = p.process(is);
		res.sizeOut = IOUtils.copy(processed, baos);
		return res;
	}
}
