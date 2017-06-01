package org.baiocchi.rslookupscraper.singlesearch.worker;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import org.baiocchi.rslookupscraper.singlesearch.util.Data;

public class DataSaver extends Worker {

	private LinkedBlockingQueue<Data> data;
	private final String saveDirectory;
	private boolean running;

	public DataSaver(int id, File file) {
		super(id);
		this.saveDirectory = file.getAbsolutePath();
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
		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(saveDirectory + "/RSLookUp-Dump.csv", true), StandardCharsets.UTF_8));) {
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
			log("Saved " + saveData.getAccount().getUsername() + " data to file!");
		}
	}

}
