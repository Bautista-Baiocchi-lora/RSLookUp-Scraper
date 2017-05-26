package org.baiocchi.rslookupscraper.worker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.logging.LogFactory;
import org.baiocchi.rslookupscraper.Engine;
import org.baiocchi.rslookupscraper.util.Account;
import org.baiocchi.rslookupscraper.util.Constants;
import org.baiocchi.rslookupscraper.util.Data;

import com.gargoylesoftware.htmlunit.AjaxController;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.ScriptResult;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSpan;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableCell;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gargoylesoftware.htmlunit.javascript.background.JavaScriptJobManager;
import com.gargoylesoftware.htmlunit.util.Cookie;

public class AccountChecker extends Worker {
	private WebClient client;
	private HtmlPage currentPage;
	private boolean handlingJavascript = false;
	private Account account = null;
	private boolean running;
	private int noResultCount = 0;
	private int emptyRowCount = 0;

	public AccountChecker(int id) {
		super(id);
		this.client = getNewClient();
		running = true;
	}

	@Override
	public void run() {
		while (running) {
			switch (currentPage.getUrl().toExternalForm()) {
			case Constants.LOGIN_URL:
				log("Handling login...");
				final HtmlForm form = currentPage.getFirstByXPath("//form[@action='https://rslookup.com/login']");
				if (form != null) {
					final HtmlTextInput usernameField = form.getInputByName("username");
					usernameField.setValueAttribute(Constants.USERNAME);
					final HtmlInput passwordField = form.getInputByName("password");
					passwordField.setValueAttribute(Constants.PASSWORD);
					final HtmlButton loginButton = (HtmlButton) form.getElementsByTagName("button").get(0);
					if (loginButton != null && loginButton.isDisplayed()) {
						try {
							currentPage = loginButton.click();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						waitForJavascriptToExecute();
					}
					log("Login handled!");
				}
				break;
			case Constants.SEARCH_URL:
				if (!handlingJavascript) {
					final HtmlTextInput searchField = currentPage.getElementByName("query");
					final HtmlButton searchButton = (HtmlButton) currentPage.getElementById("search");
					try {
						account = Engine.getInstance().getAccounts().take();
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					if (searchField != null && searchField.isDisplayed()) {
						log("Searching: " + account.getUsername() + "...");
						searchField.setValueAttribute(account.getUsername());
						try {
							currentPage = searchButton.click();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						waitForJavascriptToExecute();
						handlingJavascript = true;
					}
				}
				if (currentPage != null) {
					if (currentPage.asText().toLowerCase().contains("there was no results for")
							|| currentPage.asText().toLowerCase().contains("enter a minimum")) {
						log("No results for: " + account.getUsername());
						handlingJavascript = false;
						noResultCount++;
						if (noResultCount >= 8) {
							log("Client may have nulled. Restarting Client...");
							client = getNewClient();
							noResultCount = 0;
						}
						continue;
					} else {
						if (!currentPage.asText().toLowerCase().contains("search in hash")) {
							log("Scraping results for: " + account.getUsername());
							final HtmlTable results = currentPage.getFirstByXPath("//table[1]");
							if (results != null && results.isDisplayed() && results.getRowCount() > 1) {
								final ArrayList<Data> dataList = new ArrayList<Data>();
								for (final HtmlTableRow row : results.getRows()) {
									if (emptyRowCount >= 6) {
										break;
									}
									if (row.getIndex() > 1) {
										final Data data = new Data(account, super.getID());
										boolean emptyRow = true;
										for (final HtmlTableCell cell : row.getCells()) {
											switch (cell.getIndex()) {
											case 1:
												if (cell.asText().length() > 0) {
													data.setDatabase(cell.asText());
													emptyRow = false;
												}
												break;
											case 5:
												if (cell.asText().length() > 0) {
													data.setEmail(cell.asText());
													emptyRow = false;
												}
												break;
											case 7:
												if (cell.asText().length() > 0) {
													data.setPassword(
															cell.asText().replaceAll("Plain Password - Reveal", ""));
													emptyRow = false;
												}
												break;
											case 9:
												if (cell.asText().length() > 0) {
													data.setIP(cell.asText());
													emptyRow = false;
												}
												break;
											}
										}
										if (emptyRow) {
											emptyRowCount++;
										} else {
											dataList.add(data);
										}
									}
								}
								if (emptyRowCount >= 6) {
									log("Empty row failsafe triggered. Restarting Client...");
									emptyRowCount = 0;
									client = getNewClient();
									break;
								}
								Engine.getInstance().processData(dataList);
								handlingJavascript = false;
								log("Results for " + account.getUsername() + " scraped!");
							} else {
								log("Table is bugged. Restarting Client...");
								client = getNewClient();
								break;
							}
						} else {
							final List<HtmlSpan> greenTexts = currentPage
									.getByXPath("//table[@class='table table-bordered']/tbody/tr/td/span");
							if (greenTexts != null && greenTexts.size() > 0) {
								log("Handling javascript...");
								for (final HtmlSpan greenText : greenTexts) {
									if (greenText.asText().equalsIgnoreCase("Search in hash DB")) {
										ScriptResult result = currentPage
												.executeJavaScript(greenText.getOnClickAttribute());
										currentPage = (HtmlPage) result.getNewPage();
									}
								}
								waitForJavascriptToExecute();
							}
						}
					}
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
		}
	}

	private void waitForJavascriptToExecute() {
		JavaScriptJobManager manager = currentPage.getEnclosingWindow().getJobManager();
		while (manager.getJobCount() > 0) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private WebClient getNewClient() {
		WebClient client = new WebClient(BrowserVersion.CHROME);
		setWebClientSettings(client);
		try {
			currentPage = client.getPage(Constants.SEARCH_URL);
		} catch (FailingHttpStatusCodeException | IOException e) {
			e.printStackTrace();
		}
		return client;
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
		webClient.waitForBackgroundJavaScript(1000);
		webClient.getOptions().setGeolocationEnabled(false);
		webClient.getOptions().setRedirectEnabled(true);
		webClient.getOptions().setAppletEnabled(false);
		webClient.getOptions().setPrintContentOnFailingStatusCode(false);
		webClient.getOptions().setCssEnabled(false);
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		webClient.getOptions().setJavaScriptEnabled(true);
		webClient.getOptions().setDownloadImages(false);
		webClient.getCache().clear();
		webClient.setJavaScriptTimeout(30000);
		webClient.getOptions().setPopupBlockerEnabled(true);
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
