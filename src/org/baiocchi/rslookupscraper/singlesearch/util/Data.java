package org.baiocchi.rslookupscraper.singlesearch.util;

public class Data {

	private String database;
	private String password = "";
	private final Account account;
	private String email = "";
	private String IP = "";

	public Data(Account account) {
		this.account = account;
	}

	public String getDatabase() {
		return database;
	}

	public Account getAccount() {
		return account;
	}

	public String getPassword() {
		return password;
	}

	public String getEmail() {
		return email;
	}

	public String getIP() {
		return IP;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setIP(String IP) {
		this.IP = IP;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@Override
	public String toString() {
		return account.toString() + "," + database + "," + email + "," + password + "," + IP;
	}

}
