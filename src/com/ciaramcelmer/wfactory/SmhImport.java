package com.ciaramcelmer.wfactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.Message;
import android.util.Log;

public class SmhImport implements Runnable {
	
	// URL for the puzzle
	private static final String SMH_PUZZLE_URL =
		"http://www.smh.com.au/entertainment/puzzles/";
	private static final String SMH_TARGET_URL =
		"/target.html";
	
	// Contains URLS of previous puzzles
	// (Strings are dates, e.g. 2013/07/18")
	public ArrayList<String> pastPuzzleUrls = null;
	// Contains the letters of the current puzzle
	// L-R T-B
	
	public String currentPuzzleLetters = null;
	// Fill this in with a "pastPuzzleUrl" to fetch other than today
	
	public String fetchPuzzleDate = "";

	public void run() {
		GetSmhPuzzle();
		Message msg = Message.obtain();
		msg.what = DictionaryThread.MESSAGE_HAVE_SMH_NINELETTER;
		msg.obj = this.currentPuzzleLetters;
		DictionaryThread.currentInstance.messageHandler.sendMessage(msg);
	}

	private void GetSmhPuzzle() {
		this.pastPuzzleUrls = new ArrayList<String>();
		this.currentPuzzleLetters = new String();
		String pageContent;
		
		pageContent = FetchPage(SMH_PUZZLE_URL + this.fetchPuzzleDate + SMH_TARGET_URL);
	
		Log.d("Word Factory SMH", "Fetched " + pageContent.length() + " bytes ");
		for (String line : pageContent.split("\n")) {
			if (line.contains("size=\"+4\"")) {
				int sizeIndex = line.indexOf("size=\"+4\"");
				this.currentPuzzleLetters = this.currentPuzzleLetters
					.concat(line.substring(sizeIndex + 10, sizeIndex + 11));
			}
			if (line.contains("entertainment/puzzles/") &&
				line.contains("/target.html")) {
				int hrefIndex = line.indexOf("a href=\"/entertainment");
				this.pastPuzzleUrls.add(line.substring(hrefIndex + 31, hrefIndex + 41));
			}
		}
		Log.d("Word Factory SMH", "Todays puzzle: " + this.currentPuzzleLetters);
		for (String pastPuzzle : this.pastPuzzleUrls)
			Log.d("Word Factory SMH", "Past puzzle: " + pastPuzzle);
	}
	
	private String FetchPage(String url) {
		HttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet(String.format(url));
		String pageContent = "";
		HttpResponse response;
		int downloadSize = 0;
		int totalRead = 0;
		int len;
		Message msg;
		try {
			response = client.execute(request);
			StatusLine status = response.getStatusLine();
			Log.d("Word Factory", "Request returned status " + status);
			if (status.getStatusCode() == 200) {
				HttpEntity entity = response.getEntity();
				InputStream instream = entity.getContent();
				InputStreamReader is = new InputStreamReader(instream);
				
				downloadSize = 40000; // Dummy for now, SMH no give content-length
				msg = Message.obtain();
				msg.what = MainActivity.DOWNLOAD_STARTING;
				msg.arg1 = downloadSize;
				MainActivity.currentInstance.progressHandler.sendMessage(msg);
				char buf[] = new char[8192];
				while ((len = is.read(buf)) > 0) {
					totalRead += len;
					msg = Message.obtain();
					msg.what = MainActivity.DOWNLOAD_PROGRESS;
					msg.arg1 = 100 * totalRead / downloadSize;
					MainActivity.currentInstance.progressHandler.sendMessage(msg);
					pageContent += new String(buf).trim();
					for (int i = 0 ; i < 8192 ; i++)
						buf[i] = '\0';
				}
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		msg = Message.obtain();
		msg.what = MainActivity.DOWNLOAD_COMPLETE;
		MainActivity.currentInstance.progressHandler.sendMessage(msg);
		
		return pageContent;
	}
	

}
