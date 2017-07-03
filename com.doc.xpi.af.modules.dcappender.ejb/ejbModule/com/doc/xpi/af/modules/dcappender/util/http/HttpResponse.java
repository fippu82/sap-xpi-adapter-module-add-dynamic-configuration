package com.doc.xpi.af.modules.dcappender.util.http;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.endress.xpi.af.modules.dcappender.util.http.HttpClientException;
import com.endress.xpi.af.modules.dcappender.util.http.HttpResponse;
import com.endress.xpi.af.modules.dcappender.util.http.NamespaceResolver;
import com.sap.tc.logging.Location;

public class HttpResponse {

	private static final String PARAMETER_RESPONSE_STRINGFORMAT = "http.response.stringformat";
	private static final String PARAMETER_RESPONSE_SOURCE = "http.response.valuesource";
	private static final String PARAMETER_RESPONSE_SOURCE_XPATH = "http.response.valuesource.xpath";
	private static final String PARAMETER_RESPONSE_SOURCE_REGEX = "http.response.valuesource.regex";
	private static final Location TRACE = Location
			.getLocation(HttpResponse.class.getName());
	
	private int responseCode;
	private String response;

	public enum ValueSource {
		Default, Regex, XPath, JSONXpath,
	}

	public HttpResponse(int responseCode, String response) {
		this.responseCode = responseCode;
		this.response = response;
		TRACE.debugT(
				String.format("HttpResponse %nResponseCode: %s%nResponse:%n%s",
						this.responseCode,this.response));
	}

	public HttpResponse() {

	}

	public int getResponseCode() {
		return responseCode;
	}

	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}

	public String getResponse() {
		return response;
	}

	public String getResponse(Map<String, String> parameters)
			throws HttpClientException {
		
		String SIGNATURE = "getResponse(Map<String, String> parameters)";
		TRACE.entering(SIGNATURE, new Object[] { parameters });
		
		ValueSource valueSource = ValueSource.Default;
		String regex = null;
		String xPath = null;
		String stringFormat = "%s";

		for (Map.Entry<String, String> parameter : parameters.entrySet()) {
			if (parameter.getKey().equals(PARAMETER_RESPONSE_SOURCE)) {
				valueSource = ValueSource.valueOf(parameter.getValue());
			} else if (parameter.getKey().equals(PARAMETER_RESPONSE_SOURCE_REGEX)) {
				regex = parameter.getValue();
				TRACE.debugT("ModuleParameter "+ parameter.getKey() +": "+parameter.getValue());
			} else if (parameter.getKey().equals(PARAMETER_RESPONSE_SOURCE_XPATH)) {
				xPath = parameter.getValue();
				TRACE.debugT("ModuleParameter "+ parameter.getKey() +": "+parameter.getValue());
			} else if (parameter.getKey().equals(PARAMETER_RESPONSE_STRINGFORMAT)) {
				stringFormat = parameter.getValue();
				TRACE.debugT("ModuleParameter "+ parameter.getKey() +": "+parameter.getValue());
			}

		}

		String s = "";
		if (ValueSource.Regex.equals(valueSource)) {
			try {
				s = doRegex(response, regex);
			} catch (PatternSyntaxException e) {
				throw new HttpClientException("Regex Sytax Error" + e.getMessage());
			}
		} else if (ValueSource.XPath.equals(valueSource)) {
			
				try {
					s = doXPath(response, xPath);
				} catch (XPathExpressionException e) {
					throw new HttpClientException("XPathExpressionException:" + e.getMessage());
				} catch (ParserConfigurationException e) {
					throw new HttpClientException("ParserConfigurationException:" + e.getMessage());
				} catch (SAXException e) {
					throw new HttpClientException("SAXException:" + e.getMessage());
				} catch (IOException e) {
					throw new HttpClientException("IOException:" + e.getMessage());
				}
			
		} else if (ValueSource.JSONXpath.equals(valueSource)) {
			try {
				s = doJSONXPath(response, xPath);
			} catch (Exception e) {
				throw new HttpClientException(e.getMessage());
			}		
		} else {
			s = this.response;
		}
		String formattedResponse = String.format(stringFormat, s);
		TRACE.exiting(SIGNATURE, formattedResponse);
		return formattedResponse;
		
		
	}

	private String doJSONXPath(String inputString, String xPath) {
		String SIGNATURE = "doJSONXPath(String input, String xPath)";
		TRACE.entering(SIGNATURE, new Object[] { inputString, xPath });
		throw new UnsupportedOperationException("JSONXpath is not yet implemened");
	}

	private String doXPath(String inputString, String xPath) throws ParserConfigurationException,
			SAXException, IOException, XPathExpressionException, HttpClientException {
		
		String SIGNATURE = "doXPath(String inputString, String xPath)";
		TRACE.entering(SIGNATURE, new Object[] { inputString, xPath });
		
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder;
		Document doc = null;
		XPathExpression expr = null;
		builder = factory.newDocumentBuilder();
		InputSource is = new InputSource(new StringReader(inputString));
		doc = builder.parse(is);
		// create an XPathFactory
		XPathFactory xFactory = XPathFactory.newInstance();

		// create an XPath object
		XPath xpath = xFactory.newXPath();

		xpath.setNamespaceContext(new NamespaceResolver(doc));
		// compile the XPath expression
		expr = xpath.compile(xPath);

		// run the query and get a nodeset
		Object result = expr.evaluate(doc, XPathConstants.NODESET);
		NodeList nodes = (NodeList) result;
		// cast the result to a DOM NodeList
		
		String value;
		if (nodes.getLength() > 0) {
			value = nodes.item(0).getTextContent();
		} else {
			throw new HttpClientException("XPath expression did not yield any result.");
		}
		
		TRACE.exiting(SIGNATURE,value);
		return value;

	}

	private String doRegex(String inputString, String regex)
			throws PatternSyntaxException, HttpClientException {
		String SIGNATURE = "doRegex(String inputString, String regex)";
		TRACE.entering(SIGNATURE, new Object[] { inputString, regex });
		
		Pattern p = Pattern.compile(regex);
		Matcher matcher = p.matcher(response);
		String value;
		if (matcher.find()) {
			value = matcher.group();
		} else {
			throw new HttpClientException("Regex did not yield any result.");
		}
		
		TRACE.exiting(SIGNATURE,value);
		return value;
	}

	public void setResponse(String response) {
		this.response = response;
	}

}
