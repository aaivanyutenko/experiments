package by.aaivanyutenko.check.balance;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

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
	private boolean init = true;
	private String webContentPath;
	private WebDriver browser;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public CheckBalance() {
        super();
    }
    
    private void initCheckBalance() throws IOException {
    	webContentPath = getServletContext().getRealPath("/");
    	String phantomjsPath = new StringBuilder(webContentPath).append("phantomjs").toString();
    	Files.setPosixFilePermissions(Paths.get(phantomjsPath), new HashSet<PosixFilePermission>(Arrays.asList(PosixFilePermission.OWNER_EXECUTE)));
    	System.setProperty("phantomjs.binary.path", phantomjsPath);
    	browser = new PhantomJSDriver();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		try {
			if (init) {
				initCheckBalance();
				init = false;
			}
			browser.get("https://ibank.belinvestbank.by/signin");
			new WebDriverWait(browser, 10).until(ExpectedConditions.presenceOfElementLocated(By.id("captcha")));
			WebElement captchaElement = browser.findElement(By.id("captcha"));
			TakesScreenshot takesScreenshot = (TakesScreenshot) browser;
			File captchaFile = new File(new StringBuilder(webContentPath).append("captcha.png").toString());
			FileUtils.copyFile(takesScreenshot.getScreenshotAs(OutputType.FILE), captchaFile);
			BufferedImage image = ImageIO.read(captchaFile);
			BufferedImage captchaPiece = image.getSubimage(captchaElement.getLocation().x + 2, captchaElement.getLocation().y + 1, captchaElement.getSize().width - 2, captchaElement.getSize().height - 2);
			ImageIO.write(captchaPiece, "png", captchaFile);
			response.addHeader("Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0");
			PrintWriter writer = response.getWriter();
			writer.write("<html><head></head><body>");
			writer.write(new StringBuilder("<img src='captcha.png?foo=").append(UUID.randomUUID().toString()).append("'>").toString());
			writer.write("<form action='check' method='POST'>");
			writer.write("<input name='captcha' type='text'>");
			writer.write("<input name='submit' type='submit'>");
			writer.write("</form>");
			writer.write("</body></html>");
		} catch (WebDriverException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			String captcha = request.getParameter("captcha");
			JavascriptExecutor javascriptExecutor = (JavascriptExecutor) browser;
			javascriptExecutor.executeScript("document.getElementById('login_id').value = '3220389H030PB'");
			javascriptExecutor.executeScript("document.getElementById('keyword_id').value = 'z0q1ec'");
			javascriptExecutor.executeScript(new StringBuilder("document.getElementById('keystr_id').value = '").append(captcha).append("'").toString());
			javascriptExecutor.executeScript("document.getElementById('login_submit').click()");
			new WebDriverWait(browser, 10).until(ExpectedConditions.presenceOfElementLocated(By.id("pass")));
			javascriptExecutor.executeScript("document.getElementById('pass').value = 'yK9m23sd'");
			javascriptExecutor.executeScript("document.getElementById('password_submit').click()");
			new WebDriverWait(browser, 10).until(ExpectedConditions.presenceOfElementLocated(By.className("current-card-block")));
			WebElement div = browser.findElement(By.className("current-card-block"));
			response.setCharacterEncoding("UTF-8");
			response.addHeader("Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0");
			PrintWriter writer = response.getWriter();
			writer.write("<html><head><meta charset='utf-8'></head><body>");
			String balance = div.getText();
			String utf8balance = new String(balance.getBytes("UTF-8"));
			writer.write(utf8balance);
			writer.write("</body></html>");
		} catch (WebDriverException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		browser.close();
		browser.quit();
		init = true;
	}

}
