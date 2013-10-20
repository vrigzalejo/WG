package com.ciaramcelmer.wfactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import com.ciaramcelmer.wfactory.ShakeDetector.OnShakeListener;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	public static final int DIALOG_FETCHING = 0;
	public static final int DIALOG_DOWNLOADING = 1;
	public static final int CLEAR_TEXTBOX = 100;

	public static final int ACTIVITY_NEWGAME = 7;
	public static final int ACTIVITY_LOADGAME = 8;

	public static final int DOWNLOAD_STARTING = 0;
	public static final int DOWNLOAD_PROGRESS = 1;
	public static final int DOWNLOAD_COMPLETE = 2;
	public static final int COUNTDOWN_PING = 3;

	// Main grid object
	public TargetGridView targetGrid;

	// Current word is displayed here
	public TextView enteredWordBox;
	public AnimationSet animationSet;

	// "Clear" button
	private Button clearWord;

	// "Fetching words" dialog
	private ProgressDialog progressDialog;
	public ProgressBar countDownBar;
	public CountDown countDown;
	public TextView timeRemaining;
	private Button helpButton;

	// Player word count display
	private TextView playerWordCountLabel;
	private TextView playerWordCountText;

	// "Submit" button
	private Button submitWord;

	// Using Shake
	private ShakeDetector mShakeDetector;
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;

	// Thread to handle the dictionary
	public Thread dictionaryThread;

	// Points to the active instance of the main activity
	public static MainActivity currentInstance = null;

	// Filename where active games are saved to
	public static final String saveFilename = "/data/data/com.ciaramcelmer.wfactory/savedgame";

	// List of the players current words
	public ArrayList<PlayerWord> playerWords;

	// List view of the players current words
	public ListView playerWordList;

	// Adapter to link playerWordList to playerWords
	public WordAdapter playerWordsAdapter = null;

	// Selected word in listitem popup
	public PlayerWord currentSelectedWord;

	// Appl preferences
	public SharedPreferences preferences;
	public SharedPreferences.Editor prefeditor = null;

	private PowerManager.WakeLock wakeLock;

	// private NineLetterWord nlw;

	// Saved game
	public SavedGame savedGame = null;

	public TextView bottomText = null;

	// Modified by me 9-22-2013
	public MediaPlayer playSong;

	private static void setCurrent(MainActivity current) {
		MainActivity.currentInstance = current;
	}

	// Called when the activity is first created.
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// setContentView(R.layout.main);

		// Modified by me 10-11-2013
		this.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		// For now, A specific layout (fullscreen portrait)
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		MainActivity.setCurrent(this);
		setContentView(R.layout.main);

		this.targetGrid = (TargetGridView) findViewById(R.id.targetGrid);
		this.targetGrid.mContext = this;
		this.enteredWordBox = (TextView) findViewById(R.id.enteredWord);
		this.clearWord = (Button) findViewById(R.id.clearWord);
		this.submitWord = (Button) findViewById(R.id.submitWord);
		this.playerWordList = (ListView) findViewById(R.id.playerWordList);

		this.playerWordCountLabel = (TextView) findViewById(R.id.targetCountPlayerLabel);
		this.playerWordCountText = (TextView) findViewById(R.id.targetCountPlayer);

		// Configure the countdown timer
		this.countDownBar = (ProgressBar) findViewById(R.id.gameTimer);
		this.timeRemaining = (TextView) findViewById(R.id.timeRemaining);
		this.countDown = new CountDown(this);
		this.countDown
				.setCountDownTimeExpiredListener(this.countDown.new CountDownTimeExpiredListener() {
					@Override
					public void OnCountDownTimeExpired() {
						Log.d("Word Factory", "Countdown expired!!");

						// Set game state to false
						setGameState(false);
						enteredWordBox.setText("TIME'S UP!");
						scoreAllWords();

						// Modified by me 9-23-2013
						playSong.release();

						// You can't reshuffle using shake
						mSensorManager.unregisterListener(mShakeDetector);

					}
				});

		this.preferences = PreferenceManager.getDefaultSharedPreferences(this);
		this.prefeditor = preferences.edit();

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		this.wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
				"Word Factory");

		newVersionCheck();
		this.savedGame = new SavedGame(this);

		// ShakeDetector initialization
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mAccelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mShakeDetector = new ShakeDetector(new OnShakeListener() {

			@Override
			public void onShake() {

				try {
					// Modified by me 10-11-2013
					MainActivity.this.animateTargetGrid();

					Message msg = Message.obtain();

					int minSize = msg.arg1;
					int maxSize = msg.arg2;
					int attempts = 0;

					do {
						int random = (int) (Math.random() * DictionaryThread.nineLetterWords
								.size());
						DictionaryThread.currentNineLetter = DictionaryThread.nineLetterWords
								.get(random);
						if (attempts++ > 100)
							break;
					} while (NineLetterWord.shuffleWithRange(
							DictionaryThread.currentNineLetter, minSize,
							maxSize) == true);

					// msg.what = DictionaryThread.MESSAGE_HAVE_NINELETTER;
					msg.what = DictionaryThread.MESSAGE_HAVE_NINELETTER_ON_PROGRESS;
					msg.obj = DictionaryThread.currentNineLetter.shuffled;
					MainActivity.currentInstance.newWordReadyHandler
							.sendMessage(msg);

				} catch (Exception e) {
					Log.e("Word Factory", "Shake failed", e);
					e.printStackTrace();
				}
			}

		});

		// This is the font for the current word box
		// This is "Purisa" for now (ubuntu ttf-thai-tlwg)
		Typeface face = Typeface.createFromAsset(getAssets(), "fonts/font.ttf");
		this.enteredWordBox.setTypeface(face);

		// When a letter on the grid is touched, update the current word box
		this.targetGrid
				.setLetterTouchedListener(this.targetGrid.new LetterTouchedHandler() {
					public void handleLetterTouched(int index) {
						enteredWordBox.setText(targetGrid.getSelectedWord());
					}
				});

		// A click on "clear" clears the most recent letters
		this.clearWord.setOnClickListener(new OnClickListener() {

			// @Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				targetGrid.clearLastLetter();
				enteredWordBox.setText(targetGrid.getSelectedWord());
			}
		});

		this.helpButton = (Button) findViewById(R.id.helpButton);
		this.helpButton.setOnClickListener(new OnClickListener() {

			// @Override
			public void onClick(View v) {
				// TODO Auto-generated method stub

				openHelpDialog();
			}
		});

		// But a long click clears the word entirely
		this.clearWord.setOnLongClickListener(new OnLongClickListener() {

			// @Override
			public boolean onLongClick(View v) {
				// TODO Auto-generated method stub
				targetGrid.clearGrid();
				enteredWordBox.setText("");
				return true;
			}
		});

		// Clicking "submit" verifies the word then adds it to the list
		this.submitWord.setOnClickListener(new OnClickListener() {

			// @Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				MainActivity.this.handleSubmitClicked();
			}
		});

		this.playerWordList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> adapterView, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				PlayerWord word = MainActivity.this.playerWords.get(position);
				MainActivity.this.currentSelectedWord = word;
				String[] choices = new String[1];
				choices[0] = new String("Find definition (network)");
				new AlertDialog.Builder(view.getContext())
						.setTitle("Selected: " + word.word)
						.setItems(choices,
								new DialogInterface.OnClickListener() {

									public void onClick(DialogInterface dialog,
											int which) {
										// TODO Auto-generated method stub
										PlayerWord word = MainActivity.this.currentSelectedWord;
										Intent myIntent = null;
										myIntent = new Intent(
												MainActivity.this,
												DefineActivity.class);
										myIntent.putExtra(
												"com.ciaramcelmer.wfactory.wordToDefine",
												word.word.toLowerCase());
										startActivity(myIntent);
									}
								}).show();

			}
		});

		// Initialize the playerWord list, and set the Adapter
		this.playerWords = new ArrayList<PlayerWord>();
		this.InitPlayerWords();

		this.playerWordsAdapter = new WordAdapter(this, this.playerWords);
		this.playerWordList.setAdapter(this.playerWordsAdapter);

		// Dictionary thread is started, will fetch our words.
		this.dictionaryThread = new Thread(new DictionaryThread());
		this.dictionaryThread.start();

		this.setGameState(false);

	}

	public Handler newWordReadyHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case DictionaryThread.MESSAGE_HAVE_NINELETTER: {
				// Dictionary thread sends message that a new 9letter is
				// available.
				String nineLetterWord = (String) msg.obj;
				targetGrid.setLetters(nineLetterWord);
				setGameState(true);

				// This must be posted here. If called in the get_nine_letter
				// handler,
				// it blocks for some reason.
				Message message = Message.obtain();
				message.what = DictionaryThread.MESSAGE_GET_MATCHING_WORDS;
				DictionaryThread.currentInstance.messageHandler
						.sendMessage(message);
				break;
			}
			// Modified by me 10-11-2013
			case DictionaryThread.MESSAGE_HAVE_NINELETTER_ON_PROGRESS: {
				// Dictionary thread sends message that a new 9letter is
				// available.
				String nineLetterWord = (String) msg.obj;
				targetGrid.setLetters(nineLetterWord);
				setGameState(true);

				// This must be posted here. If called in the get_nine_letter
				// handler,
				// it blocks for some reason.
				Message message = Message.obtain();
				message.what = DictionaryThread.MESSAGE_GET_MATCHING_WORDS_ON_PROGRESS;
				DictionaryThread.currentInstance.messageHandler
						.sendMessage(message);
				break;
			}

			case DictionaryThread.MESSAGE_HAVE_MATCHING_WORDS:
				// Called when Dictionary thread has found all matching words.
				showWordCounts(0);
				MainActivity.this.dismissDialog(MainActivity.DIALOG_FETCHING);

				MainActivity.this.animateTargetGrid();

				MainActivity.this.countDown.begin(0, 0);
				Toast.makeText(
						MainActivity.this,
						"Remember - no proper nouns, no hyphenated or foreign words.",
						Toast.LENGTH_SHORT).show();

				break;
			case DictionaryThread.MESSAGE_DICTIONARY_READY:
				// Called after game is restored when dictionary is ready.
				if (MainActivity.this.savedGame
						.Restore(MainActivity.saveFilename)) {
					MainActivity.this.animateTargetGrid();
				} else {
					Intent i = new Intent(MainActivity.this,
							NewGameActivity.class);
					startActivityForResult(i, ACTIVITY_NEWGAME);
					// playArea.setVisibility(View.GONE);
				}
				break;
			case DictionaryThread.MESSAGE_FAIL_SMH_NINELETTER:
				MainActivity.this.dismissDialog(MainActivity.DIALOG_FETCHING);
				Toast.makeText(MainActivity.this, "Error fetching puzzle!",
						Toast.LENGTH_SHORT).show();
				break;
			case DictionaryThread.MESSAGE_FAIL_SMH_NINELETTER_NOTFOUND:
				MainActivity.this.dismissDialog(MainActivity.DIALOG_FETCHING);
				Toast.makeText(MainActivity.this,
						"Puzzle 9-letter word not found!", Toast.LENGTH_SHORT)
						.show();
				break;
			}
		}
	};

	// Handles progress messages for the SMH download feature.
	public Handler progressHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case DOWNLOAD_STARTING:
				progressDialog.setIndeterminate(false);
				break;
			case DOWNLOAD_PROGRESS:
				progressDialog.setProgress(msg.arg1);
				break;
			case DOWNLOAD_COMPLETE:
				MainActivity.this
						.dismissDialog(MainActivity.DIALOG_DOWNLOADING);
				showDialog(MainActivity.DIALOG_FETCHING);
				break;

			}
		}
	};

	public void handleSubmitClicked() {
		String word = targetGrid.getSelectedWord();
		String message = "";
		int addedSeconds = 0;
		if (word.length() < 4)
			message = "Must be atleast 4 letters";
		else if (!word
				.contains(DictionaryThread.currentInstance.currentNineLetter.magicLetter))
			message = "Must contain the middle letter: "
					+ DictionaryThread.currentInstance.currentNineLetter.magicLetter;
		else if (this.playerHasWord(word))
			message = "You already have that word.";
		else if (!word.contains(DictionaryThread.currentNineLetter.magicLetter))
			message = "You Got the longest word"
					+ DictionaryThread.MESSAGE_GET_NINELETTER;
		if (message != "") {
			Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
			return;
		}
		PlayerWord playerWord = new PlayerWord(word);
		playerWord.willAnimate = true;
		this.playerWords.add(playerWord);
		Collections.sort(this.playerWords);
		targetGrid.clearGrid();
		this.scoreWord(playerWord);
		this.showWordCounts(this.countCorrectWords());
		this.playerWordList.setSelectionFromTop(
				this.playerWords.indexOf(playerWord), 10);
		if (playerWord.result == PlayerWord.RESULT_OK)
			addedSeconds = this.countDown.addWord(playerWord.word.length());

		// Animate the word result text
		int animateColour;
		String animateText;
		if (this.countCorrectWords() >= DictionaryThread.currentInstance.validWords
				.size() + 1) {
			Log.d("Word Factory", "Got all words!");
			this.countDown.pause();
			animateColour = 0xFF008000;
			animateText = "COMPLETE!";
			// Modified by me 9-23-2013
			playSong.release();
			setGameState(false);

		} else if (playerWord.result == PlayerWord.RESULT_OK) {
			animateColour = 0xFF008000;
			animateText = "GOT IT!";
			if (countDown.enabled)
				animateText += " +" + addedSeconds + "s";

		} else {
			animateColour = 0xFFF00000;
			animateText = "UNKNOWN!";
		}
		animateTextBox(animateText, animateColour, R.anim.textboxfade);
		if (this.playerWords.size() == DictionaryThread.currentInstance.validWords
				.size()) {
			Log.d("Word Factory", "Got all words!");
			this.countDown.pause();
		}
	}

	// @Override
	public void onPause() {
		mSensorManager.unregisterListener(mShakeDetector);
		this.savedGame.Save();
		this.countDown.pause();
		if (this.wakeLock.isHeld())
			this.wakeLock.release();

		Log.d("Word Factory", "Paused game");
		super.onPause();
		// Modified by me 9-23-2013
		playSong.release();
	}

	public void onResume() {
		if (this.countDown.active || this.targetGrid.gameActive)
			this.countDown.resume();

		// Modified by me 9-22-2013
		playSong = MediaPlayer.create(MainActivity.this, R.raw.song);
		// start the song
		SharedPreferences getPrefs = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		boolean music = getPrefs.getBoolean("sounds", true);

		if (music == true)
			playSong.start();
		// --------------

		if (this.preferences.getBoolean("wakelock", true)) {
			Log.d("Word Factory", "Getting wake lock");
			this.wakeLock.acquire();
		}
		boolean sounds = preferences.getBoolean("sounds", true);
		this.clearWord.setSoundEffectsEnabled(sounds);
		this.submitWord.setSoundEffectsEnabled(sounds);
		this.playerWordList.setSoundEffectsEnabled(sounds);
		this.helpButton.setSoundEffectsEnabled(sounds);

		Log.d("Word Factory", "Resumed Game");
		super.onResume();
		mSensorManager.registerListener(mShakeDetector, mAccelerometer,
				SensorManager.SENSOR_DELAY_UI);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		// TODO Auto-generated method stub
		if (id == DIALOG_FETCHING) {
			progressDialog = new ProgressDialog(this);
			progressDialog.setTitle("Fetching words");
			progressDialog.setMessage("Please wait...");
			progressDialog.setIndeterminate(false);
			progressDialog.setCancelable(true);
			return progressDialog;
		} else if (id == DIALOG_DOWNLOADING) {
			progressDialog = new ProgressDialog(this);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setTitle("Downloading puzzle");
			progressDialog.setMessage("Please wait...");
			progressDialog.setIndeterminate(true);
			progressDialog.setProgress(0);
			progressDialog.setMax(100);
			return progressDialog;
		}
		return null;
	}

	// Sets the game to active or inactive by disabling/enabling the controls
	public void setGameState(boolean state) {
		this.targetGrid.gameActive = state;
		this.submitWord.setEnabled(state);
		this.clearWord.setEnabled(state);
		this.playerWordList.setClickable(state);
	}

	// Updated the "Good/Very Good/Excellent" display
	public void showWordCounts(int playerWords) {
		int numWords = DictionaryThread.currentInstance.validWords.size() + 1;
		int good = numWords / 2;
		int vgood = numWords * 3 / 4;
		int excellent = numWords;
		TextView countGood = (TextView) findViewById(R.id.targetCountGood);
		TextView countVeryGood = (TextView) findViewById(R.id.targetVeryCountGood);
		TextView countExcellent = (TextView) findViewById(R.id.targetCountExcellent);

		countGood.setText(good + " words");
		countVeryGood.setText(vgood + " words");
		countExcellent.setText(excellent + " words");

		playerWordCountText.setText(playerWords + " words");

		if (numWords > 0 && this.targetGrid.gameActive == false) {
			if (playerWords >= excellent)
				showWordMessage("EXCELLENT! PERFECT!");
			else if (playerWords >= vgood)
				showWordMessage("VERY GOOD! AMAZING!");
			else if (playerWords >= good)
				showWordMessage("YOU ARE GOOD!");
		}

	}

	public void showWordMessage(String message) {
		// TODO Auto-generated method stub
		TextView box = this.enteredWordBox;
		box.setText(message);
	}

	public void animateTargetGrid() {
		Animation animation = AnimationUtils.loadAnimation(this,
				R.anim.targetzoomin);
		this.targetGrid.setVisibility(View.VISIBLE);
		this.targetGrid.startAnimation(animation);
	}

	public void animateTextBox(String text, int colour, int animationResource) {

		Animation animation = AnimationUtils.loadAnimation(this,
				animationResource);

		animation.setAnimationListener(new AnimationListener() {

			public void onAnimationEnd(Animation animation) {
				// TODO Auto-generated method stub
				enteredWordBox.setTextColor(0xFF000000);
				MainActivity.currentInstance.enteredWordBox.setText("");
				enteredWordBox.setGravity(Gravity.LEFT);
				playerWordsAdapter.notifyDataSetChanged();
			}

			public void onAnimationStart(Animation animation) {
			}

			public void onAnimationRepeat(Animation animation) {
			}

		});
		this.enteredWordBox.setGravity(Gravity.CENTER);
		this.enteredWordBox.setText(text);
		this.enteredWordBox.setTextColor(colour);
		this.enteredWordBox.startAnimation(animation);
	}

	// Menus are created here. Composing of 3 menu items, all of it are quite
	// self
	// explanatory

	private static final int MENU_NEWWORD = 0;
	private static final int MENU_SAVELOAD = 1;
	private static final int MENU_SCORE = 2;
	private static final int MENU_INSTRUCTIONS = 3;
	private static final int MENU_OPTIONS = 4;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		// getMenuInflater().inflate(R.menu.main, menu);
		boolean supRetVal = super.onCreateOptionsMenu(menu);

		SubMenu menu_new = menu.addSubMenu(0, MENU_NEWWORD, 0, "New Game");
		menu_new.setIcon(R.drawable.menu_new);

		SubMenu menu_saveload = menu.addSubMenu(0, MENU_SAVELOAD, 0, "Save");
		menu_saveload.setIcon(R.drawable.menu_saveload);

		SubMenu menu_score = menu.addSubMenu(0, MENU_SCORE, 0, "Score Game");
		menu_score.setIcon(R.drawable.menu_score);
		playSong.pause();

		// SubMenu menu_help = menu.addSubMenu(0, MENU_INSTRUCTIONS, 0, "Help");
		// menu_help.setIcon(R.drawable.menu_help);

		// SubMenu menu_options = menu.addSubMenu(0, MENU_OPTIONS, 0,
		// "Options");
		// menu_options.setIcon(R.drawable.menu_options);

		return supRetVal;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		// TODO Auto-generated method stub
		boolean supRetVal = super.onOptionsItemSelected(menuItem);

		switch (menuItem.getItemId()) {
		case MENU_NEWWORD: {
			Intent i = new Intent(this, NewGameActivity.class);
			startActivityForResult(i, ACTIVITY_NEWGAME);
			break;
		}
		case MENU_SCORE: {
			if (targetGrid.gameActive) {
				if (this.countDown.enabled)
					this.countDown.remainingTime = 0;
				else {
					this.setGameState(false);
					this.scoreAllWords();
					playSong.pause();
				}
			}
			break;
		}
		case MENU_INSTRUCTIONS: {
			openHelpDialog();
			break;
		}
		case MENU_OPTIONS:
			startActivityForResult(new Intent(MainActivity.this,
					OptionsActivity.class), 0);
			break;
		case MENU_SAVELOAD:

			startActivityForResult(new Intent(MainActivity.this,
					SavedGameList.class), ACTIVITY_LOADGAME);
			break;
		}

		return supRetVal;

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		// super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == ACTIVITY_LOADGAME
				&& resultCode == Activity.RESULT_OK) {
			String filename = data.getExtras().getString("filename");
			if (MainActivity.this.savedGame.Restore(filename)) {
				new File(filename).delete();
				MainActivity.this.animateTargetGrid();
				return;
			}
		}
		if (requestCode != ACTIVITY_NEWGAME || resultCode != Activity.RESULT_OK)
			return;
		Log.d("Word Factory", "Got newgame result: request " + requestCode
				+ " result " + resultCode);
		final Message msg = Message.obtain();
		Bundle extras = data.getExtras();
		if (extras.getBoolean("fromsmh")) {
			msg.what = DictionaryThread.MESSAGE_GET_SMH_NINELETTER;
		} else {
			msg.what = DictionaryThread.MESSAGE_GET_NINELETTER;
			switch (extras.getInt("wordcount")) {
			case R.id.newWordCount1:
				msg.arg1 = 1;
				msg.arg2 = 30;
				break;
			case R.id.newWordCount30:
				msg.arg1 = 30;
				msg.arg2 = 75;
				break;
			case R.id.newWordCount75:
				msg.arg1 = 75;
				msg.arg2 = 500;
				break;
			default:
				return;
			}
		}
		showDialog(msg.what == DictionaryThread.MESSAGE_GET_SMH_NINELETTER ? MainActivity.DIALOG_DOWNLOADING
				: MainActivity.DIALOG_FETCHING);

		this.InitPlayerWords();
		this.playerWordsAdapter.notifyDataSetChanged();
		this.showWordMessage("");
		this.timeRemaining.setText("");
		this.targetGrid.setVisibility(View.INVISIBLE);
		this.countDown.enabled = extras.getBoolean("timed");
		this.enteredWordBox.setText("");

		new File(MainActivity.saveFilename).delete();
		DictionaryThread.currentInstance.messageHandler.sendMessage(msg);

	}

	private boolean playerHasWord(String word) {
		for (PlayerWord playerWord : this.playerWords) {
			if (playerWord.word.contentEquals(word))
				return true;
		}
		return false;
	}

	// Count number of correct words the player has
	public int countCorrectWords() {
		int correct = 0;
		for (PlayerWord playerWord : this.playerWords) {
			if (playerWord.result != PlayerWord.RESULT_HEADER
					&& scoreWord(playerWord))
				correct++;
		}
		return correct;
	}

	public void InitPlayerWords() {
		this.playerWords.clear();
		PlayerWord header = new PlayerWord("YOUR WORDS");
		header.result = PlayerWord.RESULT_HEADER;
		this.playerWords.add(header);
	}

	// Returns a count of the player's words.
	public int CountPlayerWords() {
		int count = 0;
		for (PlayerWord word : this.playerWords) {
			if (word.result != PlayerWord.RESULT_HEADER
					&& word.result != PlayerWord.RESULT_MISSED)
				count++;

		}
		return count;
	}

	// Score an individual word
	// Returns boolean, if the word is valid/ok
	public boolean scoreWord(PlayerWord playerWord) {
		if (DictionaryThread.currentInstance.validWords
				.contains(playerWord.word)
				|| playerWord.word
						.equals(DictionaryThread.currentInstance.currentNineLetter.word)) {
			playerWord.result = PlayerWord.RESULT_OK;
		} else
			playerWord.result = PlayerWord.RESULT_INVALID;

		this.playerWordsAdapter.notifyDataSetChanged();
		return playerWord.result == PlayerWord.RESULT_OK;
	}

	// Score the player's words
	public void scoreAllWords() {
		int correctUserWords;
		int missedHeaderIndex;

		this.countDown.end();
		correctUserWords = countCorrectWords();

		PlayerWord header = new PlayerWord("MISSED WORDS");
		header.result = PlayerWord.RESULT_HEADER;
		this.playerWords.add(header);
		missedHeaderIndex = this.playerWords.size();
		// If the player missed it, show the 9 letter word first
		if (!playerHasWord(DictionaryThread.currentInstance.currentNineLetter.word)) {
			PlayerWord resultWord = new PlayerWord(
					DictionaryThread.currentInstance.currentNineLetter.word);
			resultWord.result = PlayerWord.RESULT_MISSED;
			this.playerWords.add(resultWord);
		}

		// Then show all other missed words
		for (String validWord : DictionaryThread.currentInstance.validWords) {
			if (playerHasWord(validWord) == false
					&& validWord != DictionaryThread.currentInstance.currentNineLetter.word) {
				PlayerWord resultWord = new PlayerWord(validWord);
				resultWord.result = PlayerWord.RESULT_MISSED;
				this.playerWords.add(resultWord);
			}
		}
		this.playerWordsAdapter.notifyDataSetChanged();
		this.playerWordList.setSelection(missedHeaderIndex - 1);
		showWordCounts(correctUserWords);
	}

	private void openHelpDialog() {
		// Disables the game
		// this.onPause();

		LayoutInflater li = LayoutInflater.from(this);
		View view = li.inflate(R.layout.aboutview, null);
		new AlertDialog.Builder(MainActivity.this)
				.setTitle("Word Factory Help")
				.setIcon(R.drawable.about)
				.setView(view)
				.setNeutralButton("Changes",
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,
									int whichButton) {
								// TODO Auto-generated method stub
								MainActivity.currentInstance
										.openChangesDialog();
								// MainActivity.this.onResume();
							}
						})
				.setNegativeButton("Close",
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,
									int whichButton) {
								// Enables the game after clicked
								// MainActivity.this.onResume();

							}
						}).show();

	}

	private void openChangesDialog() {
		// Disables the game
		// this.onPause();

		LayoutInflater li = LayoutInflater.from(this);
		View view = li.inflate(R.layout.changeview, null);
		new AlertDialog.Builder(MainActivity.this)
				.setTitle("ChangeLog")
				.setIcon(R.drawable.about)
				.setView(view)
				.setNegativeButton("Close",
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {
								// Enables the game after clicked
								// MainActivity.this.onResume();
							}
						}).show();

	}

	public void newVersionCheck() {
		int pref_version = preferences.getInt("Current Version", -1);
		int current_version = getVersionNumber();
		if (pref_version == -1 || pref_version != current_version) {
			new File(MainActivity.saveFilename).delete();

			Log.d("Word Factory", "Version number bumped from " + pref_version
					+ " to " + current_version);
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

	public void toExit(){
		new AlertDialog.Builder(this)
		.setMessage("Are you sure you want to exit?")
		.setCancelable(false)
		.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				MainActivity.this.finish();
			}
		})
		.setNegativeButton("No", null)
		.show();
	}
	
	@Override
	public void onBackPressed() {
		toExit();
	}
}
