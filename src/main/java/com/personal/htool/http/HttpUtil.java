package com.personal.htool.http;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class HttpUtil {

    private static final String APPLICATION_JSON = "application/json";

    private static final String CONTENT_TYPE_TEXT_JSON = "text/json";

    private static final String CONTENT_ENCODING = "UTF-8";

    private static final PoolingHttpClientConnectionManager connManager
            = new PoolingHttpClientConnectionManager();

    /**
     * Http客户端
     */
    private static CloseableHttpClient client = null;

    /**
     * 发送post http请求
     */
    public static String sendDataByPost(String url,String jsonData){
        CloseableHttpClient client = HttpClients.createDefault();//todo 优化连接器 20160907
        HttpPost post = new HttpPost(url);
        StringEntity myEntity = new StringEntity(jsonData, ContentType.APPLICATION_JSON);// 构造请求数据
        post.setEntity(myEntity);// 设置请求体
        String responseContent = null; // 响应内容
        CloseableHttpResponse response = null;
        try {
            response = client.execute(post);
            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                responseContent = EntityUtils.toString(entity, "UTF-8");
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (response != null)
                    response.close();

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (client != null)
                        client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return responseContent;
    }

}
