package main;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class SeleniumBalance {

	/**
	 * @param args
	 * @throws MalformedURLException 
	 */
	public static void main(String[] args) {
		System.setProperty("webdriver.chrome.driver", "/opt/chromedriver_selenium/chromedriver");
		System.setProperty("phantomjs.binary.path", "/opt/phantomjs-1.9.2-linux-i686/bin/phantomjs");
		WebDriver browser = new PhantomJSDriver();
		browser.get("https://ibank.belinvestbank.by/signin");
		new WebDriverWait(browser, 10).until(ExpectedConditions.presenceOfElementLocated(By.id("captcha")));
		WebElement captchaElement = browser.findElement(By.id("captcha"));
		System.out.println(captchaElement.getLocation());
		TakesScreenshot takesScreenshot = (TakesScreenshot) browser;
		File captchaFile = new File("captcha.png");
		try {
			FileUtils.copyFile(takesScreenshot.getScreenshotAs(OutputType.FILE), captchaFile);
			BufferedImage image = ImageIO.read(captchaFile);
			BufferedImage captchaPiece = image.getSubimage(captchaElement.getLocation().x + 2, captchaElement.getLocation().y + 1, captchaElement.getSize().width - 2, captchaElement.getSize().height - 2);
			ImageIO.write(captchaPiece, "png", captchaFile);
			JavascriptExecutor javascriptExecutor = (JavascriptExecutor) browser;
			javascriptExecutor.executeScript("document.getElementById('login_id').value = '3220389H030PB'");
			javascriptExecutor.executeScript("document.getElementById('keyword_id').value = 'z0q1ec'");
			System.out.println("Please type the captcha:");
			byte[] b = new byte[100];
			System.in.read(b);
			String captcha = new String(b).trim();
			javascriptExecutor.executeScript(new StringBuilder("document.getElementById('keystr_id').value = '").append(captcha).append("'").toString());
			javascriptExecutor.executeScript("document.getElementById('login_submit').click()");
			new WebDriverWait(browser, 10).until(ExpectedConditions.presenceOfElementLocated(By.id("pass")));
			javascriptExecutor.executeScript("document.getElementById('pass').value = 'yK9m23sd'");
			javascriptExecutor.executeScript("document.getElementById('password_submit').click()");
			new WebDriverWait(browser, 10).until(ExpectedConditions.presenceOfElementLocated(By.className("current-card-block")));
			WebElement div = browser.findElement(By.className("current-card-block"));
			System.out.println(div.getText());
		} catch (WebDriverException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		browser.close();
		browser.quit();
	}

}
