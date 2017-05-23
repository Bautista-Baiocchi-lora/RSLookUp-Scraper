package org.baiocchi.rslookupscraper.worker;

public abstract class Worker implements Runnable {
	private final int id;

	public Worker(int id) {
		this.id = id;
	}

	public int getID() {
		return id;
	}

	protected void log(String message) {
		System.out.println("[WORKER: " + id + "] " + message);
	}

}
