package org.sevensource.commons.web.filter.tidy;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.sevensource.commons.web.filter.AbstractContentChangingFilter;
import org.sevensource.commons.web.filter.tidy.HtmlTidyProcessor.TidyProcessorFormatter;
import org.sevensource.commons.web.filter.tidy.HtmlTidyProcessor.TidyProcessorOption;
import org.sevensource.commons.web.servlet.BufferingHttpResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @see HtmlTidyProcessor
 *
 * @author pgaschuetz
 *
 */
public class HtmlTidyFilter extends AbstractContentChangingFilter {

	private static final Logger logger = LoggerFactory.getLogger(HtmlTidyFilter.class);

	public static final String OPTIONS_PARAMETER = "options";
	public static final String FORMATTER_PARAMETER = "formatter";

	private HtmlTidyProcessor processor;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		super.init(filterConfig);

		final Set<TidyProcessorOption> options = initOptions(filterConfig);
		final TidyProcessorFormatter formatter = initFormatter(filterConfig);

		this.processor = new HtmlTidyProcessor(options, formatter);
	}

	private static TidyProcessorFormatter initFormatter(FilterConfig filterConfig) {
		TidyProcessorFormatter formatter = TidyProcessorFormatter.NONE;

		String formatterParameter = filterConfig.getInitParameter(FORMATTER_PARAMETER);
		if(formatterParameter != null) {
			formatterParameter = formatterParameter.trim();
			if(formatterParameter.length() > 0) {
				try {
					formatter = TidyProcessorFormatter.valueOf(formatterParameter);
				} catch(IllegalArgumentException e) {
					logger.error("No TidyProcessorFormatter with name {}", formatterParameter);
					throw e;
				}
			}
		}
		return formatter;
	}

	private static Set<TidyProcessorOption> initOptions(FilterConfig filterConfig) {
		final Set<TidyProcessorOption> options = new HashSet<>();

		final String optionsParameter = filterConfig.getInitParameter(OPTIONS_PARAMETER);
		if(optionsParameter != null) {
			String[] optionsSplit = optionsParameter.split(",");
			for(String o : optionsSplit) {
				o = o.trim();
				if(o.length() > 0) {
					try {
						if("all".equalsIgnoreCase(o)) {
							return EnumSet.allOf(TidyProcessorOption.class);
						}

						TidyProcessorOption tpo = TidyProcessorOption.valueOf(o);
						options.add(tpo);
					} catch(IllegalArgumentException e) {
						logger.error("No TidyProcessorOption with name {}", o);
						throw e;
					}
				}
			}
		}
		return options;
	}


	@Override
	protected InputStream handleResponse(HttpServletRequest request, BufferingHttpResponseWrapper response) throws IOException {
		return processor.process(response.getBuffer());
	}
}
