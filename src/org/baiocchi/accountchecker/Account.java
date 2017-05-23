package org.baiocchi.accountchecker;

public class Account {

	private final String website;
	private final String username;
	private final String password;
	private final String email;
	private final String IP;

	public Account(String website, String username, String email, String password, String IP) {
		this.website = website;
		this.username = username;
		this.email = email;
		this.password = password;
		this.IP = IP;
	}

	public String getWebsite() {
		return website;
	}

	public String getUsername() {
		return username;
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

	@Override
	public String toString() {
		return website + "," + username + "," + email + "," + password + "," + IP;
	}

}
