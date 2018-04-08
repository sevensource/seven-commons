package org.sevensource.commons.web.filter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sevensource.commons.web.servlet.BufferingHttpResponseWrapper;

/**
 * Base class for ServletFilters, that wish to buffer and optionally change the
 * response sent to the client
 *
 * @author pgaschuetz
 *
 */
public abstract class AbstractOutputBufferingFilter implements Filter {

	private static final String ALREADY_FILTERED_SUFFIX = ".FILTERED";

	private FilterConfig filterConfig;
	private String filterName;

	private boolean filterOncePerRequest = true;
	private boolean addContentLengthHeader = true;


	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		this.filterConfig = filterConfig;

		this.filterName = this.filterConfig != null ? this.filterConfig.getFilterName() : null;
		if (this.filterName == null) {
			this.filterName = getClass().getName();
		}
	}

	@Override
	public void destroy() {
		this.filterConfig = null;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
			throw new ServletException(String.format("%s only supports HTTP requests", getClass().getName()));
		}

		final HttpServletRequest httpRequest = (HttpServletRequest) request;
		final HttpServletResponse httpResponse = (HttpServletResponse) response;

		boolean alreadyFiltered = false;
		String alreadyFilteredAttributeName = null;

		if(filterOncePerRequest) {
			alreadyFilteredAttributeName = getAlreadyFilteredAttributeName();
			alreadyFiltered = httpRequest.getAttribute(alreadyFilteredAttributeName) != null;
		}


		if (alreadyFiltered || skipExecution(httpRequest, httpResponse)) {
			// Proceed without invoking this filter...
			chain.doFilter(request, response);
		} else {
			// Do invoke this filter...
			if(filterOncePerRequest) {
				httpRequest.setAttribute(alreadyFilteredAttributeName, Boolean.TRUE);
			}

			try {
				final BufferingHttpResponseWrapper responseWrapper = new BufferingHttpResponseWrapper(httpResponse);
				chain.doFilter(httpRequest, responseWrapper);
				afterDoFilter(httpRequest, httpResponse, responseWrapper);
			}
			finally {
				// Remove the "already filtered" request attribute for this request.
				if(filterOncePerRequest) {
					request.removeAttribute(alreadyFilteredAttributeName);
				}
			}
		}
	}


	private void afterDoFilter(HttpServletRequest request, HttpServletResponse response, BufferingHttpResponseWrapper responseWrapper) throws IOException {

		responseWrapper.flushBuffer();

		if(skipHandleResponse(request, responseWrapper)) {
			writeContentLengthHeader(response, responseWrapper.getBufferSize());
			responseWrapper.writeBufferTo(response.getOutputStream());
		} else {
			final InputStream handledResponseInputStream = handleResponse(request, responseWrapper);
			writeContentLengthHeader(response, responseWrapper.getBufferSize());

			final OutputStream os = response.getOutputStream();
	    	final byte[] buffer = new byte[4096];
	        int n;
	        while (-1 != (n = handledResponseInputStream.read(buffer))) {
	            os.write(buffer, 0, n);
	        }
		}

		response.flushBuffer();
	}

	protected String getFilterName() {
		return filterName;
	}

	protected String getAlreadyFilteredAttributeName() {
		return getFilterName() + ALREADY_FILTERED_SUFFIX;
	}


	private void writeContentLengthHeader(HttpServletResponse response, int contentLength) {
		if(addContentLengthHeader) {
			response.setContentLength(contentLength);
		}
	}


	/**
	 * Should the execution of this filter be skipped?
	 *
	 * @param request
	 * @param response
	 * @return If true, the request is handed down the chain as usual.
	 */
	protected abstract boolean skipExecution(HttpServletRequest request, HttpServletResponse response);

	/**
	 * Should handling of the response be skipped?
	 * This method is called <b>after</b> the request has been handled by the filter chain
	 *
	 * @param request
	 * @param response
	 * @return if true, the response is immediately committed to the underlying response
	 */
	protected abstract boolean skipHandleResponse(HttpServletRequest request, BufferingHttpResponseWrapper response);


	/**
	 * handle the response, optionally do something with it and return the InputStream which
	 * shall be written to the underlying response
	 *
	 * @param request
	 * @param response
	 * @return the new response, that should be written to the underlying {@link HttpServletResponse}
	 * @throws IOException
	 */
	protected abstract InputStream handleResponse(HttpServletRequest request, BufferingHttpResponseWrapper response) throws IOException;

}