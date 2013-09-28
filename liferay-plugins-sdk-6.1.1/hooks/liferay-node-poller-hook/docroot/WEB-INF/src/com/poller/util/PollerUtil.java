package com.poller.util;

import com.liferay.portal.kernel.util.PropsUtil;


public class PollerUtil {
	
	private static final String NODEJS_LIFERAY_POLLER_URL = "nodejs.liferay.poller.url";

	public static String getNodePollerURL() {
		return PropsUtil.get(NODEJS_LIFERAY_POLLER_URL);
		
	}

}
