package org.baiocchi.rslookupscraper.worker;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import org.baiocchi.rslookupscraper.Engine;
import org.baiocchi.rslookupscraper.util.Constants;
import org.baiocchi.rslookupscraper.util.Data;

public class DataSaver extends Worker {

	private LinkedBlockingQueue<Data> data;
	private boolean running;

	public DataSaver(int id) {
		super(id);
		data = new LinkedBlockingQueue<Data>();
		running = true;
	}

	public void processData(ArrayList<Data> data) {
		try {
			for (Data d : data) {
				this.data.put(d);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void write(String line) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(Constants.SAVE_FILE, true));) {
			writer.write(line);
			writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		while (running) {
			Data saveData = null;
			try {
				saveData = data.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			write(saveData.toString());
			Engine.getInstance().incrementAccountsChecked();
			log("Saved " + saveData.getAccount().getUsername() + " data to file!");
		}
	}

}
