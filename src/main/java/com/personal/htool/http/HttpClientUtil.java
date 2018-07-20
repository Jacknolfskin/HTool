package com.personal.htool.http;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.CharsetUtils;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HttpClient连接池管理类
 */
public class HttpClientUtil {

	private static PoolingHttpClientConnectionManager cm;

	/**
	 * 连接池里的最大连接数
	 */
	private final static int MAX_TOTAL_CONNECTIONS = 2000;

	/**
	 * 每个路由最大连接数
	 */
	private final static int MAX_ROUTE_CONNECTIONS = 500;

	/**
	 * 连接超时
	 */
	private final static int CONNECTION_TIMEOUT = 10000;

	/**
	 * 数据传输超时
	 */
	private final static int SO_TIMEOUT = 100000;

	/**
	 * 编码格式
	 */
	private static  String CHARSET = "UTF-8";
	
	public static void setCharSet(String charset){
		CHARSET=charset;
	}
	/**
	 * HttpClient
	 */
	private static CloseableHttpClient httpClient;

	static {
		try {
			cm = new PoolingHttpClientConnectionManager();
			cm.setMaxTotal(MAX_TOTAL_CONNECTIONS);
			cm.setDefaultMaxPerRoute(MAX_ROUTE_CONNECTIONS);
		} catch (Exception e) {
			// TODO: handle exception
		}

		// 开启监控线程
		Thread monitorThread = new IdleConnectionMonitorThread(cm);
		monitorThread.setDaemon(true);
		monitorThread.start();
	}

	private static CloseableHttpClient getHttpClient() {

		if (httpClient == null) {
			// 加入3次重试机制，解决因服务器压力大，请求失败问题
			HttpRequestRetryHandler myRetryHandler = new HttpRequestRetryHandler() {

				@Override
				public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
					if (executionCount >= 3) {
						// Do not retry if over max retry count
						return false;
					}

					if (exception instanceof NoHttpResponseException) {
						return true;
					}
					return false;
				}

			};

			RequestConfig config = RequestConfig.custom().setConnectTimeout(CONNECTION_TIMEOUT).setSocketTimeout(SO_TIMEOUT).build();
			httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).setConnectionManager(cm)
					.setRetryHandler(myRetryHandler).build();

		}
		return httpClient;
	}

	public static void release() {
		if (cm != null) {
			cm.shutdown();
		}
	}

	static class IdleConnectionMonitorThread extends Thread {

		private final PoolingHttpClientConnectionManager connMgr;
		private volatile boolean shutdown;

		public IdleConnectionMonitorThread(PoolingHttpClientConnectionManager connMgr) {
			super();
			this.connMgr = connMgr;
		}

		@Override
		public void run() {
			try {
				while (!shutdown) {
					synchronized (this) {
						wait(5000);
						// 关闭失效的连接
						connMgr.closeExpiredConnections();
						// 可选的, 关闭30秒内不活动的连接
						connMgr.closeIdleConnections(30, TimeUnit.SECONDS);
					}
				}
			} catch (InterruptedException ex) {
				// terminate
			}
		}

		public void shutdown() {
			shutdown = true;
			synchronized (this) {
				notifyAll();
			}
		}

	}

	/**
	 * 通过URL进行get请求获取完整的HttpResponse对象
	 * 
	 * @param url
	 * @return HttpResponse
	 */
	public static HttpResponse getMethod(String url) {
		HttpGet httpGet = null;
		HttpResponse resp = null;
		try {
			httpGet = new HttpGet(url);
			httpClient = getHttpClient();
			httpGet.setHeader("User-Agent",
					"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1180.79 Safari/537.1");
			httpGet.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			httpGet.setHeader("Content-Type", "text/html;charset=UTF-8");
			resp = httpClient.execute(httpGet);

			int respCode = resp.getStatusLine().getStatusCode();

			if (respCode == HttpStatus.SC_OK) {
				return resp;

			} else {
				System.out.println("请求失败");
				if (httpGet != null)
					httpGet.abort();
			}

		} catch (ClientProtocolException e) {
			// TODO: handle exception
			if (httpGet != null)
				httpGet.abort();
			throw new RuntimeException(e);

		} catch (IOException e) {
			// TODO: handle exception
			if (httpGet != null)
				httpGet.abort();
			throw new RuntimeException(e);
		} catch (Exception e) { // 捕获最大的异常
			// TODO: handle exception
			if (httpGet != null)
				httpGet.abort();
			throw new RuntimeException(e);
		} finally {
			if (httpGet != null) {
				httpGet.releaseConnection();
			}
		}
		return resp;
	}

	/**
	 * 通过URL进行get请求获取完整的HttpResponse对象
	 * 
	 * @param url
	 * @param connectTimeout
	 *            连接超时，单位：毫秒，为空默认为10s
	 * @param socketTimeout
	 *            数据传输超时，单位：毫秒，为空默认为100s
	 * 
	 * @return HttpResponse
	 */
	public static HttpResponse getMethod(String url, String connectTimeout, String socketTimeout) {
		HttpGet httpGet = null;
		HttpResponse resp = null;
		try {

			httpGet = new HttpGet(url);

			int connTimeout = CONNECTION_TIMEOUT;
			int soTimeOut = SO_TIMEOUT;

			if (StringUtils.isNotBlank(connectTimeout)) {
				connTimeout = Integer.parseInt(connectTimeout);
			}

			if (StringUtils.isNotBlank(socketTimeout)) {
				soTimeOut = Integer.parseInt(socketTimeout);
			}

			// 设置请求和传输超时时间
			RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(soTimeOut).setConnectTimeout(connTimeout).build();
			httpGet.setConfig(requestConfig);

			httpClient = getHttpClient();
			httpGet.setHeader("User-Agent",
					"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1180.79 Safari/537.1");
			httpGet.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			httpGet.setHeader("Content-Type", "text/html;charset=UTF-8");
			resp = httpClient.execute(httpGet);

			int respCode = resp.getStatusLine().getStatusCode();

			if (respCode == HttpStatus.SC_OK) {
				return resp;

			} else {
				System.out.println("请求失败");
				if (httpGet != null)
					httpGet.abort();
			}

		} catch (ClientProtocolException e) {
			// TODO: handle exception
			if (httpGet != null)
				httpGet.abort();
			throw new RuntimeException(e);

		} catch (IOException e) {
			// TODO: handle exception
			if (httpGet != null)
				httpGet.abort();
			throw new RuntimeException(e);
		} catch (Exception e) { // 捕获最大的异常
			// TODO: handle exception
			if (httpGet != null)
				httpGet.abort();
			throw new RuntimeException(e);
		} finally {
			if (httpGet != null) {
				httpGet.releaseConnection();
			}
		}
		return resp;
	}

	/**
	 * get请求获取返回字符串
	 * 
	 * @param url
	 * @return
	 */
	public static String doGet(String url) {
		if (StringUtils.isBlank(url)) {
			return null;
		}
		HttpGet httpGet = null;
		try {

			httpGet = new HttpGet(url);
			httpClient = getHttpClient();
			CloseableHttpResponse response = httpClient.execute(httpGet);
			HttpEntity entity = response.getEntity();
			String result = null;
			if (entity != null) {
				result = EntityUtils.toString(entity, CHARSET);
			}
			EntityUtils.consume(entity);
			response.close();
			return result;
		} catch (Exception e) {
			/* e.printStackTrace(); */
			if (httpGet != null)
				httpGet.abort();
			throw new RuntimeException(e);
		} finally {
			if (httpGet != null) {
				httpGet.releaseConnection();
			}
		}
	}

	public static String doGetForRedirect(String url) {
		if (StringUtils.isBlank(url)) {
			return null;
		}
		HttpGet httpGet = null;
		try {

			httpGet = new HttpGet(url);
			httpClient = getHttpClient();
			CloseableHttpResponse response = httpClient.execute(httpGet);

			int statusCode = response.getStatusLine().getStatusCode();

			
			if (statusCode == 300 || statusCode == 301 || statusCode == 302 || statusCode == 303 || statusCode == 307) {
				String ret = "";
				Header[] headers = response.getHeaders("Location");
				if (headers != null && headers.length > 0)
					ret = headers[0].getValue();
				
//				if(!StringUtils.isBlank(redirectURL)){
//					httpGet = new HttpGet(redirectURL);
//					httpClient = getHttpClient();
//					response = httpClient.execute(httpGet);
//				}
				return ret;
			}

			HttpEntity entity = response.getEntity();
			String result = null;
			if (entity != null) {
				result = EntityUtils.toString(entity, CHARSET);
			}
			EntityUtils.consume(entity);
			response.close();
			return result;
		} catch (Exception e) {
			/* e.printStackTrace(); */
			if (httpGet != null)
				httpGet.abort();
			throw new RuntimeException(e);
		} finally {
			if (httpGet != null) {
				httpGet.releaseConnection();
			}
		}
	}

	/**
	 * get请求获取返回字符串
	 * 
	 * @param url
	 * @param connectTimeout
	 *            连接超时，单位：毫秒，为空默认为10s
	 * @param socketTimeout
	 *            数据传输超时，单位：毫秒，为空默认为100s
	 * @return
	 */
	public static String doGet(String url, String connectTimeout, String socketTimeout) {
		if (StringUtils.isBlank(url)) {
			return null;
		}
		HttpGet httpGet = null;
		try {

			httpGet = new HttpGet(url);

			int connTimeout = CONNECTION_TIMEOUT;
			int soTimeOut = SO_TIMEOUT;

			if (StringUtils.isNotBlank(connectTimeout)) {
				connTimeout = Integer.parseInt(connectTimeout);
			}

			if (StringUtils.isNotBlank(socketTimeout)) {
				soTimeOut = Integer.parseInt(socketTimeout);
			}

			// 设置请求和传输超时时间
			RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(soTimeOut).setConnectTimeout(connTimeout).build();
			httpGet.setConfig(requestConfig);

			httpClient = getHttpClient();
			CloseableHttpResponse response = httpClient.execute(httpGet);
			HttpEntity entity = response.getEntity();
			String result = null;
			if (entity != null) {
				result = EntityUtils.toString(entity, CHARSET);
			}
			EntityUtils.consume(entity);
			response.close();
			return result;
		} catch (Exception e) {
			/* e.printStackTrace(); */
			if (httpGet != null)
				httpGet.abort();
			throw new RuntimeException(e);
		} finally {
			if (httpGet != null) {
				httpGet.releaseConnection();
			}
		}
	}

	/**
	 * HTTP Get 获取内容
	 * 
	 * @param url
	 *            请求的url地址 ?之前的地址
	 * @param params
	 *            请求的参数
	 * @return 页面内容
	 */
	public static String doGet(String url, Map<String, String> params) {
		if (StringUtils.isBlank(url)) {
			return null;
		}
		HttpGet httpGet = null;
		try {
			if (params != null && !params.isEmpty()) {
				List<NameValuePair> pairs = new ArrayList<NameValuePair>(params.size());
				for (Map.Entry<String, String> entry : params.entrySet()) {
					String value = entry.getValue();
					if (value != null) {
						pairs.add(new BasicNameValuePair(entry.getKey(), value));
					}
				}
				url += "?" + EntityUtils.toString(new UrlEncodedFormEntity(pairs, CHARSET));
			}
			httpGet = new HttpGet(url);
			httpClient = getHttpClient();
			CloseableHttpResponse response = httpClient.execute(httpGet);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != 200) {
				httpGet.abort();
				throw new RuntimeException("HttpClient,error status code :" + statusCode);
			}
			HttpEntity entity = response.getEntity();
			String result = null;
			if (entity != null) {
				result = EntityUtils.toString(entity, CHARSET);
			}
			EntityUtils.consume(entity);
			response.close();
			return result;
		} catch (Exception e) {
			/* e.printStackTrace(); */
			if (httpGet != null)
				httpGet.abort();
			throw new RuntimeException(e);
		} finally {
			if (httpGet != null) {
				httpGet.releaseConnection();
			}
		}
	}

	/**
	 * HTTP Get 获取内容
	 * 
	 * @param url
	 *            请求的url地址 ?之前的地址
	 * @param params
	 *            请求的参数
	 * @param connectTimeout
	 *            连接超时，单位：毫秒，为空默认为10s
	 * @param socketTimeout
	 *            数据传输超时，单位：毫秒，为空默认为100s
	 * @return 页面内容
	 */
	public static String doGet(String url, Map<String, String> params, String connectTimeout, String socketTimeout) {
		if (StringUtils.isBlank(url)) {
			return null;
		}
		HttpGet httpGet = null;
		try {
			if (params != null && !params.isEmpty()) {
				List<NameValuePair> pairs = new ArrayList<NameValuePair>(params.size());
				for (Map.Entry<String, String> entry : params.entrySet()) {
					String value = entry.getValue();
					if (value != null) {
						pairs.add(new BasicNameValuePair(entry.getKey(), value));
					}
				}
				url += "?" + EntityUtils.toString(new UrlEncodedFormEntity(pairs, CHARSET));
			}
			httpGet = new HttpGet(url);

			int connTimeout = CONNECTION_TIMEOUT;
			int soTimeOut = SO_TIMEOUT;

			if (StringUtils.isNotBlank(connectTimeout)) {
				connTimeout = Integer.parseInt(connectTimeout);
			}

			if (StringUtils.isNotBlank(socketTimeout)) {
				soTimeOut = Integer.parseInt(socketTimeout);
			}

			// 设置请求和传输超时时间
			RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(soTimeOut).setConnectTimeout(connTimeout).build();
			httpGet.setConfig(requestConfig);

			httpClient = getHttpClient();
			CloseableHttpResponse response = httpClient.execute(httpGet);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != 200) {
				httpGet.abort();
				throw new RuntimeException("HttpClient,error status code :" + statusCode);
			}
			HttpEntity entity = response.getEntity();
			String result = null;
			if (entity != null) {
				result = EntityUtils.toString(entity, CHARSET);
			}
			EntityUtils.consume(entity);
			response.close();
			return result;
		} catch (Exception e) {
			/* e.printStackTrace(); */
			if (httpGet != null)
				httpGet.abort();
			throw new RuntimeException(e);
		} finally {
			if (httpGet != null) {
				httpGet.releaseConnection();
			}
		}
	}

	/**
	 * HTTP Get 获取内容
	 * 
	 * @param url
	 *            请求的url地址 ?之前的地址
	 * @param params
	 *            请求的参数
	 * @param headers
	 *            header头信息
	 * @return 页面内容
	 */
	public static String doGet(String url, Map<String, String> params, Map<String, String> headers) {
		if (StringUtils.isBlank(url)) {
			return null;
		}
		HttpGet httpGet = null;
		try {
			if (params != null && !params.isEmpty()) {
				List<NameValuePair> pairs = new ArrayList<NameValuePair>(params.size());
				for (Map.Entry<String, String> entry : params.entrySet()) {
					String value = entry.getValue();
					if (value != null) {
						pairs.add(new BasicNameValuePair(entry.getKey(), value));
					}
				}
				url += "?" + EntityUtils.toString(new UrlEncodedFormEntity(pairs, CHARSET));
			}
			httpGet = new HttpGet(url);

			// 设置header
			if (headers != null && !headers.isEmpty()) {
				for (Map.Entry<String, String> entry : headers.entrySet()) {
					String value = entry.getValue();
					if (value != null) {
						httpGet.addHeader(entry.getKey(),  URLEncoder.encode(value,CHARSET));
					}
				}
			}

			httpClient = getHttpClient();
			CloseableHttpResponse response = httpClient.execute(httpGet);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != 200) {
				httpGet.abort();
				throw new RuntimeException("HttpClient,error status code :" + statusCode);
			}
			HttpEntity entity = response.getEntity();
			String result = null;
			if (entity != null) {
				result = EntityUtils.toString(entity, CHARSET);
			}
			EntityUtils.consume(entity);
			response.close();
			return result;
		} catch (Exception e) {
			/* e.printStackTrace(); */
			if (httpGet != null)
				httpGet.abort();
			throw new RuntimeException(e);
		} finally {
			if (httpGet != null) {
				httpGet.releaseConnection();
			}
		}
	}

	/**
	 * HTTP Get 获取内容
	 * 
	 * @param url
	 *            请求的url地址 ?之前的地址
	 * @param params
	 *            请求的参数
	 * @param headers
	 *            header头信息
	 * @param connectTimeout
	 *            连接超时，单位：毫秒，为空默认为10s
	 * @param socketTimeout
	 *            数据传输超时，单位：毫秒，为空默认为100s
	 * @return 页面内容
	 */
	public static String doGet(String url, Map<String, String> params, Map<String, String> headers, String connectTimeout,
			String socketTimeout) {
		if (StringUtils.isBlank(url)) {
			return null;
		}
		HttpGet httpGet = null;
		try {
			if (params != null && !params.isEmpty()) {
				List<NameValuePair> pairs = new ArrayList<NameValuePair>(params.size());
				for (Map.Entry<String, String> entry : params.entrySet()) {
					String value = entry.getValue();
					if (value != null) {
						pairs.add(new BasicNameValuePair(entry.getKey(), value));
					}
				}
				url += "?" + EntityUtils.toString(new UrlEncodedFormEntity(pairs, CHARSET));
			}
			httpGet = new HttpGet(url);

			int connTimeout = CONNECTION_TIMEOUT;
			int soTimeOut = SO_TIMEOUT;

			if (StringUtils.isNotBlank(connectTimeout)) {
				connTimeout = Integer.parseInt(connectTimeout);
			}

			if (StringUtils.isNotBlank(socketTimeout)) {
				soTimeOut = Integer.parseInt(socketTimeout);
			}

			// 设置请求和传输超时时间
			RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(soTimeOut).setConnectTimeout(connTimeout).build();
			httpGet.setConfig(requestConfig);

			// 设置header
			if (headers != null && !headers.isEmpty()) {
				for (Map.Entry<String, String> entry : headers.entrySet()) {
					String value = entry.getValue();
					if (value != null) {
						httpGet.addHeader(entry.getKey(),  URLEncoder.encode(value,CHARSET));
					}
				}
			}

			httpClient = getHttpClient();
			CloseableHttpResponse response = httpClient.execute(httpGet);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != 200) {
				httpGet.abort();
				throw new RuntimeException("HttpClient,error status code :" + statusCode);
			}
			HttpEntity entity = response.getEntity();
			String result = null;
			if (entity != null) {
				result = EntityUtils.toString(entity, CHARSET);
			}
			EntityUtils.consume(entity);
			response.close();
			return result;
		} catch (Exception e) {
			/* e.printStackTrace(); */
			if (httpGet != null)
				httpGet.abort();
			throw new RuntimeException(e);
		} finally {
			if (httpGet != null) {
				httpGet.releaseConnection();
			}
		}
	}

	/**
	 * HTTP Post 获取内容
	 * 
	 * @param url
	 *            请求的url地址 ?之前的地址
	 * @param params
	 *            请求的参数
	 * @return 页面内容
	 */
	public static String doPost(String url, Map<String, String> params) throws ClientProtocolException, IOException {
		if (StringUtils.isBlank(url)) {
			return null;
		}
		HttpPost httpPost = null;
		CloseableHttpResponse response = null;
		try {
			httpPost = new HttpPost(url);
			
			List<NameValuePair> pairs = null;
			if (params != null && !params.isEmpty()) {
				pairs = new ArrayList<NameValuePair>(params.size());
				for (Map.Entry<String, String> entry : params.entrySet()) {
					String value = entry.getValue();
					if (value != null) {
						pairs.add(new BasicNameValuePair(entry.getKey(), value));
						httpPost.setHeader(entry.getKey(), URLEncoder.encode(value,CHARSET));
					}
				}
			}
			
			if (pairs != null && pairs.size() > 0) {
				
				httpPost.setEntity(new UrlEncodedFormEntity(pairs, CHARSET));
			}
			httpClient = getHttpClient();
			response = httpClient.execute(httpPost);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != 200) {
				httpPost.abort();
				throw new RuntimeException("HttpClient,error status code :" + statusCode);
			}
			HttpEntity entity = response.getEntity();
			String result = null;
			if (entity != null) {
				result = EntityUtils.toString(entity, CHARSET);
			}
			EntityUtils.consume(entity);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
			if (response != null)
				response.close();
			if (httpPost != null)
				httpPost.releaseConnection();
		}
	}

	/**
	 * HTTP Post 获取内容
	 * 
	 * @param url
	 *            请求的url地址 ?之前的地址
	 * @param params
	 *            请求的参数
	 * @param connectTimeout
	 *            连接超时，单位：毫秒，为空默认为10s
	 * @param socketTimeout
	 *            数据传输超时，单位：毫秒，为空默认为100s
	 * @return 页面内容
	 */
	public static String doPost(String url, Map<String, String> params, String connectTimeout, String socketTimeout)
			throws ClientProtocolException, IOException {
		if (StringUtils.isBlank(url)) {
			return null;
		}
		HttpPost httpPost = null;
		CloseableHttpResponse response = null;
		try {
			List<NameValuePair> pairs = null;
			if (params != null && !params.isEmpty()) {
				pairs = new ArrayList<NameValuePair>(params.size());
				for (Map.Entry<String, String> entry : params.entrySet()) {
					String value = entry.getValue();
					if (value != null) {
						pairs.add(new BasicNameValuePair(entry.getKey(), value));
					}
				}
			}
			httpPost = new HttpPost(url);

			int connTimeout = CONNECTION_TIMEOUT;
			int soTimeOut = SO_TIMEOUT;

			if (StringUtils.isNotBlank(connectTimeout)) {
				connTimeout = Integer.parseInt(connectTimeout);
			}

			if (StringUtils.isNotBlank(socketTimeout)) {
				soTimeOut = Integer.parseInt(socketTimeout);
			}

			// 设置请求和传输超时时间
			RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(soTimeOut).setConnectTimeout(connTimeout).build();
			httpPost.setConfig(requestConfig);

			if (pairs != null && pairs.size() > 0) {
				httpPost.setEntity(new UrlEncodedFormEntity(pairs, CHARSET));
			}
			httpClient = getHttpClient();
			response = httpClient.execute(httpPost);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != 200) {
				httpPost.abort();
				throw new RuntimeException("HttpClient,error status code :" + statusCode);
			}
			HttpEntity entity = response.getEntity();
			String result = null;
			if (entity != null) {
				result = EntityUtils.toString(entity, CHARSET);
			}
			EntityUtils.consume(entity);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
			if (response != null)
				response.close();
			if (httpPost != null)
				httpPost.releaseConnection();
		}
	}

	/**
	 * HTTP Post 获取内容
	 * 
	 * @param url
	 *            请求的url地址 ?之前的地址
	 * @param params
	 *            请求的参数
	 * @param headers
	 *            header头信息
	 * @return 页面内容
	 */
	public static String doPost(String url, Map<String, String> params, Map<String, String> headers) throws ClientProtocolException,
			IOException {
		if (StringUtils.isBlank(url)) {
			return null;
		}
		HttpPost httpPost = null;
		CloseableHttpResponse response = null;
		try {
			List<NameValuePair> pairs = null;
			if (params != null && !params.isEmpty()) {
				pairs = new ArrayList<NameValuePair>(params.size());
				for (Map.Entry<String, String> entry : params.entrySet()) {
					String value = entry.getValue();
					if (value != null) {
						pairs.add(new BasicNameValuePair(entry.getKey(), value));
					}
				}
			}
			httpPost = new HttpPost(url);

			// 设置header
			if (headers != null && !headers.isEmpty()) {
				for (Map.Entry<String, String> entry : headers.entrySet()) {
					String value = entry.getValue();
					if (value != null) {
						httpPost.addHeader(entry.getKey(),  URLEncoder.encode(value,CHARSET));
					}
				}
			}

			if (pairs != null && pairs.size() > 0) {
				httpPost.setEntity(new UrlEncodedFormEntity(pairs, CHARSET));
			}
			httpClient = getHttpClient();
			response = httpClient.execute(httpPost);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != 200) {
				httpPost.abort();
				throw new RuntimeException("HttpClient,error status code :" + statusCode);
			}
			HttpEntity entity = response.getEntity();
			String result = null;
			if (entity != null) {
				result = EntityUtils.toString(entity, CHARSET);
			}
			EntityUtils.consume(entity);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
			if (response != null)
				response.close();
			if (httpPost != null)
				httpPost.releaseConnection();
		}
	}


	public static String doPost(String url,byte [] data, Map<String, String> headers) throws ClientProtocolException,
			IOException {
		if (StringUtils.isBlank(url)) {
			return null;
		}
		HttpPost httpPost = null;
		CloseableHttpResponse response = null;
		try {

			httpPost = new HttpPost(url);

			// 设置header
			if (headers != null && !headers.isEmpty()) {
				for (Map.Entry<String, String> entry : headers.entrySet()) {
					String value = entry.getValue();
					if (value != null) {
						httpPost.addHeader(entry.getKey(),  URLEncoder.encode(value,CHARSET));
					}
				}
			}

			if (data != null && data.length > 0) {
				httpPost.setEntity(new ByteArrayEntity(data));
			}
			httpClient = getHttpClient();
			response = httpClient.execute(httpPost);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != 200) {
				httpPost.abort();
				throw new RuntimeException("HttpClient,error status code :" + statusCode);
			}
			HttpEntity entity = response.getEntity();
			String result = null;
			if (entity != null) {
				result = EntityUtils.toString(entity, CHARSET);
			}
			EntityUtils.consume(entity);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
			if (response != null)
				response.close();
			if (httpPost != null)
				httpPost.releaseConnection();
		}
	}

	/**
	 * HTTP Post 获取内容
	 * 
	 * @param url
	 *            请求的url地址 ?之前的地址
	 * @param params
	 *            请求的参数
	 * @param headers
	 *            header头信息
	 * @param connectTimeout
	 *            连接超时，单位：毫秒，为空默认为10s
	 * @param socketTimeout
	 *            数据传输超时，单位：毫秒，为空默认为100s
	 * @return 页面内容
	 */
	public static String doPost(String url, Map<String, String> params, Map<String, String> headers, String connectTimeout,
			String socketTimeout) throws ClientProtocolException, IOException {
		if (StringUtils.isBlank(url)) {
			return null;
		}
		HttpPost httpPost = null;
		CloseableHttpResponse response = null;
		try {
			List<NameValuePair> pairs = null;
			if (params != null && !params.isEmpty()) {
				pairs = new ArrayList<NameValuePair>(params.size());
				for (Map.Entry<String, String> entry : params.entrySet()) {
					String value = entry.getValue();
					if (value != null) {
						pairs.add(new BasicNameValuePair(entry.getKey(), value));
					}
				}
			}
			httpPost = new HttpPost(url);

			int connTimeout = CONNECTION_TIMEOUT;
			int soTimeOut = SO_TIMEOUT;

			if (StringUtils.isNotBlank(connectTimeout)) {
				connTimeout = Integer.parseInt(connectTimeout);
			}

			if (StringUtils.isNotBlank(socketTimeout)) {
				soTimeOut = Integer.parseInt(socketTimeout);
			}

			// 设置请求和传输超时时间
			RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(soTimeOut).setConnectTimeout(connTimeout).build();
			httpPost.setConfig(requestConfig);

			// 设置header
			if (headers != null && !headers.isEmpty()) {
				for (Map.Entry<String, String> entry : headers.entrySet()) {
					String value = entry.getValue();
					if (value != null) {
						httpPost.addHeader(entry.getKey(),  URLEncoder.encode(value,CHARSET));
					}
				}
			}

			if (pairs != null && pairs.size() > 0) {
				httpPost.setEntity(new UrlEncodedFormEntity(pairs, CHARSET));
			}
			httpClient = getHttpClient();
			response = httpClient.execute(httpPost);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != 200) {
				httpPost.abort();
				throw new RuntimeException("HttpClient,error status code :" + statusCode);
			}
			HttpEntity entity = response.getEntity();
			String result = null;
			if (entity != null) {
				result = EntityUtils.toString(entity, CHARSET);
			}
			EntityUtils.consume(entity);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
			if (response != null)
				response.close();
			if (httpPost != null)
				httpPost.releaseConnection();
		}
	}

	/**
	 * HTTP Post 获取内容
	 * 
	 * @param url
	 *            请求的url地址
	 * @param jsonData
	 *            请求的json数据字符串
	 * @return 返回内容
	 */
	public static String doPostJson(String url, String jsonData, Map<String, String> headers) throws ClientProtocolException, IOException {
		httpClient = getHttpClient();
		CloseableHttpResponse response = null;
		HttpPost httpPost = null;
		try {
			httpPost = new HttpPost(url);

			// 设置header
			if (headers != null && !headers.isEmpty()) {
				for (Map.Entry<String, String> entry : headers.entrySet()) {
					String value = entry.getValue();
					if (value != null) {
						httpPost.addHeader(entry.getKey(),  URLEncoder.encode(value,CHARSET));
					}
				}
			}

			StringEntity s = new StringEntity(jsonData, "UTF-8");
			s.setContentEncoding("UTF-8");
			s.setContentType("application/json");
			httpPost.setEntity(s);
			response = httpClient.execute(httpPost);
			int httpCode = response.getStatusLine().getStatusCode();
			if (httpCode != 200) {
				httpPost.abort();
				throw new RuntimeException("HttpClient,error status code :" + httpCode);
			}
			String result = null;
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				result = EntityUtils.toString(entity, CHARSET);
			}
			EntityUtils.consume(entity);
			return result;

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}finally {
			if (response != null)
				response.close();
			if (httpPost != null)
				httpPost.releaseConnection();
		}
	}

	/**
	 * HTTP Post 获取内容
	 * 
	 * @param url
	 *            请求的url地址
	 * @param jsonData
	 *            请求的json数据字符串
	 * @param connectTimeout
	 *            连接超时，单位：毫秒，为空默认为10s
	 * @param socketTimeout
	 *            数据传输超时，单位：毫秒，为空默认为100s
	 * @return 返回内容
	 */
	public static String doPostJson(String url, String jsonData, Map<String, String> headers, String connectTimeout, String socketTimeout)
			throws ClientProtocolException, IOException {
		httpClient = getHttpClient();
		CloseableHttpResponse response = null;
		HttpPost httpPost = null;
		try {
			httpPost = new HttpPost(url);

			int connTimeout = CONNECTION_TIMEOUT;
			int soTimeOut = SO_TIMEOUT;

			if (StringUtils.isNotBlank(connectTimeout)) {
				connTimeout = Integer.parseInt(connectTimeout);
			}

			if (StringUtils.isNotBlank(socketTimeout)) {
				soTimeOut = Integer.parseInt(socketTimeout);
			}

			// 设置请求和传输超时时间
			RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(soTimeOut).setConnectTimeout(connTimeout).build();
			httpPost.setConfig(requestConfig);

			// 设置header
			if (headers != null && !headers.isEmpty()) {
				for (Map.Entry<String, String> entry : headers.entrySet()) {
					String value = entry.getValue();
					if (value != null) {
						httpPost.addHeader(entry.getKey(),  URLEncoder.encode(value,CHARSET));
					}
				}
			}

			StringEntity s = new StringEntity(jsonData, "UTF-8");
			s.setContentEncoding("UTF-8");
			s.setContentType("application/json");
			httpPost.setEntity(s);
			response = httpClient.execute(httpPost);
			int httpCode = response.getStatusLine().getStatusCode();
			if (httpCode != 200) {
				httpPost.abort();
				throw new RuntimeException("HttpClient,error status code :" + httpCode);
			}
			String result = null;
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				result = EntityUtils.toString(entity, CHARSET);
			}
			EntityUtils.consume(entity);
			return result;

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}finally {
			if (response != null)
				response.close();
			if (httpPost != null)
				httpPost.releaseConnection();
		}
	}

	/**
	 * HTTP Post 获取内容
	 * 
	 * @param url
	 *            请求的url地址
	 * @param jsonData
	 *            请求的json数据字符串
	 * @return 返回内容
	 */
	public static String doPostJson(String url, String jsonData) throws ClientProtocolException, IOException {
		httpClient = getHttpClient();
		CloseableHttpResponse response = null;
		HttpPost httpPost = null;
		try {
			httpPost = new HttpPost(url);

			StringEntity s = new StringEntity(jsonData, "UTF-8");
			s.setContentEncoding("UTF-8");
			s.setContentType("application/json");
			httpPost.setEntity(s);
			response = httpClient.execute(httpPost);
			int httpCode = response.getStatusLine().getStatusCode();
			if (httpCode != 200) {
				httpPost.abort();
				throw new RuntimeException("HttpClient,error status code :" + httpCode);
			}
			String result = null;
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				result = EntityUtils.toString(entity, CHARSET);
			}
			EntityUtils.consume(entity);
			return result;

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}finally {
			if (response != null)
				response.close();
			if (httpPost != null)
				httpPost.releaseConnection();
		}

	}

	/**
	 * HTTP Post 获取内容
	 * 
	 * @param url
	 *            请求的url地址
	 * @param jsonData
	 *            请求的json数据字符串
	 * @param connectTimeout
	 *            连接超时，单位：毫秒，为空默认为10s
	 * @param socketTimeout
	 *            数据传输超时，单位：毫秒，为空默认为100s
	 * @return 返回内容
	 */
	public static String doPostJson(String url, String jsonData, String connectTimeout, String socketTimeout) throws ClientProtocolException,
			IOException {
		httpClient = getHttpClient();
		CloseableHttpResponse response = null;
		HttpPost httpPost = null;
		try {
			httpPost = new HttpPost(url);

			int connTimeout = CONNECTION_TIMEOUT;
			int soTimeOut = SO_TIMEOUT;

			if (StringUtils.isNotBlank(connectTimeout)) {
				connTimeout = Integer.parseInt(connectTimeout);
			}

			if (StringUtils.isNotBlank(socketTimeout)) {
				soTimeOut = Integer.parseInt(socketTimeout);
			}

			// 设置请求和传输超时时间
			RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(soTimeOut).setConnectTimeout(connTimeout).build();
			httpPost.setConfig(requestConfig);

			StringEntity s = new StringEntity(jsonData, "UTF-8");
			s.setContentEncoding("UTF-8");
			s.setContentType("application/json");
			httpPost.setEntity(s);
			response = httpClient.execute(httpPost);
			int httpCode = response.getStatusLine().getStatusCode();
			if (httpCode != 200) {
				httpPost.abort();
				throw new RuntimeException("HttpClient,error status code :" + httpCode);
			}
			String result = null;
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				result = EntityUtils.toString(entity, CHARSET);
			}
			EntityUtils.consume(entity);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}finally {
			if (response != null)
				response.close();
			if (httpPost != null)
				httpPost.releaseConnection();
		}

	}

	/**
	 * HTTP Post 获取内容
	 * 
	 * @param url
	 *            请求的url地址
	 * @param xmlData
	 *            请求的json数据字符串
	 * @return 返回内容
	 */
	public static String doPostXml(String url, String xmlData) throws ClientProtocolException, IOException {
		httpClient = getHttpClient();
		CloseableHttpResponse response = null;
		HttpPost httpPost = null;
		try {
			httpPost = new HttpPost(url);

			StringEntity s = new StringEntity(xmlData, "UTF-8");
			s.setContentEncoding("UTF-8");
			s.setContentType("application/xml");
			httpPost.setEntity(s);
			response = httpClient.execute(httpPost);
			int httpCode = response.getStatusLine().getStatusCode();
			if (httpCode != 200) {
				httpPost.abort();
				throw new RuntimeException("HttpClient,error status code :" + httpCode);
			}
			String result = null;
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				result = EntityUtils.toString(entity, CHARSET);
			}
			EntityUtils.consume(entity);
			return result;

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}finally {
			if (response != null)
				response.close();
			if (httpPost != null)
				httpPost.releaseConnection();
		}

	}

	/**
	 * HTTP Post 获取内容
	 * 
	 * @param url
	 *            请求的url地址
	 * @param xmlData
	 *            请求的json数据字符串
	 * @param connectTimeout
	 *            连接超时，单位：毫秒，为空默认为10s
	 * @param socketTimeout
	 *            数据传输超时，单位：毫秒，为空默认为100s
	 * @return 返回内容
	 */
	public static String doPostXml(String url, String xmlData, String connectTimeout, String socketTimeout) throws ClientProtocolException,
			IOException {
		httpClient = getHttpClient();
		CloseableHttpResponse response = null;
		HttpPost httpPost = null;
		try {
			httpPost = new HttpPost(url);

			int connTimeout = CONNECTION_TIMEOUT;
			int soTimeOut = SO_TIMEOUT;

			if (StringUtils.isNotBlank(connectTimeout)) {
				connTimeout = Integer.parseInt(connectTimeout);
			}

			if (StringUtils.isNotBlank(socketTimeout)) {
				soTimeOut = Integer.parseInt(socketTimeout);
			}

			// 设置请求和传输超时时间
			RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(soTimeOut).setConnectTimeout(connTimeout).build();
			httpPost.setConfig(requestConfig);

			StringEntity s = new StringEntity(xmlData, "UTF-8");
			s.setContentEncoding("UTF-8");
			s.setContentType("application/xml");
			httpPost.setEntity(s);
			response = httpClient.execute(httpPost);
			int httpCode = response.getStatusLine().getStatusCode();
		
			if (httpCode != 200) {
				httpPost.abort();
				throw new RuntimeException("HttpClient,error status code :" + httpCode);
			}
			String result = null;
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				result = EntityUtils.toString(entity, CHARSET);
			}
			EntityUtils.consume(entity);
			return result;

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}finally {
			if (response != null)
				response.close();
			if (httpPost != null)
				httpPost.releaseConnection();
		}

	}
	
	/**
	 * HTTP Post 获取内容
	 * 
	 * @param url
	 *            请求的url地址
	 * @param xmlData
	 *            请求的json数据字符串
	 * @return 返回内容
	 */
	public static String doPostXml(String url, String xmlData, Map<String, String> headers) throws ClientProtocolException, IOException {
		httpClient = getHttpClient();
		CloseableHttpResponse response = null;
		HttpPost httpPost = null;
		try {
			httpPost = new HttpPost(url);

			// 设置header
			if (headers != null && !headers.isEmpty()) {
				for (Map.Entry<String, String> entry : headers.entrySet()) {
					String value = entry.getValue();
					if (value != null) {
						httpPost.addHeader(entry.getKey(), value);
					}
				}
			}

			StringEntity s = new StringEntity(xmlData, "UTF-8");
			s.setContentEncoding("UTF-8");
			s.setContentType("application/xml");
			httpPost.setEntity(s);
			response = httpClient.execute(httpPost);
			int httpCode = response.getStatusLine().getStatusCode();
			if (httpCode != 200) {
				httpPost.abort();
				throw new RuntimeException("HttpClient,error status code :" + httpCode);
			}
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != 200) {
				httpPost.abort();
				throw new RuntimeException("HttpClient,error status code :" + statusCode);
			}
			String result = null;
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				result = EntityUtils.toString(entity, CHARSET);
			}
			EntityUtils.consume(entity);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}finally {
			if (response != null)
				response.close();
			if (httpPost != null)
				httpPost.releaseConnection();
		}

	}


	/**
	 * HTTP Post 获取内容
	 * @param url 请求的url地址
	 * @param xmlData 请求的json数据字符串
	 * @param headers headers
	 * @param connectTimeout 连接超时，单位：毫秒，为空默认为10s
	 * @param socketTimeout 数据传输超时，单位：毫秒，为空默认为100s
	 * @return 返回内容
	 * @throws ClientProtocolException
     * @throws IOException
     */
	public static String doPostXml(String url, String xmlData, Map<String, String> headers, String connectTimeout, String socketTimeout)
			throws ClientProtocolException, IOException {
		httpClient = getHttpClient();
		CloseableHttpResponse response = null;
		HttpPost httpPost = null;
		try {
			httpPost = new HttpPost(url);

			int connTimeout = CONNECTION_TIMEOUT;
			int soTimeOut = SO_TIMEOUT;

			if (StringUtils.isNotBlank(connectTimeout)) {
				connTimeout = Integer.parseInt(connectTimeout);
			}

			if (StringUtils.isNotBlank(socketTimeout)) {
				soTimeOut = Integer.parseInt(socketTimeout);
			}

			// 设置请求和传输超时时间
			RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(soTimeOut).setConnectTimeout(connTimeout).build();
			httpPost.setConfig(requestConfig);

			// 设置header
			if (headers != null && !headers.isEmpty()) {
				for (Map.Entry<String, String> entry : headers.entrySet()) {
					String value = entry.getValue();
					if (value != null) {
						httpPost.addHeader(entry.getKey(), value);
					}
				}
			}

			StringEntity s = new StringEntity(xmlData, "UTF-8");
			s.setContentEncoding("UTF-8");
			s.setContentType("application/xml");
			httpPost.setEntity(s);
			response = httpClient.execute(httpPost);
			int httpCode = response.getStatusLine().getStatusCode();
			if (httpCode != 200) {
				httpPost.abort();
				throw new RuntimeException("HttpClient,error status code :" + httpCode);
			}
			String result = null;
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				result = EntityUtils.toString(entity, CHARSET);
			}
			EntityUtils.consume(entity);
			return result;

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}finally {
			if (response != null)
				response.close();
			if (httpPost != null)
				httpPost.releaseConnection();
		}

	}
	
	/**
	 * 上传文件
	 * 
	 * @param url
	 *            上传URL
	 * @param headers
	 *            头信息
	 * @param localFilePath
	 *            本地文件地址
	 * @param uploadFile
	 *            对应服务端类的同名属性
	 * @param uploadFileName
	 *            对应服务端类的同名属性
	 * @throws ParseException
	 * @throws IOException
	 */
	public static String postFile(String url, Map<String, String> headers, String localFilePath, String uploadFile, String uploadFileName)
			throws ParseException, IOException {
		CloseableHttpClient httpClient = getHttpClient();
		CloseableHttpResponse response = null;
		HttpPost httpPost = null;
		HttpEntity resEntity = null;
		String result = null;
		try {
			// 把一个普通参数和文件上传给下面这个地址
			httpPost = new HttpPost(url);

			// 设置header
			if (headers != null && !headers.isEmpty()) {
				for (Map.Entry<String, String> entry : headers.entrySet()) {
					String value = entry.getValue();
					if (value != null) {
						httpPost.addHeader(entry.getKey(),  URLEncoder.encode(value,CHARSET));
					}
				}
			}
			// 把文件转换成流对象FileBody
			File file = new File(localFilePath);
			FileBody bin = new FileBody(file);
			String fileName = localFilePath.substring(localFilePath.lastIndexOf(File.separator) + 1);

			StringBody uploadFileNameBody = new StringBody(fileName, ContentType.create("text/plain", Consts.UTF_8));
			HttpEntity reqEntity = null;
			// 以浏览器兼容模式运行，防止文件名乱码。
			if (uploadFileName == null) {
				reqEntity = MultipartEntityBuilder.create().setMode(HttpMultipartMode.BROWSER_COMPATIBLE).addPart(uploadFile, bin)// uploadFile对应服务端类的同名属性
						.setCharset(CharsetUtils.get("UTF-8")).build();
			} else {
				reqEntity = MultipartEntityBuilder.create().setMode(HttpMultipartMode.BROWSER_COMPATIBLE).addPart(uploadFile, bin)// uploadFile对应服务端类的同名属性
						.addPart(uploadFileName, uploadFileNameBody)// uploadFileName对应服务端类的同名属性
						.setCharset(CharsetUtils.get("UTF-8")).build();
			}
			httpPost.setEntity(reqEntity);
			// 发起请求 并返回请求的响应
			response = httpClient.execute(httpPost);

			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != 200) {
				httpPost.abort();
				throw new RuntimeException("HttpClient,error status code :" + statusCode);
			}
			HttpEntity entity = response.getEntity();

			if (entity != null) {
				result = EntityUtils.toString(entity, CHARSET);
			}
			EntityUtils.consume(entity);
			response.close();

		}catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
			// 销毁
			EntityUtils.consume(resEntity);
			if (response != null)
				response.close();
			if (httpPost != null)
				httpPost.releaseConnection();
		}
		return result;

	}

	/**
	 * 上传文件
	 *
	 * @param url
	 *            上传URL
	 * @param headers
	 *            头信息
	 * @param localFilePath
	 *            本地文件地址
	 * @param uploadFile
	 *            对应服务端类的同名属性
	 * @param uploadFileName
	 *            对应服务端类的同名属性
	 * @param connectTimeout
	 *            连接超时，单位：毫秒，为空默认为10s
	 * @param socketTimeout
	 *            数据传输超时，单位：毫秒，为空默认为100s
	 * @throws ParseException
	 * @throws IOException
	 */
	public static String postFile(String url, Map<String, String> headers, String localFilePath, String uploadFile, String uploadFileName,
			String connectTimeout, String socketTimeout) throws ParseException, IOException {
		CloseableHttpClient httpClient = getHttpClient();
		CloseableHttpResponse response = null;
		HttpPost httpPost = null;
		HttpEntity resEntity = null;
		try {
			// 把一个普通参数和文件上传给下面这个地址
			httpPost = new HttpPost(url);

			int connTimeout = CONNECTION_TIMEOUT;
			int soTimeOut = SO_TIMEOUT;

			if (StringUtils.isNotBlank(connectTimeout)) {
				connTimeout = Integer.parseInt(connectTimeout);
			}

			if (StringUtils.isNotBlank(socketTimeout)) {
				soTimeOut = Integer.parseInt(socketTimeout);
			}

			// 设置请求和传输超时时间
			RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(soTimeOut).setConnectTimeout(connTimeout).build();
			httpPost.setConfig(requestConfig);

			// 设置header
			if (headers != null && !headers.isEmpty()) {
				for (Map.Entry<String, String> entry : headers.entrySet()) {
					String value = entry.getValue();
					if (value != null) {
						httpPost.addHeader(entry.getKey(),  URLEncoder.encode(value,CHARSET));
					}
				}
			}
			// 把文件转换成流对象FileBody
			File file = new File(localFilePath);
			FileBody bin = new FileBody(file);
			String fileName = localFilePath.substring(localFilePath.lastIndexOf(File.separator) + 1);

			StringBody uploadFileNameBody = new StringBody(fileName, ContentType.create("text/plain", Consts.UTF_8));
			HttpEntity reqEntity = null;
			// 以浏览器兼容模式运行，防止文件名乱码。
			if (uploadFileName == null) {
				reqEntity = MultipartEntityBuilder.create().setMode(HttpMultipartMode.BROWSER_COMPATIBLE).addPart(uploadFile, bin)// uploadFile对应服务端类的同名属性
						.setCharset(CharsetUtils.get("UTF-8")).build();
			} else {
				reqEntity = MultipartEntityBuilder.create().setMode(HttpMultipartMode.BROWSER_COMPATIBLE).addPart(uploadFile, bin)// uploadFile对应服务端类的同名属性
						.addPart(uploadFileName, uploadFileNameBody)// uploadFileName对应服务端类的同名属性
						.setCharset(CharsetUtils.get("UTF-8")).build();
			}
			httpPost.setEntity(reqEntity);

			// 发起请求 并返回请求的响应
			response = httpClient.execute(httpPost);

			// 获取响应对象
			resEntity = response.getEntity();
			if (resEntity != null) {
				// 打印响应内容
				return EntityUtils.toString(resEntity, Charset.forName("UTF-8"));
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}finally {
			// 销毁
			EntityUtils.consume(resEntity);
			if (response != null)
				response.close();
			if (httpPost != null)
				httpPost.releaseConnection();
		}
		return null;

	}

	/**
	 * 上传文件
	 *
	 * @param url
	 *            上传URL
	 * @param localFilePath
	 *            本地文件地址
	 * @param uploadFile
	 *            对应服务端类的同名属性
	 * @param uploadFileName
	 *            对应服务端类的同名属性
	 * @throws ParseException
	 * @throws IOException
	 */
	public static String postFile(String url, String localFilePath, String uploadFile, String uploadFileName) throws ParseException,
			IOException {
		CloseableHttpClient httpClient = getHttpClient();
		CloseableHttpResponse response = null;
		HttpPost httpPost = null;
		HttpEntity resEntity = null;
		try {
			// 把一个普通参数和文件上传给下面这个地址
			httpPost = new HttpPost(url);
			// 把文件转换成流对象FileBody
			File file = new File(localFilePath);
			FileBody bin = new FileBody(file);
			String fileName = localFilePath.substring(localFilePath.lastIndexOf(File.separator) + 1);

			StringBody uploadFileNameBody = new StringBody(fileName, ContentType.create("text/plain", Consts.UTF_8));
			HttpEntity reqEntity = null;
			// 以浏览器兼容模式运行，防止文件名乱码。
			if (uploadFileName == null) {
				reqEntity = MultipartEntityBuilder.create().setMode(HttpMultipartMode.BROWSER_COMPATIBLE).addPart(uploadFile, bin)// uploadFile对应服务端类的同名属性
						.setCharset(CharsetUtils.get("UTF-8")).build();
			} else {
				reqEntity = MultipartEntityBuilder.create().setMode(HttpMultipartMode.BROWSER_COMPATIBLE).addPart(uploadFile, bin)// uploadFile对应服务端类的同名属性
						.addPart(uploadFileName, uploadFileNameBody)// uploadFileName对应服务端类的同名属性
						.setCharset(CharsetUtils.get("UTF-8")).build();
			}
			httpPost.setEntity(reqEntity);

			// 发起请求 并返回请求的响应
			response = httpClient.execute(httpPost);

			// 获取响应对象
			resEntity = response.getEntity();
			if (resEntity != null) {
				// 打印响应内容
				return EntityUtils.toString(resEntity, Charset.forName("UTF-8"));
			}

		}catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
			// 销毁
			EntityUtils.consume(resEntity);
			if (response != null)
				response.close();
			if (httpPost != null)
				httpPost.releaseConnection();
		}
		return null;

	}

	/**
	 * 上传文件
	 *
	 * @param url
	 *            上传URL
	 * @param localFilePath
	 *            本地文件地址
	 * @param uploadFile
	 *            对应服务端类的同名属性
	 * @param uploadFileName
	 *            对应服务端类的同名属性
	 * @param connectTimeout
	 *            连接超时，单位：毫秒，为空默认为10s
	 * @param socketTimeout
	 *            数据传输超时，单位：毫秒，为空默认为100s
	 * @throws ParseException
	 * @throws IOException
	 */
	public static String postFile(String url, String localFilePath, String uploadFile, String uploadFileName, String connectTimeout,
			String socketTimeout) throws ParseException, IOException {
		CloseableHttpClient httpClient = getHttpClient();
		CloseableHttpResponse response = null;
		HttpPost httpPost = null;
		HttpEntity resEntity = null;
		try {
			// 把一个普通参数和文件上传给下面这个地址
			httpPost = new HttpPost(url);

			int connTimeout = CONNECTION_TIMEOUT;
			int soTimeOut = SO_TIMEOUT;

			if (StringUtils.isNotBlank(connectTimeout)) {
				connTimeout = Integer.parseInt(connectTimeout);
			}

			if (StringUtils.isNotBlank(socketTimeout)) {
				soTimeOut = Integer.parseInt(socketTimeout);
			}

			// 设置请求和传输超时时间
			RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(soTimeOut).setConnectTimeout(connTimeout).build();
			httpPost.setConfig(requestConfig);

			// 把文件转换成流对象FileBody
			File file = new File(localFilePath);
			FileBody bin = new FileBody(file);
			String fileName = localFilePath.substring(localFilePath.lastIndexOf(File.separator) + 1);

			StringBody uploadFileNameBody = new StringBody(fileName, ContentType.create("text/plain", Consts.UTF_8));
			HttpEntity reqEntity = null;
			// 以浏览器兼容模式运行，防止文件名乱码。
			if (uploadFileName == null) {
				reqEntity = MultipartEntityBuilder.create().setMode(HttpMultipartMode.BROWSER_COMPATIBLE).addPart(uploadFile, bin)// uploadFile对应服务端类的同名属性
						.setCharset(CharsetUtils.get("UTF-8")).build();
			} else {
				reqEntity = MultipartEntityBuilder.create().setMode(HttpMultipartMode.BROWSER_COMPATIBLE).addPart(uploadFile, bin)// uploadFile对应服务端类的同名属性
						.addPart(uploadFileName, uploadFileNameBody)// uploadFileName对应服务端类的同名属性
						.setCharset(CharsetUtils.get("UTF-8")).build();
			}
			httpPost.setEntity(reqEntity);

			// 发起请求 并返回请求的响应
			response = httpClient.execute(httpPost);

			// 获取响应对象
			resEntity = response.getEntity();
			if (resEntity != null) {
				// 打印响应内容
				return EntityUtils.toString(resEntity, Charset.forName("UTF-8"));
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}finally {
			// 销毁
			EntityUtils.consume(resEntity);
			if (response != null)
				response.close();
			if (httpPost != null)
				httpPost.releaseConnection();
		}
		return null;

	}

	/**
	 * 下载文件
	 *
	 * @param url
	 * @param destFileName
	 *            xxx.jpg/xxx.png/xxx.txt
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static void getFile(String url, String destFileName) throws ClientProtocolException, IOException {
		// 生成一个httpclient对象
		CloseableHttpClient httpclient = getHttpClient();
		HttpGet httpget = null;
		CloseableHttpResponse response = null;
		HttpEntity entity = null;
		InputStream in = null;
		FileOutputStream fout = null;
		try {

			httpget = new HttpGet(url);
			response = httpclient.execute(httpget);
			entity = response.getEntity();
			in = entity.getContent();
			File file = new File(destFileName);

			fout = new FileOutputStream(file);
			int l = -1;
			byte[] tmp = new byte[1024];
			while ((l = in.read(tmp)) != -1) {
				fout.write(tmp, 0, l);
				// 注意这里如果用OutputStream.write(buff)的话，图片会失真，大家可以试试
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}finally {
			// 关闭低层流。
			if (in != null) {
				in.close();
			}
			if (fout != null) {
				fout.flush();
				fout.close();
			}
			if (response != null)
				response.close();
			if (httpget != null)
				httpget.releaseConnection();
		}
	}

	/**
	 * 下载文件
	 *
	 * @param url
	 * @param destFileName
	 *            xxx.jpg/xxx.png/xxx.txt
	 * @param connectTimeout
	 *            连接超时，单位：毫秒，为空默认为10s
	 * @param socketTimeout
	 *            数据传输超时，单位：毫秒，为空默认为100s
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static void getFile(String url, String destFileName, String connectTimeout, String socketTimeout)
			throws ClientProtocolException, IOException {
		// 生成一个httpclient对象
		CloseableHttpClient httpclient = getHttpClient();
		HttpGet httpget = null;
		CloseableHttpResponse response = null;
		HttpEntity entity = null;
		InputStream in = null;
		FileOutputStream fout = null;
		try {

			httpget = new HttpGet(url);

			int connTimeout = CONNECTION_TIMEOUT;
			int soTimeOut = SO_TIMEOUT;

			if (StringUtils.isNotBlank(connectTimeout)) {
				connTimeout = Integer.parseInt(connectTimeout);
			}

			if (StringUtils.isNotBlank(socketTimeout)) {
				soTimeOut = Integer.parseInt(socketTimeout);
			}

			// 设置请求和传输超时时间
			RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(soTimeOut).setConnectTimeout(connTimeout).build();
			httpget.setConfig(requestConfig);

			response = httpclient.execute(httpget);
			entity = response.getEntity();
			in = entity.getContent();
			File file = new File(destFileName);

			fout = new FileOutputStream(file);
			int l = -1;
			byte[] tmp = new byte[1024];
			while ((l = in.read(tmp)) != -1) {
				fout.write(tmp, 0, l);
				// 注意这里如果用OutputStream.write(buff)的话，图片会失真，大家可以试试
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}finally {
			// 关闭低层流。
			if (in != null) {
				in.close();
			}
			if (fout != null) {
				fout.flush();
				fout.close();
			}
			if (response != null)
				response.close();
			if (httpget != null)
				httpget.releaseConnection();
		}

	}

	/**
	 * get 请求(联通能力平台定制)
	 *
	 * 当请求参数鉴权不合法时 平台返回的http-状态码将不是200
	 *
	 * @param url
	 * @param params
	 *            url?号后面的参数
	 * @return
	 * @throws IOException
	 */
	public static String doGetWithRestFulApi(String url, Map<String, String> params) throws IOException {

		if (StringUtils.isBlank(url)) {
			return null;
		}
		HttpGet httpGet = null;

		CloseableHttpResponse response = null;
		try {

			if (params != null && !params.isEmpty()) {
				List<NameValuePair> pairs = new ArrayList<NameValuePair>(params.size());
				for (Map.Entry<String, String> entry : params.entrySet()) {
					String value = entry.getValue();
					if (value != null) {
						pairs.add(new BasicNameValuePair(entry.getKey(), value));
					}
				}
				url += "?" + EntityUtils.toString(new UrlEncodedFormEntity(pairs, CHARSET));
			}
			httpGet = new HttpGet(url);
			httpClient = getHttpClient();
			response = httpClient.execute(httpGet);
			HttpEntity entity = response.getEntity();
			String result = null;
			if (entity != null) {
				result = EntityUtils.toString(entity, CHARSET);
			}
			EntityUtils.consume(entity);
			response.close();

			// int respCode = response.getStatusLine().getStatusCode();
			// if (respCode == HttpStatus.SC_OK) {
			// return result;
			//
			// } else {
			// result = "ERROR: HttpStatus => " + String.valueOf(respCode);
			// return result;
			// }
			return result;
		} catch (Exception e) {
			/* e.printStackTrace(); */
			if (httpGet != null)
				httpGet.abort();
			throw new RuntimeException(e);
		} finally {
			if (response != null)
				response.close();
			if (httpGet != null)
				httpGet.releaseConnection();
		}
	}

	/**
	 * HTTP Post 获取内容(联通能力平台定制)
	 *
	 * @param url
	 *            请求的url地址 ?之前的地址
	 * @param params1
	 *            需要通过URL传的参数
	 * @param params2
	 *            请求的参数
	 * @return 页面内容
	 */
	public static String doPostWithRestFulApi(String url, Map<String, String> params1, Map<String, String> params2)
			throws ClientProtocolException, IOException {
		if (StringUtils.isBlank(url)) {
			return null;
		}
		HttpPost httpPost = null;
		CloseableHttpResponse response = null;
		try {
			List<NameValuePair> pairs1 = null;
			if (params1 != null && !params1.isEmpty()) {
				pairs1 = new ArrayList<NameValuePair>(params1.size());
				for (Map.Entry<String, String> entry : params1.entrySet()) {
					String value = entry.getValue();
					if (value != null) {
						pairs1.add(new BasicNameValuePair(entry.getKey(), value));
					}
				}
				url += "?" + EntityUtils.toString(new UrlEncodedFormEntity(pairs1, CHARSET));
			}

			List<NameValuePair> pairs2 = null;
			if (params2 != null && !params2.isEmpty()) {
				pairs2 = new ArrayList<NameValuePair>(params2.size());
				for (Map.Entry<String, String> entry : params2.entrySet()) {
					String value = entry.getValue();
					if (value != null) {
						pairs2.add(new BasicNameValuePair(entry.getKey(), value));
					}
				}
			}

			httpPost = new HttpPost(url);
			if (pairs2 != null && pairs2.size() > 0) {
				httpPost.setEntity(new UrlEncodedFormEntity(pairs2, CHARSET));
			}
			httpClient = getHttpClient();
			response = httpClient.execute(httpPost);

			HttpEntity entity = response.getEntity();
			String result = null;
			if (entity != null) {
				result = EntityUtils.toString(entity, CHARSET);
			}
			EntityUtils.consume(entity);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
			if (response != null)
				response.close();
			if (httpPost != null)
				httpPost.releaseConnection();
		}
	}

	/**
	 * get 请求RestFul API
	 *
	 * 当请求参数鉴权不合法时 平台返回的http-状态码将不是200
	 *
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public static String doGetWithRestFulApi(String url) throws IOException {
		if (StringUtils.isBlank(url)) {
			return null;
		}
		HttpGet httpGet = new HttpGet(url);
		return doGetWithRestFulApi(httpGet);
	}

	/**
	 * get 请求RestFul API
	 *
	 * 当请求参数鉴权不合法时 平台返回的http-状态码将不是200
	 *
	 * @return
	 * @throws IOException
	 */
	public static String doGetWithRestFulApi(HttpGet httpGet) throws IOException {

		if (httpGet == null) {
			return null;
		}
		CloseableHttpResponse response = null;
		try {

			httpClient = getHttpClient();
			response = httpClient.execute(httpGet);
			HttpEntity entity = response.getEntity();
			String result = null;
			if (entity != null) {
				result = EntityUtils.toString(entity, CHARSET);
			}
			EntityUtils.consume(entity);
			response.close();

			// int respCode = response.getStatusLine().getStatusCode();
			// if (respCode == HttpStatus.SC_OK) {
			// return result;
			//
			// } else {
			// result = "ERROR: HttpStatus => " + String.valueOf(respCode);
			// return result;
			// }
			return result;
		} catch (Exception e) {
			/* e.printStackTrace(); */
			if (httpGet != null)
				httpGet.abort();
			throw new RuntimeException(e);
		} finally {
			if (response != null)
				response.close();
			if (httpGet != null)
				httpGet.releaseConnection();
		}
	}

	/**
	 *
	 * post 请求RestFul API
	 *
	 * 当请求参数鉴权不合法时 平台返回的http-状态码将不是200
	 *
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static String doPostWithRestFulApi(HttpPost httpPost) throws ClientProtocolException, IOException {

		if (httpPost == null) {
			return null;
		}

		CloseableHttpResponse response = null;

		try {

			httpClient = getHttpClient();
			response = httpClient.execute(httpPost);
			HttpEntity entity = response.getEntity();
			String result = null;
			if (entity != null) {
				result = EntityUtils.toString(entity, CHARSET);
			}
			EntityUtils.consume(entity);

			int respCode = response.getStatusLine().getStatusCode();
			// if (respCode == HttpStatus.SC_OK) {
			// return result;
			//
			// } else {
			// result = "ERROR: HttpStatus => " + String.valueOf(respCode);
			// return result;
			// }
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			if (httpPost != null)
				httpPost.abort();
			throw new RuntimeException(e);
		} finally {
			if (response != null)
				response.close();
			if (httpPost != null)
				httpPost.releaseConnection();
		}
	}

	public static void main(String[] args) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("telno", "18756091526");
		String getData = doGet("http://192.168.85.109:10808/", params);
		System.out.println(getData);
		System.out.println("----------------------分割线-----------------------");

		/*
		 * String postData = doPost("http://www.oschina.net/",null);
		 * System.out.println(postData);
		 */
	}
}