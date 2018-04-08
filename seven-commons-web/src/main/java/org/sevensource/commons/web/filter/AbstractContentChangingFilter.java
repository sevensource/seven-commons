package org.sevensource.commons.web.filter;

import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sevensource.commons.web.servlet.BufferingHttpResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class AbstractContentChangingFilter extends AbstractOutputBufferingFilter {

	private static final Logger logger = LoggerFactory.getLogger(AbstractContentChangingFilter.class);

	private static final Pattern HTML_ONLY_PATTERN = Pattern.compile(
			"\\.(bmp|css|csv|doc|docx|eot|flv|gif|gz|ico|jpeg|jpg|js|mp[34]|pdf|png|rtf|svg|swf|tif{1,2}|ttf|txt|webp|woff|woff2|xls|xlsx|xml|zip)$",
			Pattern.CASE_INSENSITIVE);

	private boolean filterHtmlOnly = true;
	private boolean handleSuccessfulResponseOnly = true;
	private int maxProcessingContentLength = 1024*1024;



	@Override
	protected boolean skipExecution(HttpServletRequest request, HttpServletResponse response) {
		if(filterHtmlOnly) {
			final String uri = request.getRequestURI();
			if (HTML_ONLY_PATTERN.matcher(uri).find()) {
				return true;
			}
		}

		return false;
	}

	@Override
	protected boolean skipHandleResponse(HttpServletRequest request, BufferingHttpResponseWrapper response) {
		if(handleSuccessfulResponseOnly && !isSuccessfulResponse(response)) {
			return true;
		} else if(filterHtmlOnly && !isHtmlContentType(response)) {
			return true;
		} else if(maxProcessingContentLength > 0 && isResponseLarger(response, maxProcessingContentLength)) {
			if (logger.isInfoEnabled()) {
				logger.info("Skipping filter execution for filter {} and URI {} due to response size {}", getFilterName(), request.getRequestURI(), response.getBufferSize());
			}
			return true;
		}

		return false;
	}

	private boolean isSuccessfulResponse(HttpServletResponse response) {
		final int status = response.getStatus();

		if(status == 0) {
			if (logger.isWarnEnabled()) {
				logger.warn("HTTP status is 0 - returning true on isSuccessfulResponse");
			}
			return true;
		}

		return (status >= 200 && status < 300);
	}

	private boolean isHtmlContentType(HttpServletResponse response) {
		final String contentType = response.getContentType();
		return (contentType != null && contentType.toLowerCase().startsWith("text/html"));
	}

	private boolean isResponseLarger(HttpServletResponse response, int max) {
		return response.getBufferSize() > max;
	}
}