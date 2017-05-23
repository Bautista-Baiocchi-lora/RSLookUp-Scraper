package org.baiocchi.rslookupscraper.worker;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.concurrent.LinkedBlockingQueue;

import org.baiocchi.rslookupscraper.Constants;
import org.baiocchi.rslookupscraper.Data;

public class DataSaver extends Worker {

	private final LinkedBlockingQueue<Data> data;
	private Writer writer;

	public DataSaver(int id) {
		super(id);
		data = new LinkedBlockingQueue<Data>();
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(Constants.SAVE_FILE), "utf-8"));
		} catch (UnsupportedEncodingException | FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void processData(Data data) {
		try {
			this.data.put(data);
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
			write(data.take().toString());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		run();
	}

}
