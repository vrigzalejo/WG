package com.ciaramcelmer.wfactory;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Message;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;

public class OptionsActivity extends PreferenceActivity implements
	OnSharedPreferenceChangeListener {
	
	ListPreference dictpref = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.optionsview);
		this.dictpref = (ListPreference) findPreference("dictpref");
		this.dictpref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			
			//@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Message message = Message.obtain();
				message.what = DictionaryThread.MESSAGE_REREAD_DICTIONARY;
				DictionaryThread.currentInstance.messageHandler.sendMessage(message);
				return true;
			}
		});
	}

	//@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		Log.d("Word Factory", "Preference " + key + " changed.");
		if (key.equals("dictpref")) {
			Message message = Message.obtain();
			message.what = DictionaryThread.MESSAGE_REREAD_DICTIONARY;
			DictionaryThread.currentInstance.messageHandler.sendMessage(message);
		}
	}

}
