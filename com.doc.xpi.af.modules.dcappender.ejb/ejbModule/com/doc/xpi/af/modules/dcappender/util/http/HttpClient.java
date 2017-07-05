package com.doc.xpi.af.modules.dcappender.util.http;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;


import com.doc.xpi.af.modules.dcappender.util.http.HttpClient;
import com.doc.xpi.af.modules.dcappender.util.http.HttpClientException;
import com.doc.xpi.af.modules.dcappender.util.http.HttpRequest;
import com.doc.xpi.af.modules.dcappender.util.http.HttpResponse;
import com.sap.tc.logging.Location;



public class HttpClient {
	
	private static final Location TRACE = Location
		.getLocation(HttpClient.class.getName());
	
	public static HttpResponse doRequest(HttpRequest request,
			Map<String, String> parameters) throws HttpClientException {

		String SIGNATURE = "doRequest(HttpRequest request, Map<String, String> parameters)";
		TRACE.entering(SIGNATURE, new Object[] { request,parameters });
		
		URL url;
		String response = "";
		int responseCode = -1;

		try {
			url = new URL(request.getURL());
		} catch (MalformedURLException e) {
			throw new HttpClientException("URL is not well formed"
					+ e.getMessage(), e);
		}

		Proxy proxy = request.getProxy();
		HttpURLConnection conn = null;
		try {
			conn = (proxy == null ? (HttpURLConnection) url.openConnection()
					: (HttpURLConnection) url.openConnection(proxy));
		} catch (IOException e) {
			throw new HttpClientException("Could not open connection to "
					+ request.getURL() + ". " + e.getMessage(), e);
		}
		conn.setReadTimeout(request.getReadTimeout());
		conn.setConnectTimeout(request.getConnectTimeout());
		try {
			conn.setRequestMethod(request.getMethodString());
		} catch (ProtocolException e) {
			throw new HttpClientException("HTTP Method not supported. "
					+ e.getMessage(), e);
		}

		for (Entry<String, String> pair : request.getHeaders().entrySet()) {
			conn.setRequestProperty(pair.getKey(), pair.getValue());
		}

		conn.setDoInput(true);
		if (request.hasOutput()) {
			conn.setDoOutput(true);
			try {
				OutputStream os = conn.getOutputStream();
				BufferedWriter writer = new BufferedWriter(
						new OutputStreamWriter(os, "UTF-8"));
				writer.write(request.getPostData());
				writer.flush();
				writer.close();
				os.close();
			} catch (IOException e) {
				throw new HttpClientException(
						"Could not write data to OutputStream. "
								+ e.getMessage(), e);
			}
		}

		try {
			responseCode = conn.getResponseCode();
		} catch (IOException e) {
			throw new HttpClientException("Could not read response code. "
					+ e.getMessage(), e);
		}

		if (responseCode == HttpURLConnection.HTTP_OK) {
			String line;
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(
						conn.getInputStream()));

				while ((line = br.readLine()) != null) {
					response += line;
				}
			} catch (IOException e) {
				throw new HttpClientException("Could not read InputStream. "
						+ e.getMessage(), e);
			}

		} else {
			String line;
			BufferedReader br = new BufferedReader(new InputStreamReader(conn
					.getErrorStream()));
			try {
				while ((line = br.readLine()) != null) {
					response += line;
				}
			} catch (IOException e) {
				throw new HttpClientException("Could not read ErrorStream. "
						+ e.getMessage(), e);
			}

		}

		HttpResponse httpResponse = new HttpResponse(responseCode, response);
		
		TRACE.exiting(SIGNATURE,httpResponse);
		return httpResponse;
	}
}