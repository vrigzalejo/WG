package com.ciaramcelmer.wfactory;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.ciaramcelmer.wfactory.SavedGame.SavedGameState;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

public class SavedGameListAdapter extends BaseAdapter {

	public static final String SAVEDGAME_DIR = "/data/data/com.ciaramcelmer.wfactory/";
	public ArrayList<String> mGameFiles;
	private LayoutInflater inflater;
	private SavedGameList mContext;
	//private TargetGridView targetG;
	private Typeface mFace;

	public SavedGameListAdapter(SavedGameList context) {
		this.inflater = LayoutInflater.from(context);
		this.mContext = context;
		this.mGameFiles = new ArrayList<String>();
		this.mFace = Typeface.createFromAsset(context.getAssets(),
				"fonts/font.ttf");
		this.refreshFiles();
	}

	public void refreshFiles() {
		// TODO Auto-generated method stub
		this.mGameFiles.clear();
		File dir = new File(SAVEDGAME_DIR);
		String[] allFiles = dir.list();
		for (String entryName : allFiles)
			if (entryName.startsWith("savedgame_"))
				this.mGameFiles.add(entryName);
	}

	// @Override
	public int getCount() {
		return this.mGameFiles.size() + 1;
	}

	// @Override
	public Object getItem(int arg0) {
		if (arg0 == 0)
			return "";
		return this.mGameFiles.get(arg0 - 1);
	}

	// @Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	// @Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		if (position == 0) {
			convertView = inflater.inflate(R.layout.savedgamesaveitem, null);
			final Button saveCurrent = (Button) convertView
					.findViewById(R.id.saveCurrent);

	/*		
	 * 		if (targetG.gameActive == true) {
				saveCurrent.setVisibility(View.VISIBLE);
				
			} else {
				saveCurrent.setVisibility(View.INVISIBLE);
			}
	*
	*/
			saveCurrent.setOnClickListener(new OnClickListener() {

				// @Override
				public void onClick(View v) {
					saveCurrent.setEnabled(false);
					mContext.saveCurrent();
				}
			});
			if (mContext.mCurrentSaved)
				saveCurrent.setEnabled(false);
			return convertView;
		}

		convertView = inflater.inflate(R.layout.savedgameitem, null);
		TargetGridView grid = (TargetGridView) convertView
				.findViewById(R.id.savedGridView);

		final String saveFile = SAVEDGAME_DIR + "/"
				+ this.mGameFiles.get(position - 1);

		grid.mContext = this.mContext;

		SavedGame saver = new SavedGame();
		try {
			TextView wordCounts = (TextView) convertView
					.findViewById(R.id.savedWordCounts);
			SavedGameState sgs = saver.RestoreGrid(saveFile);
			grid.setLetters(sgs.currentShuffled);
			wordCounts.setText("Word Factory: " + sgs.validWords + "\n\nWords: "
					+ sgs.playerWords);
		} catch (Exception e) {
			// Error, delete the file
			new File(saveFile).delete();
			return convertView;
		}

		grid.setBackgroundColor(0x00FFFFFF);

		Button loadButton = (Button) convertView.findViewById(R.id.gameLoad);
		loadButton.setOnClickListener(new OnClickListener() {

			// @Override
			public void onClick(View v) {
				mContext.LoadGame(saveFile);

			}
		});

		Button deleteButton = (Button) convertView
				.findViewById(R.id.gameDelete);
		deleteButton.setOnClickListener(new OnClickListener() {

			// @Override
			public void onClick(View v) {
				mContext.DeleteGame(saveFile);

			}
		});
		return convertView;
	}

}
