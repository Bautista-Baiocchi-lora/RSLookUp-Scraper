package org.baiocchi.accountchecker.worker;

public abstract class Worker implements Runnable {
	private final int id;

	public Worker(int id) {
		this.id = id;
	}

	public int getID() {
		return id;
	}

}
