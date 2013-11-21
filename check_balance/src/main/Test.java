package main;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		Pattern MY_PATTERN = Pattern.compile("/captcha/?v=(\\d+)");
		Pattern MY_PATTERN = Pattern.compile("/captcha/\\?v=(\\d+)");
		Matcher m = MY_PATTERN.matcher("<img src=\"/captcha/?v=1385033469\" class=\"captcha\" id=\"captcha\" alt=\"\" />");
		if (m.find()) {
		    String s = m.group(1);
		    System.out.println(s);
		    // s now contains "BAR"
		}
	}

}
