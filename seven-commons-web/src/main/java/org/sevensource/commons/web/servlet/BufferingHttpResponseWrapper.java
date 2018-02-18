package org.sevensource.commons.web.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.sevensource.commons.web.util.FastByteArrayOutputStream;


public class BufferingHttpResponseWrapper extends HttpServletResponseWrapper {

	private static final int INITIAL_BUFFER_SIZE = 1024;

	private final FastByteArrayOutputStream buffer;
	
	private ServletOutputStream servletOutputStream;
	private PrintWriter writer;

	public BufferingHttpResponseWrapper(HttpServletResponse response) {
		super(response);
		buffer = new FastByteArrayOutputStream(INITIAL_BUFFER_SIZE);
	}

	@Override
	public ServletOutputStream getOutputStream() {

		if (this.writer != null) {
			throw new IllegalStateException("getWriter() has already been called on this response.");
		}

		if (this.servletOutputStream == null) {
			this.servletOutputStream = new ServletOutputStream() {

				@Override
				public void write(int b) throws IOException {
					buffer.write(b);
				}

				@Override
				public void flush() throws IOException {
					buffer.flush();
				}

				@Override
				public void close() throws IOException {
					buffer.close();
				}

				@Override
				public boolean isReady() {
					return true;
				}

				@Override
				public void setWriteListener(WriteListener listener) {
					// no-op - we should never have a reason to call any of the
					// listeners methods
				}
			};
		}

		return servletOutputStream;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		if (this.servletOutputStream != null) {
			throw new IllegalStateException("getOutputStream() has already been called on this response.");
		}

		if (this.writer == null) {
			this.writer = new PrintWriter(new OutputStreamWriter(buffer, getCharacterEncoding()));
		}

		return writer;
	}

	@Override
	public void flushBuffer() throws IOException {
		if (writer != null) {
			writer.flush();
		} else if (servletOutputStream != null) {
			servletOutputStream.flush();
		}
	}

	@Override
	public int getBufferSize() {
		return this.buffer.size();
	}

	@Override
	public void reset() {
		super.reset();
		buffer.reset();
	}

	@Override
	public void resetBuffer() {
		super.resetBuffer();
		buffer.reset();
	}
	
	public void writeBufferTo(OutputStream os) throws IOException {
		close();
		buffer.writeTo(os);
	}
	
	public InputStream getBuffer() {
		close();
		return buffer.getInputStream();
	}
	
	private void close() {
		if (writer != null) {
			writer.close();
		} else if (servletOutputStream != null) {
			try {
				servletOutputStream.close();
			} catch(IOException e) {
			// no-op - this should never happen
			}
		}
	}
}