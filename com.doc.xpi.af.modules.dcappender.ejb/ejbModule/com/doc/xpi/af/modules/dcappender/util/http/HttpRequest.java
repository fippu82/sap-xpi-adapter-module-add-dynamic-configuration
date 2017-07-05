package com.doc.xpi.af.modules.dcappender.util.http;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;

import com.doc.xpi.af.modules.dcappender.util.http.HttpRequest;
import com.sap.tc.logging.Location;

public class HttpRequest {

	private static final String PARAMETER_REQUEST_URL = "http.request.url";
	private static final String PARAMETER_REQUEST_PROXYHOST = "http.request.proxyHost";
	private static final String PARAMETER_REQUEST_PROXYPORT = "http.request.proxyPort";
	private static final String PARAMETER_REQUEST_METHOD = "http.request.method";
	private static final String PARAMETER_REQUEST_HEADER = "http.request.header";
	private static final String PARAMETER_REQUEST_POSTDATA = "http.request.postdata";
	private static final String PARAMETER_REQUEST_CONNECTIONTIMEOUT = "http.request.connectiontimeout";
	private static final String PARAMETER_REQUEST_READTIMEOUT = "http.request.readtimeout";
	private static final Location TRACE = Location
	.getLocation(HttpRequest.class.getName());
	
    public static enum Method {
        GET,
        POST
    }

    private Method method = Method.POST;
    private String URL;
    private HashMap<String, String> headers;
    private String postData;
    private int connectionTimeout = 60000;
    private int readTimeout = 60000;
    private String proxyHost;
    private Integer proxyPort;


	public HttpRequest(Map<String, String> parameters) {
		headers = new HashMap<String, String>();
		headers.put("Content-Length", "0");
		
		for (Map.Entry<String, String> parameter : parameters.entrySet()) {
			if (parameter.getKey().equals(PARAMETER_REQUEST_METHOD)) {
				this.method = Method.valueOf(parameter.getValue());
				TRACE.debugT("ModuleParameter "+ parameter.getKey() +": "+parameter.getValue());
			} else if (parameter.getKey().equals(PARAMETER_REQUEST_URL)) {
				this.URL = parameter.getValue();
				TRACE.debugT("ModuleParameter "+ parameter.getKey() +": "+parameter.getValue());
			} else if (parameter.getKey().equals(PARAMETER_REQUEST_PROXYHOST)) {
				this.proxyHost = parameter.getValue();
				TRACE.debugT("ModuleParameter "+ parameter.getKey() +": "+parameter.getValue());
			} else if (parameter.getKey().equals(PARAMETER_REQUEST_PROXYPORT)) {
				this.proxyPort = Integer.valueOf(parameter.getValue());
				TRACE.debugT("ModuleParameter "+ parameter.getKey() +": "+parameter.getValue());
			} else if (parameter.getKey().equals(PARAMETER_REQUEST_POSTDATA)) {
				this.postData = parameter.getValue();
				TRACE.debugT("ModuleParameter "+ parameter.getKey() +": "+parameter.getValue());
				headers.put("Content-Length", String.valueOf(this.postData.getBytes().length));
			} else if (parameter.getKey().equals(PARAMETER_REQUEST_CONNECTIONTIMEOUT)) {
				connectionTimeout = Integer.parseInt(parameter.getValue());
				TRACE.debugT("ModuleParameter "+ parameter.getKey() +": "+parameter.getValue());
			} else if (parameter.getKey().equals(PARAMETER_REQUEST_READTIMEOUT)) {
				readTimeout = Integer.parseInt(parameter.getValue());
				TRACE.debugT("ModuleParameter "+ parameter.getKey() +": "+parameter.getValue());
			} else if (parameter.getKey().startsWith(PARAMETER_REQUEST_HEADER)) {
				String v = parameter.getValue();
				if(parameter.getValue().contains(":")){
					headers.put(v.substring(0,v.indexOf(":")).trim(),v.substring(v.indexOf(":")+1,v.length()).trim());
					TRACE.debugT("ModuleParameter "+ parameter.getKey() +": "+parameter.getValue());
				}
			} else {

			}
		}
	}

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public String getMethodString() {
        return method.toString();
    }

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public HashMap<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(HashMap<String, String> headers) {
        this.headers = headers;
    }

    public String getPostData() {
        return postData;
    }

    public void setPostData(String postData) {
        this.postData = postData;
    }

	public int getConnectTimeout() {
		return connectionTimeout;
	}

	public int getReadTimeout() {
		return readTimeout;
	}

	public String getProxyHost() {
		return proxyHost;
	}

	public Integer getProxyPort() {
		return proxyPort;
	}

	public Boolean hasOutput() {
		return (this.postData!=null 
				&& Method.POST.equals(this.method) );
	}

	public Proxy getProxy() {
		 if (this.proxyHost != null
					&& this.proxyPort != null) {
				 return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(
						this.proxyHost, this.proxyPort));

         }else{
        	 return null;
         }
	}
}