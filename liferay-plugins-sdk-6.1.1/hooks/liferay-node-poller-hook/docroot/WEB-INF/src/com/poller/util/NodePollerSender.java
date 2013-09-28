package com.poller.util;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.http.client.fluent.Async;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.concurrent.FutureCallback;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

public class NodePollerSender {
	
	private static String _nodePollerURL = PollerUtil.getNodePollerURL();
	
	private static ExecutorService _threadpool = Executors.newFixedThreadPool(2);
	private static Async async = Async.newInstance().use(_threadpool);
	
	
	private static NodePollerSender _instance = new NodePollerSender();
	
	private static List<RequestParams> _requestParams = new ArrayList<RequestParams>();
	
	public static void push(String portletId, long userId, JSONObject data){
		RequestParams params = _instance.new RequestParams(portletId, userId, data);
		_requestParams.add(params);		
	}
	
	public static void send() throws URISyntaxException {
		URIBuilder builder = new URIBuilder(_nodePollerURL);
		JSONObject receiveJSON = JSONFactoryUtil.createJSONObject();
		int i = 0;
		for(RequestParams params: _requestParams){
			JSONObject object = JSONFactoryUtil.createJSONObject();
			object.put("portletId", params.getPortletId()).put("data", params.getData().toString())
			.put("userIds", String.valueOf(params.getUserId()));
//				builder.addParameter("portletId", params.getPortletId()).addParameter("data", params.getData().toString()).
//				addParameter("userIds", String.valueOf(params.getUserId()));
			receiveJSON.put(i+"", object);		
			i++;
		}
		_requestParams.clear();
		builder.addParameter("receive", receiveJSON.toString());
		
		final Request request = Request.Get(builder.build());
		
		Future<Content> future = async.execute(request, new FutureCallback<Content>() {
		    public void failed (final Exception e) {
		    	_log.error(e.getMessage() +": "+ request);
		    	PollerProcessorUtil.destroy();
		    }
		    public void completed (final Content content) {
		    	_log.info("Request completed: "+ request);
//		    	_log.info("Response: "+ content.asString());
		    }

		    public void cancelled () {}
		});
		
	}
	
	private class RequestParams{
		private String portletId;
		private long userId;
		private JSONObject data;
		
		public RequestParams(String portletId, long userId, JSONObject data) {
			this.portletId = portletId;
			this.userId = userId;
			this.data = data;
		}

		public String getPortletId() {
			return portletId;
		}

		public long getUserId() {
			return userId;
		}

		public JSONObject getData() {
			return data;
		}
		
	}
	
	public static void destroy() {
		_threadpool.shutdown();
	}
	
	private static Log _log = LogFactoryUtil.getLog(NodePollerSender.class);
}
