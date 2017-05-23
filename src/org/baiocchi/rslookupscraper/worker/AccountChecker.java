package org.baiocchi.rslookupscraper.worker;

import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.Callable;
import java.util.logging.Level;

import org.apache.commons.logging.LogFactory;
import org.baiocchi.rslookupscraper.Constants;
import org.baiocchi.rslookupscraper.Data;
import org.baiocchi.rslookupscraper.sleep.ConditionalSleep;

import com.gargoylesoftware.htmlunit.AjaxController;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.ScriptResult;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableCell;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gargoylesoftware.htmlunit.util.Cookie;

public class AccountChecker extends Worker {
	private final WebClient client;
	private HtmlPage currentPage;

	public AccountChecker(int id) {
		super(id);
		this.client = new WebClient(BrowserVersion.BEST_SUPPORTED);
		setWebClientSettings(client);
		try {
			currentPage = client.getPage(Constants.SEARCH_URL);
		} catch (FailingHttpStatusCodeException | IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		switch (currentPage.getUrl().toExternalForm()) {
		case Constants.LOGIN_URL:
			log("Handling login...");
			final HtmlForm form = currentPage.getFirstByXPath("//form[@action='https://rslookup.com/login']");
			final HtmlTextInput usernameField = form.getInputByName("username");
			usernameField.setValueAttribute(Constants.USERNAME);
			final HtmlInput passwordField = form.getInputByName("password");
			passwordField.setValueAttribute(Constants.PASSWORD);
			final HtmlButton loginButton = (HtmlButton) form.getElementsByTagName("button").get(0);
			try {
				currentPage = loginButton.click();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			ConditionalSleep.sleep(new Callable<Boolean>() {

				@Override
				public Boolean call() throws Exception {
					return currentPage.getTitleText().toLowerCase().contains("search");
				}

			}, 50, 20);
			log("Login handled...");
			break;
		case Constants.TERMS_URL:
			log("Handling terms...");
			break;
		case Constants.SEARCH_URL:
			final HtmlTextInput searchField = currentPage.getElementByName("query");
			final HtmlButton searchButton = (HtmlButton) currentPage.getElementById("search");
			final HtmlDivision resultsDivision = (HtmlDivision) currentPage.getFirstByXPath("//div[@id='results']");
			String username = "pablo4";
			/*
			 * try { username = Engine.getInstance().getUsernamesQueue().take();
			 * } catch (InterruptedException e1) { e1.printStackTrace(); }
			 */
			log("Searching: " + username);
			searchField.setValueAttribute(username);
			try {
				currentPage = searchButton.click();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			ConditionalSleep.sleep(new Callable<Boolean>() {

				@Override
				public Boolean call() throws Exception {
					return resultsDivision.getTextContent().length() > 5;
				}

			}, 50, 50);
			if (resultsDivision.getTextContent().toLowerCase().contains("there was no results for")) {
				log("No results for: " + username);
			} else {
				log("Scraping results for: " + username + "...");
				for (int cycles = 1; cycles < 3; cycles++) {
					HtmlTable resultsTable = currentPage.getFirstByXPath("//table[@class='table table-bordered']");
					ScriptResult scriptResult = null;
					for (int index = 1; index < resultsTable.getRowCount(); index++) {
						Data data = null;
						for (final HtmlTableCell cell : resultsTable.getRow(index).getCells()) {
							if (cycles == 1) {
								if (cell.asText().toLowerCase().contains("search in hash")
										|| cell.asText().toLowerCase().contains("plain password")) {
									scriptResult = currentPage.executeJavaScript(cell.getOnClickAttribute());
								}
							} else {
								log(cell.asText());
							}
						}

					}
					if (cycles == 1) {
						currentPage = (HtmlPage) scriptResult.getNewPage();
					}
				}
				log("Results scraped.");
			}
			break;
		default:
			try {
				currentPage = client.getPage(Constants.SEARCH_URL);
			} catch (FailingHttpStatusCodeException | IOException e) {
				e.printStackTrace();
			}
			break;
		}
		run();
	}

	private void setWebClientSettings(WebClient webClient) {
		CookieManager cookieManager = new CookieManager();
		cookieManager.setCookiesEnabled(true);
		webClient.setCookieManager(cookieManager);
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.HOUR, 1);
		Cookie cookie = new Cookie("rslookup.com", "tos", "true", "/", cal.getTime(), false);
		cookieManager.addCookie(cookie);
		webClient.setAjaxController(new NicelyResynchronizingAjaxController());
		webClient.setAjaxController(new AjaxController());
		webClient.waitForBackgroundJavaScript(2000);
		webClient.waitForBackgroundJavaScriptStartingBefore(2000);
		webClient.getOptions().setPrintContentOnFailingStatusCode(false);
		webClient.getOptions().setCssEnabled(false);
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		webClient.getOptions().setJavaScriptEnabled(true);
		webClient.getOptions().setDownloadImages(false);
		webClient.getCache().clear();
		webClient.getOptions().isDoNotTrackEnabled();
		webClient.getOptions().setUseInsecureSSL(true);
		LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log",
				"org.apache.commons.logging.impl.NoOpLog");
		java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
		java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);
		java.util.logging.Logger.getLogger("org.apache").setLevel(Level.OFF);
		java.util.logging.Logger.getLogger("org.apache.http.client.protocol.ResponseProcessCookies")
				.setLevel(Level.OFF);

	}
}
