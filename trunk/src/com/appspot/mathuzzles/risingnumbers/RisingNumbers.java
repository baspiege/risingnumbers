package com.appspot.mathuzzles.risingnumbers;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.appspot.mathuzzles.risingnumbers.R;
import com.appspot.mathuzzles.risingnumbers.GameView.GameThread;

/**
 * Rising numbers.
 * 
 * Copyright 2011 - Brian Spiegel
 */
public class RisingNumbers extends Activity implements OnClickListener {

	private static final int MENU_PAUSE = 1;
	private static final int MENU_RESUME = 2;
	private static final int MENU_START = 3;
	private static final int MENU_START_MULTI_PLAY = 4;

	private static final String SAVED_GAME_FILE_NAME = "savedGame";
	public static final String HIGHSCORE_FILENAME = "highScore";

	/** A handle to the View in which the game is running. */
	private GameView mGameView;

	public static final String CURR_BALL = "CURR_BALL";
	public static final String BALLS = "BALLS";
	public static final String BALLS_IN_QUEUE = "BALLS_IN_QUEUE";
	public static final String CURRENT_POINTS = "CURRENT_POINTS";
	public static final String IS_GAME_OVER = "IS_GAME_OVER";
	public static final String IS_GAME_WON = "IS_GAME_WON";
	public static final String MOVE_X = "MOVE_X";
	public static final String MOVE_Y = "MOVE_Y";
	public static final String LAST_X = "LAST_X";
	public static final String IS_SHOOTING = "IS_SHOOTING";
	public static final String IS_PLAY_ONLINE = "IS_PLAY_ONLINE";
	public static final String MULTI_PLAY_GAME_STATUS = "MULTI_PLAY_GAME_STATUS";
	public static final String MULTI_PLAY_GAME_STARTED = "MULTI_PLAY_GAME_STARTED";
	public static final String MULTI_PLAY_USER_ID = "MULTI_PLAY_USER_ID";
	public static final String BALLS_TO_OPPONENT = "BALLS_TO_OPPONENT";
	public static final String BALLS_FROM_OPPONENT = "BALLS_FROM_OPPONENT";

	/**
	 * Invoked during init to give the Activity a chance to set up its Menu.
	 * 
	 * @param menu
	 *            the Menu to which entries may be added
	 * @return true
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_START, 0, R.string.menu_start);
		menu.add(0, MENU_START_MULTI_PLAY, 0, R.string.menu_start_multiplay);
		menu.add(0, MENU_PAUSE, 0, R.string.menu_pause);
		menu.add(0, MENU_RESUME, 0, R.string.menu_resume);
		return true;
	}

	/**
	 * Invoked when the user selects an item from the Menu.
	 * 
	 * @param item
	 *            the Menu entry which was selected
	 * @return true if the Menu item was legit (and we consumed it), false
	 *         otherwise
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		GameThread gameThread = mGameView.getThread();
		switch (item.getItemId()) {
		case MENU_START:
			gameThread.setState(GameThread.STATE_RUNNING);
			gameThread.setIsPlayOnline(false);
			gameThread.doStart();
			return true;
		case MENU_START_MULTI_PLAY:
			gameThread.setState(GameThread.STATE_RUNNING);
			gameThread.setIsPlayOnline(true);
			gameThread.doStart();
			return true;
		case MENU_PAUSE:
			gameThread.pause();
			return true;
		case MENU_RESUME:
			gameThread.unpause();
			return true;
		}

		return false;
	}

	/**
	 * Invoked when the Activity is created.
	 * 
	 * @param savedInstanceState
	 *            a Bundle containing state saved from a previous execution, or
	 *            null if this is a new execution
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Use the layout defined in our XML file
		setContentView(R.layout.risingnumbers_layout);

		// Get handles to the GameView from XML
		mGameView = (GameView) findViewById(R.id.risingnumbers);

		// Start game thread
		GameThread gameThread = mGameView.getThread();
		gameThread.setState(GameThread.STATE_RUNNING);
		gameThread.doStart();
	}

	/**
	 * Invoked when the Activity loses user focus.
	 */
	@Override
	protected void onPause() {
		super.onPause();

		try {
			// Save game
			FileOutputStream fos = openFileOutput(SAVED_GAME_FILE_NAME,
					Context.MODE_PRIVATE);
			ObjectOutputStream out = new ObjectOutputStream(fos);
			out.writeObject(mGameView.getThread().getGameState());
			out.close();

		} catch (Exception e) {
			Log
					.e(this.getClass().getName(), "Exception saving:"
							+ e.toString());
		}
	}

	protected void onResume() {

		super.onResume();

		// Restore
		HashMap<String, Object> savedGame = getSavedGame(this);
		if (savedGame != null && savedGame.get(CURR_BALL) != null) {
			mGameView.getThread().restoreState(savedGame);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onClick(DialogInterface arg0, int arg1) {
	}

	/**
	 * Get saved game.
	 * 
	 * Get bundle.
	 */
	public static HashMap<String, Object> getSavedGame(Context context) {
		HashMap<String, Object> hashMap = null;
		try {
			FileInputStream fis = context.openFileInput(SAVED_GAME_FILE_NAME);
			if (fis != null) {
				ObjectInputStream in = new ObjectInputStream(fis);
				if (in != null) {
					hashMap = (HashMap<String, Object>) in.readObject();
					if (hashMap != null && !hashMap.isEmpty()) {
						return hashMap;
					}
				}
			}
		} catch (Exception e) {
			Log.e(RisingNumbers.class.getName(),
					"Exception getting saved game:" + e.toString());
		}
		return hashMap;
	}
}
