package main;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

public class Main {
	private static final String PASSWORD = "yK9m23sd";
	private static final Map<String, String> HEADERS = new HashMap<String, String>();
	private static final List<String> START_HEADERS = new ArrayList<String>();
	private static final List<String> SIGNIN_HEADERS = new ArrayList<String>();

	static {
		HEADERS.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
		HEADERS.put("Accept-Encoding", "gzip,deflate,sdch");
		HEADERS.put("Accept-Language", "en-GB,en;q=0.8,en-US;q=0.6,ru;q=0.4");
		HEADERS.put("Cache-Control", "max-age=0");
		HEADERS.put("Connection", "keep-alive");
		HEADERS.put("Host", "ibank.belinvestbank.by");
		HEADERS.put("User-Agent", "Mozilla/5.0 (X11; Linux i686) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36");
		HEADERS.put("Content-Type", "application/x-www-form-urlencoded");
		HEADERS.put("Origin", "https://ibank.belinvestbank.by");
		HEADERS.put("Referer", "https://ibank.belinvestbank.by/signin");
		HEADERS.put("", "");

		START_HEADERS.add("Accept");
		START_HEADERS.add("Accept-Encoding");
		START_HEADERS.add("Accept-Language");
		START_HEADERS.add("Cache-Control");
		START_HEADERS.add("Connection");
		START_HEADERS.add("Host");
		START_HEADERS.add("User-Agent");

		SIGNIN_HEADERS.add("Accept");
		SIGNIN_HEADERS.add("Accept-Encoding");
		SIGNIN_HEADERS.add("Accept-Language");
		SIGNIN_HEADERS.add("Cache-Control");
		SIGNIN_HEADERS.add("Connection");
		SIGNIN_HEADERS.add("Host");
		SIGNIN_HEADERS.add("User-Agent");
		SIGNIN_HEADERS.add("Content-Type");
		SIGNIN_HEADERS.add("Origin");
		SIGNIN_HEADERS.add("Referer");
		SIGNIN_HEADERS.add("Cookie");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String start_url = "https://ibank.belinvestbank.by/";
		String signin_url = "URL:https://ibank.belinvestbank.by/signin";
		String signin_url_2 = "URL:https://ibank.belinvestbank.by/signin2";
		URL url;
		try {
			url = new URL(start_url);
			HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
			con.setRequestMethod("GET");
			setHeaders(con, START_HEADERS);
			con.connect();

			HEADERS.put("Cookie", con.getHeaderFields().get("Set-Cookie").get(0).split(";")[0]);
			saveCaptcha(con);
			byte[] b = new byte[100];
			System.in.read(b);
			String captcha = new String(b);

			url = new URL(signin_url);
			con = (HttpsURLConnection) url.openConnection();
			con.setRequestMethod("POST");
			setHeaders(con, SIGNIN_HEADERS);
			StringBuilder urlParameters = new StringBuilder("login=3220389H030PB&keyword=z0q1ec&keystring=");
			urlParameters.append(captcha);
			con.setRequestProperty("Content-Length", Integer.toString(urlParameters.length()));
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(urlParameters.toString());
			wr.flush();
			wr.close();

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			// print result
			System.out.println(response.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void setHeaders(HttpsURLConnection con, List<String> headers) {
		for (String header : headers) {
			con.setRequestProperty(header, HEADERS.get(header));
		}
	}

	public static void saveCaptcha(HttpsURLConnection con) {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			Pattern MY_PATTERN = Pattern.compile("/captcha/\\?v=(\\d+)");
			Matcher m = MY_PATTERN.matcher(response.toString());
			if (m.find()) {
				String s = m.group(1);
				System.out.println(new StringBuilder("https://ibank.belinvestbank.by/captcha/?v=").append(s).toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}