# SAP PI/PO: Adapter module AddDynamicConfigurationBean (Fork)
Custom adapter module by Martin Buselmeier for Adapter Engine of SAP PI/PO systems to set dynamic configuration attributes of the processed message.
Extended by Philippe Addor by a provider class for OAuth Token Lookup to use for example with Salesforce. 

Added oAuth provider class (removed the others not needed for this purpose): https://github.com/fippu82/sap-xpi-adapter-module-add-dynamic-configuration/blob/master/com.doc.xpi.af.modules.dcappender.ejb/ejbModule/com/doc/xpi/af/modules/dcappender/provider/DynamicConfigurationProviderOAuthTokenLookup.java

Modified provider factory to use the above class: https://github.com/fippu82/sap-xpi-adapter-module-add-dynamic-configuration/blob/master/com.doc.xpi.af.modules.dcappender.ejb/ejbModule/com/doc/xpi/af/modules/dcappender/util/DynamicConfigurationProviderFactory.java

Forked from Martin Buselmeier's repository (https://github.com/MartinBuselmeier/sap-xpi-adapter-module-add-dynamic-configuration), based on his great blog post here: https://blogs.sap.com/2017/07/05/api-token-via-http-lookup-in-adapter-module/
