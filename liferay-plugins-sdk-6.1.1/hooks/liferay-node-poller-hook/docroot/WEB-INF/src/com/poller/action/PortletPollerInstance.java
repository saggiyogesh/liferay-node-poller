
package com.poller.action;

import java.util.HashMap;
import java.util.Map;

import com.liferay.portal.kernel.events.ActionException;
import com.liferay.portal.kernel.events.SimpleAction;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.poller.PollerProcessor;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.service.PortletLocalServiceUtil;

public class PortletPollerInstance extends SimpleAction {

	/*
	 * (non-Java-doc)
	 * @see com.liferay.portal.kernel.events.SimpleAction#SimpleAction()
	 */
	public PortletPollerInstance() {

		super();
	}

	/*
	 * (non-Java-doc)
	 * @see com.liferay.portal.kernel.events.SimpleAction#run(String[] ids)
	 */
	public void run(String[] ids)
		throws ActionException {

		for (Portlet portlet : PortletLocalServiceUtil.getPortlets()) {
			String pollerProcessorClass = portlet.getPollerProcessorClass();
			if (Validator.isNotNull(pollerProcessorClass)) {
				_log.info("Poller detected: " + portlet.getPortletId() + " :: " + portlet.getPollerProcessorInstance());
				_pollerProcessorInstances.put(portlet.getPortletId(), portlet.getPollerProcessorInstance());
			}
		}

	}

	private static Map<String, PollerProcessor> _pollerProcessorInstances = new HashMap<String, PollerProcessor>();

	public static PollerProcessor getPollerProcessorInstance(String portletId) {

		return _pollerProcessorInstances.get(portletId);
	}
	
	private static Log _log = LogFactoryUtil.getLog(PortletPollerInstance.class);

}
