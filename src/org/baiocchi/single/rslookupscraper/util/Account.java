package org.baiocchi.single.rslookupscraper.util;

public class Account {
	private final int rank;
	private final String username;
	private final String server;
	private final String donorStatus;

	public Account(int rank, String username, String donorStatus, String server) {
		this.rank = rank;
		this.username = username;
		this.donorStatus = donorStatus;
		this.server = server;
	}

	public String getDonorStatus() {
		return donorStatus;
	}

	public int getRank() {
		return rank;
	}

	public String getUsername() {
		return username;
	}

	public String getServer() {
		return server;
	}

	@Override
	public String toString() {
		return rank + "::" + username + "::" + donorStatus + "::" + server;
	}

}
