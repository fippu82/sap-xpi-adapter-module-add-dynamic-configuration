package com.doc.xpi.af.modules.dcappender.util.http;

public class HttpClientException extends Exception{

	private static final long serialVersionUID = 1L;

	public HttpClientException(String message) {
		super(message);
	}

	public HttpClientException(String message,
			Throwable cause) {
		super(message, cause);
	}
}
