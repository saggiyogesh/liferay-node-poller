package com.poller.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.liferay.portal.kernel.json.JSONObject;

public class UsersPortletMapper {

	private static Map<String, Map<Long, JSONObject>> _map = new HashMap<String, Map<Long, JSONObject>>();

	public static void init(String portletId){
		if(!_map.containsKey(portletId)){
			
			_map.put(portletId, new ConcurrentHashMap<Long, JSONObject>());
		}
	}

	public static void put(String portletId, JSONObject json) {
		
		_map.get(portletId).put(json.getLong("userId"), json);
		
	}

	public static Collection<JSONObject> get(String portletId) {
		return _map.get(portletId).values(); 
	}

	public static JSONObject get(String portletId, long userId) {
		return _map.get(portletId).get(userId); 
	}
	
	public static int remove(String portletId, long userId) {
		Map<Long, JSONObject> values = _map.get(portletId);
		values.remove(userId);
		
		int size = values.size();
		if (size == 0) {
			_map.remove(portletId);
		}
		return size;
	}
}
