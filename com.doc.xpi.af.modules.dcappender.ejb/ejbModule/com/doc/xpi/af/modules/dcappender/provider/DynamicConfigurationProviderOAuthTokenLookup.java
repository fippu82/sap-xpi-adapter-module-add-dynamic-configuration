package com.doc.xpi.af.modules.dcappender.provider;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.doc.xpi.af.modules.dcappender.util.AuditLogHelper;
import com.doc.xpi.af.modules.dcappender.util.DynamicConfigurationAttribute;
import com.doc.xpi.af.modules.dcappender.util.DynamicConfigurationProvider;
import com.doc.xpi.af.modules.dcappender.util.DynamicConfigurationProviderException;
import com.doc.xpi.af.modules.dcappender.util.KeyValueStore;
import com.doc.xpi.af.modules.dcappender.util.KeyValueStoreException;
import com.doc.xpi.af.modules.dcappender.util.http.HttpClient;
import com.doc.xpi.af.modules.dcappender.util.http.HttpClientException;
import com.doc.xpi.af.modules.dcappender.util.http.HttpRequest;
import com.doc.xpi.af.modules.dcappender.util.http.HttpResponse;
import com.sap.engine.interfaces.messaging.api.Message;
import com.sap.engine.interfaces.messaging.api.MessageKey;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditLogStatus;
import com.sap.tc.logging.Location;

public class DynamicConfigurationProviderOAuthTokenLookup implements DynamicConfigurationProvider {

	// oauth params
	private static final String PARAMETER_OA_USERNAME = "oauth.username";
	private static final String PARAMETER_OA_PASSWORD = "oauth.password";
	private static final String PARAMETER_OA_CLIENT_ID = "oauth.clientid";
	private static final String PARAMETER_OA_CLIENT_SECRET = "oauth.clientsecret";

	// dynamic configuration params
	private static final String PARAMETER_DC_ATTR_NAME = "attribute.name";
	private static final String PARAMETER_DC_ATTR_NAMESPACE = "attribute.namespace";

	// key value store params
	private static final String PARAMETER_KVS_ENABLED = "keyValueStore.enabled";
	private static final String PARAMETER_KVS_KEY = "keyValueStore.key";
	private static final String PARAMETER_KVS_EXPIRATIONTIME = "keyValueStore.expirationTime";
	private static final String PARAMETER_KVS_CLEAR = "keyValueStore.clear";
	private static final String PARAMETER_KVS_SERVER_NODE_SPECIFIC = "keyValueStore.serverNodeSpecific";

	private static final Location TRACE = Location
			.getLocation(DynamicConfigurationProviderOAuthTokenLookup.class.getName());

	private int tries = 0; 
	
	@Override
	public List<DynamicConfigurationAttribute> execute(Message message, Map<String, String> parameters)
			throws DynamicConfigurationProviderException {

		String SIGNATURE = "execute(Message message, Map<String, String> parameters)";
		TRACE.entering(SIGNATURE, new Object[] { message, parameters });

		MessageKey messageKey = message.getMessageKey();

		AuditLogHelper audit = new AuditLogHelper(messageKey);
		List<DynamicConfigurationAttribute> dcAttributes = new ArrayList<DynamicConfigurationAttribute>();
		DynamicConfigurationAttribute dcAttribute = null;

		String parameterNamespace = "http://sap.com/xi/XI/System/REST";
		String parameterName = "token";
		String parameterValue = null;
		Boolean storageEnabled = true;
		Boolean storageClear = false;
		String storageKey = parameters.get("ModuleContext.ChannelID");
		Boolean serverNodeSpecific = false;
		int storageExpirationTime = 3600000; // 1h

		String username = "";
		String password = "";
		String clientId = "";
		String clientSecret = "";

		for (Map.Entry<String, String> parameter : parameters.entrySet()) {
			if (parameter.getKey().equals(PARAMETER_DC_ATTR_NAMESPACE)) {
				parameterNamespace = parameter.getValue();
				TRACE.debugT("ModuleParameter " + parameter.getKey() + " = " + parameter.getValue());
			} else if (parameter.getKey().equals(PARAMETER_DC_ATTR_NAME)) {
				parameterName = parameter.getValue();
				TRACE.debugT("ModuleParameter " + parameter.getKey() + " = " + parameter.getValue());
			} else if (parameter.getKey().equals(PARAMETER_KVS_ENABLED)) {
				storageEnabled = Boolean.valueOf(parameter.getValue());
				TRACE.debugT("ModuleParameter " + parameter.getKey() + " = " + parameter.getValue());
			} else if (parameter.getKey().equals(PARAMETER_KVS_CLEAR)) {
				storageClear = Boolean.valueOf(parameter.getValue());
				TRACE.debugT("ModuleParameter " + parameter.getKey() + " = " + parameter.getValue());
			} else if (parameter.getKey().equals(PARAMETER_KVS_KEY)) {
				storageKey = parameter.getValue();
				TRACE.debugT("ModuleParameter " + parameter.getKey() + " = " + parameter.getValue());
			} else if (parameter.getKey().equals(PARAMETER_KVS_EXPIRATIONTIME)) {
				storageExpirationTime = Integer.valueOf(parameter.getValue());
				TRACE.debugT("ModuleParameter " + parameter.getKey() + " = " + parameter.getValue());
			} else if (parameter.getKey().equals(PARAMETER_KVS_SERVER_NODE_SPECIFIC)) {
				serverNodeSpecific = Boolean.valueOf(parameter.getValue());
				TRACE.debugT("ModuleParameter " + parameter.getKey() + " = " + parameter.getValue());
			} else if (parameter.getKey().equals(PARAMETER_OA_USERNAME)) {
				username = encodeValue(parameter.getValue());
				TRACE.debugT("ModuleParameter " + parameter.getKey() + " = " + encodeValue(parameter.getValue()));
			} else if (parameter.getKey().equals(PARAMETER_OA_PASSWORD)) {
				password = encodeValue(parameter.getValue());
				TRACE.debugT("ModuleParameter " + parameter.getKey() + " = " + encodeValue(parameter.getValue()));
			} else if (parameter.getKey().equals(PARAMETER_OA_CLIENT_ID)) {
				clientId = encodeValue(parameter.getValue());
				TRACE.debugT("ModuleParameter " + parameter.getKey() + " = " + encodeValue(parameter.getValue()));
			} else if (parameter.getKey().equals(PARAMETER_OA_CLIENT_SECRET)) {
				clientSecret = encodeValue(parameter.getValue());
				TRACE.debugT("ModuleParameter " + parameter.getKey() + " = " + encodeValue(parameter.getValue()));
			} else {
				// Reserved for future use
			}
		}

		KeyValueStore keyValueStore = new KeyValueStore(serverNodeSpecific);

		int maxTries = 3;
		int count = 0;
		Boolean success = false;
		while (!success && maxTries > count) { // retry loop
			success = true;

			if (storageEnabled && storageKey != null) {
				if (storageClear) {
					keyValueStore.remove(storageKey);
					audit.addAuditLogEntry(AuditLogStatus.SUCCESS, "Value removed from KeyValueStore");
				} else {
					parameterValue = keyValueStore.get(storageKey);
					if (parameterValue != null)
						audit.addAuditLogEntry(AuditLogStatus.SUCCESS, "Value found in KeyValueStore");
				}
			}

			if (parameterValue == null) {

				try {

					HttpRequest request = new HttpRequest(parameters);

					String postData = "grant_type=password&username=" + username + "&password=" + password
							+ "&scope=" + "&client_id=" + clientId + "&client_secret=" + clientSecret + "";
					request.setPostData(postData);
					
					/* Retry once if response didn't yield a token. After two tries, throw exception if still failed. 
					   This was added due to observed issues during the first try.   */
					while (parameterValue == null && tries <= 1)
					{
						HttpResponse response = HttpClient.doRequest(request, parameters);
						try {
							tries++;
							parameterValue = response.getResponse(parameters);							
						}
						catch (HttpClientException ex) {
							if (tries > 1 ) throw new HttpClientException(ex.getMessage() + " " + " - Response was: " + response.getResponse() );													
						}						
					}
					
					audit.addAuditLogEntry(AuditLogStatus.SUCCESS, "New value received from HttpResponse.");

					if (storageEnabled && !storageClear && storageKey != null && parameterValue != null) {
						try {
							keyValueStore.add(storageKey, parameterValue, storageExpirationTime);
							audit.addAuditLogEntry(AuditLogStatus.SUCCESS, "Entry added to KeyValueStore.");
						} catch (KeyValueStoreException e) {
							audit.addAuditLogEntry(AuditLogStatus.ERROR, e.getMessage());
							if (++count >= maxTries) {
								throw new DynamicConfigurationProviderException(e.getMessage(), e);
							}
							success = false;
						}
					}
				} catch (HttpClientException e) {
					audit.addAuditLogEntry(AuditLogStatus.ERROR, e.getMessage());
					throw new DynamicConfigurationProviderException(e.getMessage());
				}
			}

		} // end of try loop


		
		if (parameterNamespace != null && !parameterNamespace.isEmpty() && parameterName != null
				&& !parameterName.isEmpty() && parameterValue != null && !parameterValue.isEmpty()) {
			
			audit.addAuditLogEntry(AuditLogStatus.SUCCESS, "Saving token to ASMA parameter: " + parameterNamespace + " - " + parameterName);

			dcAttribute = new DynamicConfigurationAttribute(parameterNamespace, parameterName, parameterValue);
			if (dcAttribute.isDynamicConfigurationAttributeComplete()) {
				dcAttributes.add(dcAttribute);
			} else {
				throw new DynamicConfigurationProviderException(
						String.format("Invalid configuration, check attribute %s an %s", PARAMETER_DC_ATTR_NAME,
								PARAMETER_DC_ATTR_NAMESPACE));
			}

		}
		else
		{
			audit.addAuditLogEntry(AuditLogStatus.WARNING, "ASMA parameters are empty, please configure adapter module.");
		}
		TRACE.exiting(SIGNATURE);
		return dcAttributes;
	}
	
    private static String encodeValue(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex.getCause());
        }    
    }
}
