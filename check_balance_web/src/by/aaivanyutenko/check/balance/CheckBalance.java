package by.aaivanyutenko.check.balance;

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

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class CheckBalance
 */
@WebServlet("/check")
public class CheckBalance extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public CheckBalance() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		String webContentPath = getServletContext().getRealPath("/");
		System.setProperty("phantomjs.binary.path", new StringBuilder(webContentPath).append("phantomjs").toString());
		WebDriver browser = new PhantomJSDriver();
		browser.get("https://ibank.belinvestbank.by/signin");
		new WebDriverWait(browser, 10).until(ExpectedConditions.presenceOfElementLocated(By.id("captcha")));
		WebElement captchaElement = browser.findElement(By.id("captcha"));
		System.out.println(captchaElement.getLocation());
		TakesScreenshot takesScreenshot = (TakesScreenshot) browser;
		File captchaFile = new File(new StringBuilder(webContentPath).append("captcha.png").toString());
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

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
