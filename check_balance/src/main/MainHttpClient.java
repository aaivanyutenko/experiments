package main;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class MainHttpClient {

	public static void main(String[] args) {
		try {
			CloseableHttpClient httpClient = HttpClients.createDefault();
			HttpGet httpGet = new HttpGet("https://ibank.belinvestbank.by");
			CloseableHttpResponse response1 = httpClient.execute(httpGet);
			
			try {
			    System.out.println(response1.getStatusLine());
			    HttpEntity entity1 = response1.getEntity();
			    EntityUtils.consume(entity1);
			} finally {
			    response1.close();
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
