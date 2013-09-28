package com.poller.response;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.poller.DefaultPollerResponse;
import com.liferay.portal.kernel.poller.PollerHeader;
import com.liferay.portal.kernel.poller.PollerResponseClosedException;
import com.liferay.portal.kernel.util.Validator;

public class NewPollerResponse extends DefaultPollerResponse{

	public NewPollerResponse(PollerHeader pollerHeader, String portletId,
			String chunkId) {
		super(pollerHeader, portletId, chunkId);
	}
	
	public  void setParameter(String name, JSONArray value)
			throws PollerResponseClosedException {

			_parameterMap.put(name, value);
		}

		public  void setParameter(String name, JSONObject value)
			throws PollerResponseClosedException {


			_parameterMap.put(name, value);
		}

		public void setParameter(String name, String value)
			throws PollerResponseClosedException {
			_parameterMap.put(name, value);
		}
		
		public JSONObject toJSONObject() {
			JSONObject pollerResponseJSONObject =
				JSONFactoryUtil.createJSONObject();

			pollerResponseJSONObject.put("portletId", getPortletId());

			if (Validator.isNotNull(_chunkId)) {
				pollerResponseJSONObject.put("chunkId", _chunkId);
			}

			JSONObject dataJSONObject = JSONFactoryUtil.createJSONObject();

			Iterator<Map.Entry<String, Object>> itr =
				_parameterMap.entrySet().iterator();

			while (itr.hasNext()) {
				Map.Entry<String, Object> entry = itr.next();

				String name = entry.getKey();
				Object value = entry.getValue();

				if (value instanceof JSONArray) {
					dataJSONObject.put(name, (JSONArray)value);
				}
				else if (value instanceof JSONObject) {
					dataJSONObject.put(name, (JSONObject)value);
				}
				else {
					dataJSONObject.put(name, String.valueOf(value));
				}
			}

			pollerResponseJSONObject.put("data", dataJSONObject);

			return pollerResponseJSONObject;
		}
		
		public boolean isEmpty() {
			return _parameterMap.isEmpty();
		}
		
		private String _chunkId;
		private Map<String, Object> _parameterMap = new HashMap<String, Object>();
		private String _portletId;
	
}
