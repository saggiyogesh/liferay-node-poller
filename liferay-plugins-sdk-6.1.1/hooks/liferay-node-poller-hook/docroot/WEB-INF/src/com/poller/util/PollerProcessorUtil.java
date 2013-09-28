
package com.poller.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.poller.PollerHeader;
import com.liferay.portal.kernel.poller.PollerProcessor;
import com.liferay.portal.kernel.poller.PollerRequest;
import com.liferay.portal.kernel.poller.PollerResponse;
import com.liferay.portal.kernel.util.Validator;
import com.poller.action.PortletPollerInstance;
import com.poller.response.NewPollerResponse;

public class PollerProcessorUtil {

	static Map<String, ScheduledFuture<?>> _scheduledFutureTasks = new ConcurrentHashMap<String, ScheduledFuture<?>>();

	private final static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
	
	public static boolean initPolling(final String portletId, final JSONObject json) {
		UsersPortletMapper.init(portletId);
		_log.info(json);
		final JSONObject meta = json.getJSONObject("meta");
		UsersPortletMapper.put(portletId, meta);
		boolean returnVal = false;
		if (!hasFutureTask(portletId)) {
			PollerProcessor processor = PortletPollerInstance.getPollerProcessorInstance(portletId);
			if (Validator.isNotNull(processor)) {
				final Runnable beeper = new Runnable() {

					public void run() {

						try {
							startPolling(portletId);
						}
						catch (Exception e) {
							_log.error(e);
						}
					}
				};
				_scheduledFutureTasks.put(portletId, scheduler.scheduleAtFixedRate(beeper, 0, 1, TimeUnit.SECONDS));
				returnVal = true;
			}
			else {
				_log.error("No poller processor exists for: " + portletId);

			}
		}
		else {
			_log.info("Future task exists: " + portletId);
			returnVal = true;
		}
		return returnVal;

		/*
		 * scheduler.schedule(new Runnable() { public void run() {
		 * beeperHandle.cancel(true); } }, 5, TimeUnit.SECONDS);
		 */

	}

	protected static void startPolling(String portletId) throws Exception {
		Collection<JSONObject> collection = UsersPortletMapper.get(portletId);
//		_log.info("mapper: " + collection.size());
		for(JSONObject json: collection){
			recieve(createPollerHeader(json), portletId);
			json.put("initialRequest", false);
		}
		NodePollerSender.send();
	}
	
	private static PollerHeader createPollerHeader(JSONObject json) {
		long companyId = json.getLong("companyId");
		long userId = json.getLong("userId");
		long browserKey = json.getLong("browserKey");
		String[] portletIds = null;
		boolean initialRequest = json.getBoolean("initialRequest");
		boolean startPolling = true;
		return new PollerHeader(companyId, userId, browserKey, portletIds,
				initialRequest, startPolling);

	}

	private static boolean hasFutureTask(final String porteltId) {

		return _scheduledFutureTasks.containsKey(porteltId);
	}

	public static ScheduledFuture<?> getFutureTask(String portletId) {

		return _scheduledFutureTasks.get(portletId);
	}

	public static boolean cancelPolling(String portletId, long userId) {
		
		int size = UsersPortletMapper.remove(portletId, userId);
		
		if(size == 0){
			
			return cancelPolling(portletId);
		}
		return true;

	}

	private static boolean cancelPolling(String portletId) {
		ScheduledFuture<?> task = getFutureTask(portletId);
		if (Validator.isNotNull(task)) {

			boolean cancelled = task.isDone() || task.isCancelled() || task.cancel(true);
			if (cancelled) {
				_scheduledFutureTasks.remove(portletId);
			}
			return cancelled;
		}
		else {
			return true;
		}
	}

	public static void destroy() {

		for (Map.Entry<String, ScheduledFuture<?>> entry : _scheduledFutureTasks.entrySet()) {
			cancelPolling(entry.getKey());
		}
		// scheduler.shutdown();
		_log.info("...........shutdown.....................");
	}

	private static void recieve(PollerHeader pollerHeader, String portletId)
		throws Exception {

//		_log.info(pollerHeader.toString());
		PollerProcessor processor = PortletPollerInstance.getPollerProcessorInstance(portletId);
		String chunkId = null;
		Map<String, String> parameterMap = new HashMap<String, String>();
		PollerRequest pollerRequest = new PollerRequest(null, pollerHeader, portletId, parameterMap, chunkId, true);

		PollerResponse pollerResponse = new NewPollerResponse(pollerHeader, portletId, chunkId);

		processor.receive(pollerRequest, pollerResponse);

		if (!pollerResponse.isEmpty()) {
			NodePollerSender.push(portletId, pollerHeader.getUserId(), pollerResponse.toJSONObject());
		}

	}
	public static void send(String portletId, long userId, JSONObject dataJSON)
			throws Exception {
		PollerProcessor processor = PortletPollerInstance.getPollerProcessorInstance(portletId);
		PollerHeader pollerHeader = createPollerHeader(UsersPortletMapper.get(portletId, userId));
		Map<String, String> dataMap = getPollerRequestDataMap(dataJSON);
		PollerRequest pollerRequest = new PollerRequest(null, pollerHeader , portletId, dataMap, null, true);
		processor.send(pollerRequest);
	}
	
	private static Map<String, String> getPollerRequestDataMap(JSONObject dataJSON) throws JSONException {
		
		Map<String, String> dataMap = new HashMap<String, String>();
		
		Iterator<String> iterator = dataJSON.keys();
		while (iterator.hasNext()) {
			String key = iterator.next();
			dataMap.put(key, dataJSON.getString(key));
		}
		
		return dataMap;
	}
	

	/*private static void sendToNodeJSServer(PollerRequest pollerRequest, PollerResponse pollerResponse) {

		// _log.info(pollerResponse.toJSONObject());

		try {
			String url = HttpUtil.addParameter(PollerUtil.getNodePollerURL(), "data", pollerResponse.toJSONObject().toString());
			url = HttpUtil.addParameter(url, "portletId", pollerResponse.getPortletId());
			// _log.info(url);
			Request.Get(url).connectTimeout(1000).socketTimeout(1000).execute().returnContent().asString();
		}
		catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			destroy();
		}
	}*/

	static Log _log = LogFactoryUtil.getLog(PollerProcessorUtil.class);

}
