package com.personal.htool.web.defense;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class XssServletRequestWrapper extends HttpServletRequestWrapper {

	public XssServletRequestWrapper(HttpServletRequest request) {
		super(request);
	}

	public String getParameter(String name) {
		String value = super.getParameter(name);
		if (value == null) {
			return null;
		}
		return cleanXSS(value);
	}

	public String[] getParameterValues(String name) {
		String[] values = super.getParameterValues(name);
		if (values == null) {
			return null;
		}
		int count = values.length;
		String[] encodedParams = new String[count];
		for (int i = 0; i < count; i++) {
			encodedParams[i] = cleanXSS(values[i]);
		}
		return encodedParams;
	}

	private String cleanXSS(String value) {
		value = value.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
		value = value.replaceAll("\\(", "&#40;").replaceAll("\\)", "&#41;");
		value = value.replaceAll("'", "&#39;");
		value = value.replaceAll("eval\\((.*)\\)", "");
		value = value.replaceAll("[\\\"\\\'][\\s]*javascript:(.*)[\\\"\\\']", "\"\"");
		value = value.replaceAll("script", "");
		return value;
	}

}
