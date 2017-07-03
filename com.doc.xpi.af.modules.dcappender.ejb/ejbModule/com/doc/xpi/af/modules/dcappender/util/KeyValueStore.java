package com.doc.xpi.af.modules.dcappender.util;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.sap.aii.af.lib.util.UtilException;
import com.sap.aii.af.service.idmap.MessageIDMapper;
import com.sap.tc.logging.Location;

public class KeyValueStore {
	
	static final String UNIQUE_KEY = "KeyValueStore";
	static final String UNIQUE_KEY_LAST_CLEAR = UNIQUE_KEY+".LastClear";
	static final Integer MAX_TOKEN_IDENTIFIER_LENGHT = 40;
	static final Integer MAX_TOKEN_PART_LENGHT = 100;
	static final String ID_SUFFIX_FORMAT = "§Part:%05d";
	static final String END_OF_TOKEN = "=#EOK#";
	private static final Location TRACE = Location
	.getLocation(KeyValueStore.class.getName());

	Boolean ServerNodeSpecific;
	/**
	 * Default constructor.
	 */
	public KeyValueStore(){
		this(false);
	}
	
	public KeyValueStore(Boolean serverNodeSpecific) {
		String SIGNATURE = "KeyValueStore(Boolean serverNodeSpecific)";
		TRACE.entering(SIGNATURE, new Object[] { serverNodeSpecific });
		
		this.ServerNodeSpecific =  serverNodeSpecific;		
		try{
		MessageIDMapper mapper = MessageIDMapper.getInstance();
		String lastClear =	mapper.getMappedId(UNIQUE_KEY_LAST_CLEAR);
		
		Boolean doClear = false;
		if(lastClear== null){
			doClear = true;
			mapper.createIDMap(UNIQUE_KEY_LAST_CLEAR,
					String.format("%s", (System.currentTimeMillis()+60*1000)),
					System.currentTimeMillis()+2592000000L,  //  entry lasts for 30days...
					false);
		}else{
			long lc = Long.parseLong(lastClear);
			if(lc<(System.currentTimeMillis()-60*1000)){
				// only do a clear max every minute...
				doClear = true;
				mapper.updateIDMap(UNIQUE_KEY_LAST_CLEAR, 
						String.format("%s", (System.currentTimeMillis()+60*1000)),
						System.currentTimeMillis()+2592000000L,  // entry lasts for 30days...
						false);
			}
		}
	
		if(doClear){
			mapper.removeExpiredIDMaps();
			TRACE.infoT("Epired KeyValueStore entries have been removed.");
		}
		}catch (Exception ex){
			TRACE.errorT("Could not remove old KeyValueStore entries. "+ex.getMessage());
		}
		TRACE.exiting(SIGNATURE);
	
	}


	
	/***
	 * 
	 * @param Key
	 * @return Value

	 * @throws ModuleException
	 */
	public String get(String key) {
		String SIGNATURE = "get(String key)";
		TRACE.entering(SIGNATURE, new Object[] { key });
		
			MessageIDMapper mapper = MessageIDMapper.getInstance();
			String mid = getMessageID(key);

			List<Map.Entry<String, String>> resultList = getEntries(mid,
					mapper, 0);
			TRACE.infoT(resultList.size() +" entries found.");
			String internalValue = "";
			for (Map.Entry<String, String> tokenStoreEntry : resultList) {
				TRACE.
					debugT("reading " 
							+"Key: "
							+tokenStoreEntry.getKey()
							+"ValuePart"
							+tokenStoreEntry.getValue());
				
				internalValue += tokenStoreEntry.getValue();
			}

			String value = null;
			// if a rawToken found, remove the End_Of_Token identifier....
			if (internalValue != null && internalValue.contains(END_OF_TOKEN)) {
				value = internalValue.substring(0, internalValue.length()
						- END_OF_TOKEN.length());
			}
			TRACE.exiting(SIGNATURE,value);
			return value;
	}

	/***
	 * 
	 * @param Key
	 * @param Value
	 * @param ExpirationTime
	 *            in seconds
	 * @throws KeyValueStoreException 
	 * @throws ModuleException
	 * @throws KeyValueStoreException
	 */
	public void add(String key, String value, int expirationTime) 
			throws KeyValueStoreException {
		String SIGNATURE = "add(String key, String value, int expirationTime)";
		TRACE.entering(SIGNATURE, new Object[] { key, value, expirationTime });

		MessageIDMapper mapper = MessageIDMapper.getInstance();
		String mid = getMessageID(key);
	
			long expirationDate = expirationTime * 1000 *60
					+ System.currentTimeMillis();
			
			String[] parts = splitByNumber(value + END_OF_TOKEN,
					MAX_TOKEN_PART_LENGHT);
			int i = 0;

			for (String valuePart : parts) {

				String keyPart = mid + getSuffix(i);
				i++;
				TRACE.
				debugT(SIGNATURE,"adding " 
						+ "Key: "
						+ keyPart
						+ "ValuePart: "
						+ valuePart);
				try{
				mapper.createIDMap(keyPart, valuePart, expirationDate,false);

				}catch (UtilException e) {
					TRACE.catching(SIGNATURE, e);
					KeyValueStoreException kvsex = new KeyValueStoreException("Could not add entry to KeyValueStore ("+e.getClass()+"): "+e.getMessage(), e);
					TRACE.throwing(SIGNATURE, kvsex);
					throw kvsex;
				}
			}
			TRACE.exiting(SIGNATURE);
	}
	
	

	/***
	 * delete the KeyValuePair stored in the Database
	 * 
	 * @throws KeyValueStoreException
	 */
	public void remove(String key) {
		String SIGNATURE = "remove(String key)";
		TRACE.entering(SIGNATURE, new Object[] { key });

		
			String mid = getMessageID(key);

			MessageIDMapper mapper = MessageIDMapper.getInstance();

			List<Map.Entry<String, String>> entriesToDelete = getEntries(mid,
					mapper, 0);

			for (Map.Entry<String, String> entry : entriesToDelete) {
				TRACE.
				debugT(SIGNATURE,"removing " 
						+ "Key: "
						+ entry.getKey()
						+ "ValuePart: "
						+ entry.getValue());
				mapper.remove(entry.getKey(),false);
			}
			TRACE.exiting(SIGNATURE);
	}

	private String getMessageID(String key) {
		String SIGNATURE = "getMessageID(String key)";
		TRACE.entering(SIGNATURE, new Object[] { key });
		
		String mid;
		if(ServerNodeSpecific){
			mid =  UNIQUE_KEY + "." + getServerNodeID() + "." + key;
		}
		else{
			mid = UNIQUE_KEY + "." + key;	
		}
		TRACE.exiting(SIGNATURE,mid);
		return mid;
		
	}

	private String getSuffix(int i) {
		String SIGNATURE = "getSuffix(int i)";
		TRACE.entering(SIGNATURE, new Object[] { i });
		
		String suffix;
		
		if (i == 0) {
			suffix = "";
		} else {

			suffix = String.format(ID_SUFFIX_FORMAT, i);
		}
		TRACE.exiting(SIGNATURE,suffix);
		return suffix;

	}

	/**
	 * returns the cluster ID of the Current Cluster the Module is executed
	 * right now
	 * 
	 * @return unique ClusterID
	 * @throws ModuleException
	 */
	private String getServerNodeID() {
		String SIGNATURE = " getServerNodeID()";
		TRACE.entering(SIGNATURE);
		
		
		String ServerNodeID = "N/A";
		
		TRACE.exiting(SIGNATURE,ServerNodeID);
		
		return ServerNodeID;
	}

	/***
	 * Splits a string into fixed length parts
	 * 
	 * @param s
	 * @param size
	 * @return
	 */
	private String[] splitByNumber(String s, int size) {
		
		String SIGNATURE = "splitByNumber(String s, int size)";
		TRACE.entering(SIGNATURE, new Object[] { s,size });
		if (s == null || size <= 0)
			return null;
		int chunks = s.length() / size + ((s.length() % size > 0) ? 1 : 0);
		String[] arr = new String[chunks];
		for (int i = 0, j = 0, l = s.length(); i < l; i += size, j++)
			arr[j] = s.substring(i, Math.min(l, i + size));
		
		TRACE.exiting(SIGNATURE,arr);
		return arr;
	}

	/*
	 * Internal function to read the KeyValuePair from the MessageIDMapper, if
	 * an entry does not contain an End_of_Key identifier, search recursively
	 * for the next part.
	 * ---> switch to LInkedLlist
	 */
	private List<Map.Entry<String, String>> getEntries(String mid,
			MessageIDMapper mapper, Integer part) {
	
		String SIGNATURE = "getEntries(String mid, MessageIDMapper mapper, Integer part)";
		TRACE.entering(SIGNATURE, new Object[] { mid, mapper, part });
		
		String internalMsgID = mid + getSuffix(part);
		String value = mapper.getMappedId(internalMsgID);

		//com.sap.aii.af.service.idmap.IDMap	map = mapper.getIDMapExt(internalMsgID);
		
		ArrayList<Map.Entry<String, String>> result = new ArrayList<Map.Entry<String, String>>();

		if (value != null) {
			result.add(new AbstractMap.SimpleEntry<String, String>(
					internalMsgID, value));
			if (!value.endsWith(END_OF_TOKEN)) {
				result.addAll(getEntries(internalMsgID, mapper, part + 1));
			}
		}
		TRACE.exiting(SIGNATURE, result);
		return result;
	}


	
	
}