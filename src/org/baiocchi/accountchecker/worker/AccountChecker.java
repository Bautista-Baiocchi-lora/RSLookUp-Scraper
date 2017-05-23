package org.baiocchi.accountchecker.worker;

import java.util.List;
import java.util.concurrent.Callable;

import org.baiocchi.accountchecker.Constants;
import org.baiocchi.accountchecker.Engine;
import org.baiocchi.accountchecker.sleep.ConditionalSleep;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import com.machinepublishers.jbrowserdriver.JBrowserDriver;
import com.machinepublishers.jbrowserdriver.Settings;

public class AccountChecker extends Worker {
	private final JBrowserDriver browser;

	public AccountChecker(int id) {
		super(id);
		this.browser = new JBrowserDriver(Settings.builder().headless(false).cache(true).build());
		browser.get(Constants.SEARCH_URL);
	}

	@Override
	public void run() {
		switch (browser.getCurrentUrl()) {
		case Constants.LOGIN_URL:
			log("Handling login page...");
			WebElement usernameField = browser.findElement(By.xpath(Constants.USERNAME_FIELD_XPATH));
			if (isElementInteractable(usernameField)) {
				usernameField.sendKeys(Constants.USERNAME);
				WebElement passwordField = browser.findElement(By.xpath(Constants.PASSWORD_FIELD_XPATH));
				if (isElementInteractable(passwordField)) {
					passwordField.sendKeys(Constants.PASSWORD);
					passwordField.submit();
					ConditionalSleep.sleep(new Callable<Boolean>() {

						@Override
						public Boolean call() throws Exception {
							return browser.getCurrentUrl().equalsIgnoreCase(Constants.SEARCH_URL);
						}

					}, 300, 20);
					log("Login in page handled...");
					break;
				}
			}
			log("Failed to handle login page...");
			break;
		case Constants.SEARCH_URL:
			log("Handling search...");
			String username = null;
			try {
				username = Engine.getInstance().getUsernamesQueue().take();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (username != null) {
				log("Searching: " + username + "...");
				WebElement searchField = browser.findElement(By.xpath(Constants.SEARCH_FIELD_XPATH));
				if (isElementInteractable(searchField)) {
					searchField.sendKeys(username + Keys.ENTER);
					ConditionalSleep.sleep(new Callable<Boolean>() {

						@Override
						public Boolean call() throws Exception {
							return existsElement(Constants.SEARCH_RESULTS_TABLE_XPATH);
						}

					}, 500, 10);
					if (existsElement(Constants.SEARCH_RESULTS_TABLE_XPATH)) {
						List<WebElement> passwordFields = browser.findElementsByXPath("//div[@style='color: green']");
						for (WebElement element : passwordFields) {
							element.click();
						}
					} else {
						log("No results found for: " + username + "...");
						break;
					}
				}
			}
			break;
		case Constants.TERMS_URL:
			log("Handling terms page...");
			WebElement agreeButton = browser.findElement(By.xpath(Constants.I_AGREE_XPATH));
			if (isElementInteractable(agreeButton)) {
				agreeButton.click();
				ConditionalSleep.sleep(new Callable<Boolean>() {

					@Override
					public Boolean call() throws Exception {
						return !browser.getCurrentUrl().equalsIgnoreCase(Constants.TERMS_URL);
					}

				}, 50, 20);
			}
			log("Terms page handled....");
			break;
		default:
			log("Lost! Reseting...");
			browser.get(Constants.SEARCH_URL);
			ConditionalSleep.sleep(new Callable<Boolean>() {

				@Override
				public Boolean call() throws Exception {
					return browser.getCurrentUrl().equalsIgnoreCase(Constants.SEARCH_URL);
				}

			}, 100, 20);
			break;
		}
		//run();
	}

	private boolean isElementInteractable(WebElement element) {
		return element.isDisplayed() && element.isEnabled();
	}

	private void log(String message) {
		System.out.println("[WORKER: " + super.getID() + "] " + message);
	}

	private boolean existsElement(String xpath) {
		try {
			browser.findElement(By.xpath(xpath));
		} catch (NoSuchElementException e) {
			return false;
		}
		return true;
	}
}
