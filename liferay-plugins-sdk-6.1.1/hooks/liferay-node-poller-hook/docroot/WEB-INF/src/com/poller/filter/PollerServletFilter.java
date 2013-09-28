
package com.poller.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.poller.util.NodePollerSender;
import com.poller.util.PollerProcessorUtil;

public class PollerServletFilter implements Filter {

	@Override
	public void destroy() {

		PollerProcessorUtil.destroy();
		NodePollerSender.destroy();
		_log.info("Filter removed......");
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
		throws IOException, ServletException {

		HttpServletRequest request = ((HttpServletRequest) servletRequest);
		String url = request.getRequestURL().toString();
		if (servletRequest instanceof HttpServletRequest) {
			boolean returnVal = false;
			if (url.contains("/poller/socketConnect")) {
				String portletId = ParamUtil.getString(request, "id");
				String data = ParamUtil.getString(request, "data");
				try {
					JSONObject jsonObject = JSONFactoryUtil.createJSONObject(data);
					returnVal = PollerProcessorUtil.initPolling(portletId, jsonObject);
				}
				catch (JSONException e) {
					e.printStackTrace();
				}
				_log.info("socketConnect: " + portletId);

			}
			else if (url.contains("/poller/socketDisconnect")) {
				String portletId = ParamUtil.getString(request, "id");
				long userId = ParamUtil.getLong(request, "userId");
				_log.info("socketDisconnect: " + portletId);
				_log.info("Poller removed for portlet: " + portletId);
				returnVal = PollerProcessorUtil.cancelPolling(portletId, userId);
			}
			else if (url.contains("/poller/send")) {
				String portletId = ParamUtil.getString(request, "id");
				long userId = ParamUtil.getLong(request, "userId");
				String data = ParamUtil.getString(request, "data");
				try {
					JSONObject jsonObject = JSONFactoryUtil.createJSONObject(data);
					PollerProcessorUtil.send(portletId, userId, jsonObject.getJSONObject("send").getJSONObject("data"));
					returnVal = true;
				}
				catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				_log.info("socketSend: " + portletId);
			}
			else if (url.contains("/poller/destroy")) {
				PollerProcessorUtil.destroy();
				returnVal = true;
			}
			JSONObject jsonObject = JSONFactoryUtil.createJSONObject();
			jsonObject.put("result", returnVal);
			servletResponse.getWriter().write(jsonObject.toString());
		}
		else {
			filterChain.doFilter(servletRequest, servletResponse);
		}
	}

	@Override
	public void init(FilterConfig arg0)
		throws ServletException {

		// TODO Auto-generated method stub
	}

	Log _log = LogFactoryUtil.getLog(PollerServletFilter.class);

}
