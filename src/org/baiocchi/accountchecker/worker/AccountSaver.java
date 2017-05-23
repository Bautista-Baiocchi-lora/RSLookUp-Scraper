package org.baiocchi.accountchecker.worker;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.concurrent.LinkedBlockingQueue;

import org.baiocchi.accountchecker.Account;
import org.baiocchi.accountchecker.Constants;

public class AccountSaver extends Worker {

	private final LinkedBlockingQueue<Account> accounts;
	private Writer writer;

	public AccountSaver(int id) {
		super(id);
		accounts = new LinkedBlockingQueue<Account>();
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(Constants.SAVE_FILE), "utf-8"));
		} catch (UnsupportedEncodingException | FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void processAccount(Account account) {
		try {
			accounts.put(account);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void write(String line) {
		try {
			writer.write(line + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		try {
			write(accounts.take().toString());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		run();
	}

}
