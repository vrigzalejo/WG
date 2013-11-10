package com.ciaramcelmer.wfactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioGroup;
import android.widget.TextView;

public class NewGameActivity extends Activity implements OnClickListener{

	// Area for new game widgets
	// private CheckBox newGameFromSMH;
	// private TextView newGameSMHLabel;
	private RadioGroup newGameWordCount;
	//private CheckBox newGameTimed;
	//private TextView newGameTimedLabel;
	private Button newGameStart;
	private Button newGameHelp;
	private Button newGameOptions;
	private Button newGameExit;
	private TextView newGameDictLabel;
	
	// Modified by me 10-11-2013
	MainActivity mActivity;
	
	public SharedPreferences preferences;
	public SharedPreferences.Editor prefeditor = null;
	

	// Called when the activity is first created
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// For now, a specific layout (fullscreen portrait)
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.newgame);

		this.newGameWordCount = (RadioGroup) findViewById(R.id.newGameWordCount);
		//this.newGameTimed = (CheckBox) findViewById(R.id.newGameTimed);
		//this.newGameTimedLabel = (TextView) findViewById(R.id.newGameTimedLabel);
		this.newGameStart = (Button) findViewById(R.id.newGameStart);
		this.newGameHelp = (Button) findViewById(R.id.newGameHelp);
		this.newGameOptions = (Button) findViewById(R.id.newOptions);
		this.newGameExit = (Button) findViewById(R.id.newExit);
		this.newGameDictLabel = (TextView) findViewById(R.id.newGameDictLabel);

		this.preferences = PreferenceManager.getDefaultSharedPreferences(this);
		this.prefeditor = this.preferences.edit();

		this.newGameWordCount
				.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						// TODO Auto-generated method stub

					}
				});
		this.newGameStart.setOnClickListener(this);
		
		this.newGameOptions.setOnClickListener(this);
		
		this.newGameHelp.setOnClickListener(this);
		
		this.newGameExit.setOnClickListener(this);
		
		/*
		this.newGameTimed.setChecked(this.preferences.getBoolean("timed_game",
				true));
		this.newGameTimedLabel.setOnClickListener(new OnClickListener() {

			// @Override
			public void onClick(View v) {
				newGameTimed.setChecked(!newGameTimed.isChecked());

			}
		});
		*/
		
		Message message = Message.obtain();
		message.what = DictionaryThread.MESSAGE_REREAD_DICTIONARY;
		DictionaryThread.currentInstance.messageHandler.sendMessage(message);
		newVersionCheck();
	}


	
	// @Override
	public void onResume() {
		String dictionary = PreferenceManager.getDefaultSharedPreferences(this)
				.getString("dictpref", "2");
		if (dictionary.equals("0") || dictionary.equals("2")) {
			this.newGameDictLabel
					.setText("AMERICAN words. (Change in options)");
		} else {
			this.newGameDictLabel.setText("BRITISH words. (Change in options)");
		}
		super.onResume();
	}

	// The two methods below are duplicated from MainActivity
	// TODO: Merge back into one file
	private void openHelpDialog() {
		LayoutInflater li = LayoutInflater.from(this);
		View view = li.inflate(R.layout.aboutview, null);
		new AlertDialog.Builder(NewGameActivity.this)
				.setTitle("Instruction")
				.setIcon(R.drawable.about)
				.setView(view)
				//.setNeutralButton("Changes",
					//	new DialogInterface.OnClickListener() {

							// @Override
				//			public void onClick(DialogInterface dialog,
					//				int whichButton) {
					//			openChangesDialog();
							//}
						//})
				.setNegativeButton("Close",
						new DialogInterface.OnClickListener() {

							// @Override
							public void onClick(DialogInterface dialog,
									int whichButton) {
								//

							}
						}).show();
	}

	private void openChangesDialog() {
		LayoutInflater li = LayoutInflater.from(this);
		View view = li.inflate(R.layout.changeview, null);
		new AlertDialog.Builder(NewGameActivity.this)
				.setTitle("Changelog")
				.setIcon(R.drawable.about)
				.setView(view)
				.setNegativeButton("Close",
						new DialogInterface.OnClickListener() {

							// @Override
							public void onClick(DialogInterface dialog,
									int whichButton) {
								// TODO Auto-generated method stub

							}
						}).show();
	}

/*
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean supRetVal = super.onCreateOptionsMenu(menu);
		SubMenu menu_saveload = menu.addSubMenu(0, 0, 0, "Load");
		menu_saveload.setIcon(R.drawable.menu_saveload);
		SubMenu menu_options = menu.addSubMenu(0, 1, 0, "Options");
		menu_options.setIcon(R.drawable.menu_options);
		return supRetVal;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		boolean supRetVal = super.onOptionsItemSelected(menuItem);
		switch (menuItem.getItemId()) {
		case 0:
			// Modified by me 09-22-2013
			startActivityForResult(new Intent(NewGameActivity.this,
					MainActivity.class), MainActivity.ACTIVITY_LOADGAME);
			//
			startActivityForResult(new Intent(NewGameActivity.this,
					SavedGameList.class), MainActivity.ACTIVITY_LOADGAME);
			
			// Modified by me 09-17-2013
			//finish();
			break;
		case 1:
			
			startActivityForResult(new Intent(NewGameActivity.this,
					OptionsActivity.class), 0);
			break;
		}
		return supRetVal;
	}

*/
	public void newVersionCheck() {
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		SharedPreferences.Editor prefeditor = preferences.edit();

		int pref_version = preferences.getInt("currentversion", -1);
		int current_version = getVersionNumber();
		if (pref_version == -1 || pref_version != current_version) {
			prefeditor.putInt("currentversion", current_version);
			prefeditor.commit();
			this.openChangesDialog();
			return;
		}
	}

	public int getVersionNumber() {
		int version = -1;
		try {
			PackageInfo pi = getPackageManager().getPackageInfo(
					getPackageName(), 0);
			version = pi.versionCode;
		} catch (Exception e) {
			Log.e("Word Factory", "Package name not found", e);
		}
		return version;
	}

	public String getVersionName() {
		String version = "";
		try {
			PackageInfo pi = getPackageManager().getPackageInfo(
					getPackageName(), 0);
			version = pi.versionName;
		} catch (Exception e) {
			Log.e("Word Factory", "Package name not found", e);
		}
		return version;
	}

	@Override
	public void onClick(View v) {
		
		switch(v.getId()){
		
		case R.id.newGameStart:
			
			if (newGameWordCount.getCheckedRadioButtonId() < 1) {
				Toast.makeText(NewGameActivity.this,
						"Please select word count", Toast.LENGTH_SHORT)
						.show();
				return;
			}
			
			// Modified by me 9-15-2013 
			// To fetched the checkbox of timed game from preferences
			SharedPreferences getPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
			boolean timedGameChecked = getPrefs.getBoolean("timedgame", true);
			
			/**
			prefeditor.putBoolean("timed_game", newGameTimed.isChecked());
			prefeditor.commit();
			
			
			Intent i = new Intent().putExtra("wordcount",
					newGameWordCount.getCheckedRadioButtonId()).putExtra(
					"timed", newGameTimed.isChecked());
			setResult(Activity.RESULT_OK, i);
			finish();
			
			**/
			
			// timedGameChecked is from the optionsview.xml
			Intent i = new Intent().putExtra("wordcount",
					newGameWordCount.getCheckedRadioButtonId()).putExtra(
					"timed", timedGameChecked);
			setResult(Activity.RESULT_OK, i);			
			finish();
			break;
			
		case R.id.newGameHelp:
			openHelpDialog();
			break;
		
		case R.id.newOptions:
			startActivityForResult(new Intent(NewGameActivity.this,
					OptionsActivity.class), 0);
			break;
		
		case R.id.newExit:
			// Modified by me 10-11-2013
				new AlertDialog.Builder(this)
				.setMessage("Are you sure you want to exit?")
				.setCancelable(false)
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						NewGameActivity.this.finish();
					}
				})
				.setNegativeButton("No", null)
				.show();
			
			break;
		
		}
		
	}
	


}
