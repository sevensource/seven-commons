package org.sevensource.commons.web.filter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sevensource.commons.web.filter.BufferingHttpResponseWrapper;

public class BufferingHttpResponseWrapperTest {
	
	HttpServletResponse response;
	
	@Before
	public void beforeEach() {
		response = Mockito.mock(HttpServletResponse.class);
		when(response.getCharacterEncoding()).thenReturn("UTF-8");
	}
	
	@Test
	public void delegates_addHeader_to_underlying() {
		BufferingHttpResponseWrapper wrapper = new BufferingHttpResponseWrapper(response);
		wrapper.addHeader("TEST", "1");
		
		verify(response, times(1)).addHeader("TEST", "1");
	}
	
	@Test(expected=IllegalStateException.class)
	public void cannot_getWriter_after_getOutputStream() throws IOException {
		BufferingHttpResponseWrapper wrapper = new BufferingHttpResponseWrapper(response);
		
		assertThat(wrapper.getOutputStream(), notNullValue());
		wrapper.getWriter();
	}
	
	@Test(expected=IllegalStateException.class)
	public void cannot_getOutputStream_after_getWriter() throws IOException {
		BufferingHttpResponseWrapper wrapper = new BufferingHttpResponseWrapper(response);
		
		assertThat(wrapper.getWriter(), notNullValue());
		wrapper.getOutputStream();
	}
	
	@Test
	public void writes_content_to_buffer() throws IOException {
		BufferingHttpResponseWrapper wrapper = new BufferingHttpResponseWrapper(response);
		wrapper.getOutputStream().write("Hello World".getBytes());
		
		verify(response, times(0)).getOutputStream();
		
		String result = new BufferedReader(new InputStreamReader(wrapper.getBuffer()))
		  .lines().collect(Collectors.joining("\n"));

		assertThat(result, equalTo("Hello World"));
	}
}
