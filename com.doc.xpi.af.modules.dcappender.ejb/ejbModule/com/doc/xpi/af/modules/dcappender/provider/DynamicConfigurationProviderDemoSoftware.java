package com.doc.xpi.af.modules.dcappender.provider;

import java.util.List;
import java.util.Map;

import com.doc.xpi.af.modules.dcappender.util.DynamicConfigurationAttribute;
import com.doc.xpi.af.modules.dcappender.util.DynamicConfigurationProviderException;
import com.sap.engine.interfaces.messaging.api.Message;

public class DynamicConfigurationProviderDemoSoftware extends
		DynamicConfigurationProviderHttpLookup {

	private static final String PARAMETER_USERNAME = "DemoSoftware.username";
	private static final String PARAMETER_PASSWORD = "DemoSoftware.password";
	private static final String PARAMETER_URL = "DemoSoftware.url";

	@Override
	public List<DynamicConfigurationAttribute> execute(Message message,
			Map<String, String> parameters)
			throws DynamicConfigurationProviderException {

		String username = "";
		String password = "";
		String url = "";
		for (Map.Entry<String, String> parameter : parameters.entrySet()) {
			if (parameter.getKey().equals(PARAMETER_URL)) {
				url = parameter.getValue();
			} else if (parameter.getKey().equals(PARAMETER_USERNAME)) {
				username = parameter.getValue();
			} else if (parameter.getKey().equals(PARAMETER_PASSWORD)) {
				password = parameter.getValue();
			} else {

			}
		}
		String postdata = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
				+ "<soapenv:Header/>" + "<soapenv:Body>" + "<ns:Login>"
				+ "<ns:userName>" + username + "</ns:userName>"
				+ "<ns:password>" + password + "</ns:password>" + "</ns:Login>"
				+ "</soapenv:Body>" + "</soapenv:Envelope>";

		// Set default values if not already set by Module Parameter
		if (!parameters.containsKey("http.request.url"))
			parameters.put("http.request.url", url);

		if (!parameters.containsKey("http.request.postdata"))
			parameters.put("http.request.postdata", postdata);

		if (!parameters.containsKey("http.request.header.soapAction"))
			parameters.put("http.request.header.soapAction",
					"SOAPAction: \"Login\"");

		if (!parameters.containsKey("http.response.valuesource"))
			parameters.put("http.response.valuesource", "Regex");

		if (!parameters.containsKey("http.response.valuesource.regex"))
			parameters.put("http.response.valuesource.regex",
					"(?<=<LoginResult>).*(?=</LoginResult>)");

		if (!parameters.containsKey("keyValueStore.enabled"))
			parameters.put("keyValueStore.enabled", "true");

		if (!parameters.containsKey("keyValueStore.key"))
			parameters.put("keyValueStore.key", "DemoSoftware");

		if (!parameters.containsKey("keyValueStore.expirationTime"))
			parameters.put("keyValueStore.expirationTime", "10");

		return super.execute(message, parameters);
	}
}
