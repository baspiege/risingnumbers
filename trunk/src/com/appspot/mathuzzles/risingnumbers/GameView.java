package com.appspot.mathuzzles.risingnumbers;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import com.appspot.mathuzzles.risingnumbers.R;
import com.appspot.mathuzzles.risingnumbers.model.Ball;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;

/**
 * Rising numbers view.
 * 
 * Has a mode: running, paused, game over.
 */
class GameView extends SurfaceView implements SurfaceHolder.Callback {
	class GameThread extends Thread {

		// State-tracking constants
		public static final int STATE_OVER = 1;
		public static final int STATE_PAUSE = 2;
		public static final int STATE_RUNNING = 3;

		// Set-up constants
		private static final int BALLS_IN_ROW = 6;
		private static final int NEW_BALL_MAX = 25;
		private static final int BALLS_IN_QUEUE = 3;
		private static final int ANIMATION_MILLIS = 20;
		private static final int BALL_SPACING = 48;
		private static final int KEYBOARD_SPACING = 12;
		private static final int STARTING_BALL_SPACING_LEFT = 12;
		private static final int MARGIN_LEFT = 18;
		private static final int MARGIN_RIGHT = 282;
		private static final int MARGIN_TOP = 18;
		private static final int QUEUE_Y = 282;
		private static final int Y_DELTA = 1;
		private static final int BALL_RADIUS = 15;
		private static final int BALL_DISTANCE = BALL_RADIUS * 2;

		// Running game fields
		private Ball currBall = null;
		private ArrayList<Ball> balls = new ArrayList<Ball>();
		private ArrayList<Ball> ballsInQueue = new ArrayList<Ball>();
		private int points = 0;
		private int highScore = 0;
		private boolean isGameOver = false;
		private float moveX = 0;
		private float moveY = 0;
		private int lastX = 0;
		private boolean shooting = false;

		/** Used to figure out elapsed time between frames */
		private long mLastTime;

		// Colors
		private Paint mClearColor;
		private Paint mGreyColor;
		private Paint mBallColor;
		private Paint mTextColorSmallBold;
		private Paint mTextColorMedium;
		private Paint mTextColorMediumBold;
		private Paint mTextColorLargeBold;

		/** The state of the game. One of running, pause, or game over. */
		private int mMode;

		/** Indicate whether the surface has been created & is ready to draw */
		private boolean mRun = false;

		/** Handle to the surface manager object we interact with */
		private SurfaceHolder mSurfaceHolder;

		// Points display (so strings don't need to be created with each drawing
		// pass)
		private String pointsDisplay = "";
		private String highScoreDisplay = "";

		public GameThread(SurfaceHolder surfaceHolder, Context context) {
			mSurfaceHolder = surfaceHolder;
			mContext = context;

			// Initialize paints
			mClearColor = new Paint();
			mClearColor.setAntiAlias(true);
			mClearColor.setARGB(255, 0, 0, 0);

			mGreyColor = new Paint();
			mGreyColor.setAntiAlias(true);
			mGreyColor.setARGB(255, 20, 20, 20);

			mBallColor = new Paint();
			mBallColor.setAntiAlias(true);
			mBallColor.setARGB(255, 0, 0, 255);

			mTextColorLargeBold = new Paint();
			mTextColorLargeBold.setAntiAlias(true);
			mTextColorLargeBold.setARGB(255, 255, 255, 255);
			mTextColorLargeBold.setFakeBoldText(true);
			mTextColorLargeBold.setTextSize(24);

			mTextColorMedium = new Paint();
			mTextColorMedium.setAntiAlias(true);
			mTextColorMedium.setARGB(255, 255, 255, 255);
			mTextColorMedium.setTextSize(22);

			mTextColorMediumBold = new Paint();
			mTextColorMediumBold.setAntiAlias(true);
			mTextColorMediumBold.setARGB(255, 255, 255, 255);
			mTextColorMediumBold.setFakeBoldText(true);
			mTextColorMediumBold.setTextSize(22);

			mTextColorSmallBold = new Paint();
			mTextColorSmallBold.setAntiAlias(true);
			mTextColorSmallBold.setARGB(255, 255, 255, 255);
			mTextColorSmallBold.setFakeBoldText(true);
			mTextColorSmallBold.setTextSize(16);
		}

		/**
		 * Starts the game.
		 */
		public void doStart() {
			synchronized (mSurfaceHolder) {
				if (mMode != STATE_PAUSE) {
					initHighScore();
					createBoard();
					lastX = MARGIN_LEFT + STARTING_BALL_SPACING_LEFT
							+ (BALLS_IN_QUEUE * BALL_SPACING);
					createNewBall();
					points = 0;
					setPointsDisplay();
					setHighScoreDisplay();
					isGameOver = false;
					shooting = false;
					moveX = 0;
					moveY = 0;
				}
				mLastTime = System.currentTimeMillis();
				setState(STATE_RUNNING);
			}
		}

		/**
		 * Get the high score from the file system.
		 */
		public void initHighScore() {
			try {
				FileInputStream fis = mContext
						.openFileInput(RisingNumbers.HIGHSCORE_FILENAME);
				if (fis != null) {
					ObjectInputStream in = new ObjectInputStream(fis);
					if (in != null) {
						Integer highScoreInteger = (Integer) in.readObject();
						if (highScoreInteger != null) {
							highScore = highScoreInteger.intValue();
						}
					}
				}
			} catch (Exception e) {
				Log.e(this.getClass().getName(),
						"Exception getting high score from file:"
								+ e.toString());
			}
		}

		/**
		 * Pauses the game.
		 */
		public void pause() {
			synchronized (mSurfaceHolder) {
				if (mMode == STATE_RUNNING) {
					setState(STATE_PAUSE);
				}
			}
		}

		/**
		 * Sets state to game ended.
		 */
		public void stopGame() {
			synchronized (mSurfaceHolder) {
				if (mMode == STATE_RUNNING) {
					setState(STATE_OVER);
				}
			}
		}

		/**
		 * Set move.
		 */
		public void setMove(float x, float y) {
			synchronized (mSurfaceHolder) {
				moveX = x;
				moveY = y;
			}
		}

		/**
		 * Set if shooting.
		 */
		public void setShooting(boolean aShooting) {
			synchronized (mSurfaceHolder) {
				shooting = aShooting;
			}
		}

		/**
		 * Restores game state.
		 * 
		 * @param saveGame
		 *            map of fields
		 */
		public synchronized void restoreState(HashMap<String, Object> savedGame) {
			synchronized (mSurfaceHolder) {
				try {
					currBall = (Ball) savedGame.get(RisingNumbers.CURR_BALL);
					balls = (ArrayList<Ball>) savedGame
							.get(RisingNumbers.BALLS);
					ballsInQueue = (ArrayList<Ball>) savedGame
							.get(RisingNumbers.BALLS_IN_QUEUE);
					points = (Integer) savedGame
							.get(RisingNumbers.CURRENT_POINTS);
					setPointsDisplay();
					isGameOver = (Boolean) savedGame
							.get(RisingNumbers.IS_GAME_OVER);
					moveX = (Float) savedGame.get(RisingNumbers.MOVE_X);
					moveY = (Float) savedGame.get(RisingNumbers.MOVE_Y);
					shooting = (Boolean) savedGame
							.get(RisingNumbers.IS_SHOOTING);
					lastX = (Integer) savedGame.get(RisingNumbers.LAST_X);
					initHighScore();
					setHighScoreDisplay();
				} catch (Exception e) {
					Log.e(this.getClass().getName(),
							"Exception restoring state saved game: "
									+ e.toString());
					// Start new game
					doStart();
				}
			}
		}

		@Override
		public void run() {
			while (mRun) {
				Canvas c = null;
				try {
					c = mSurfaceHolder.lockCanvas();

					if (c != null) {
						synchronized (mSurfaceHolder) {
							if (mMode == STATE_RUNNING) {
								incrementBoard();
								doDraw(c);
							} else if (mMode == STATE_PAUSE) {
								// Clear screen
								c.drawPaint(mClearColor);
								drawPaused(c);
							} else if (mMode == STATE_OVER) {
								doDraw(c);
								drawGameOver(c);
							}
						}
					}

				} finally {
					// Do this in a finally so that if an exception is thrown
					// during the above, the Surface is not in an inconsistent
					// state
					if (c != null) {
						mSurfaceHolder.unlockCanvasAndPost(c);
					}
				}
			}
		}

		/**
		 * Save state. Activity is being suspended.
		 * 
		 * @return Map with this view's state
		 */
		public HashMap<String, Object> getGameState() {
			HashMap<String, Object> gameSate = new HashMap<String, Object>();
			synchronized (mSurfaceHolder) {
				gameSate.put(RisingNumbers.CURR_BALL, currBall);
				gameSate.put(RisingNumbers.BALLS, balls);
				gameSate.put(RisingNumbers.BALLS_IN_QUEUE, ballsInQueue);
				gameSate.put(RisingNumbers.CURRENT_POINTS, points);
				gameSate.put(RisingNumbers.IS_GAME_OVER, isGameOver);
				gameSate.put(RisingNumbers.MOVE_X, moveX);
				gameSate.put(RisingNumbers.MOVE_Y, moveY);
				gameSate.put(RisingNumbers.IS_SHOOTING, shooting);
				gameSate.put(RisingNumbers.LAST_X, lastX);
			}
			return gameSate;
		}

		/**
		 * Used to signal the thread whether it should be running or not.
		 * Passing true allows the thread to run; passing false will shut it
		 * down if it's already running. Calling start() after this was most
		 * recently called with false will result in an immediate shutdown.
		 * 
		 * @param b
		 *            true to run, false to shut down
		 */
		public void setRunning(boolean b) {
			mRun = b;
		}

		/**
		 * Sets the game mode. That is, whether we are running, paused, in the
		 * failure state, in the victory state, etc.
		 * 
		 * @param mode
		 *            one of the STATE_* constants
		 */
		public void setState(int mode) {
			synchronized (mSurfaceHolder) {
				mMode = mode;
			}
		}

		/**
		 * Resumes from a pause.
		 */
		public void unpause() {
			synchronized (mSurfaceHolder) {

				// Give 100 millsecond delay
				mLastTime = System.currentTimeMillis() + 100;
			}
			setState(STATE_RUNNING);
		}

		/**
		 * Handles a key-down event.
		 * 
		 * @param keyCode
		 *            the key that was pressed
		 * @param msg
		 *            the original event object
		 * @return true
		 */
		boolean doKeyDown(int keyCode, KeyEvent msg) {
			synchronized (mSurfaceHolder) {

				if (mMode == STATE_RUNNING) {
					// center/space -> fire
					if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
							|| keyCode == KeyEvent.KEYCODE_SPACE) {
						shooting = true;
						return true;
					}
					// left/q -> left
					else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
							|| keyCode == KeyEvent.KEYCODE_Q) {
						currBall.x -= KEYBOARD_SPACING;
						if (currBall.x < MARGIN_LEFT) {
							currBall.x = MARGIN_LEFT;
						}
						return true;

					}
					// right/w -> right
					else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
							|| keyCode == KeyEvent.KEYCODE_W) {
						currBall.x += KEYBOARD_SPACING;
						if (currBall.x > MARGIN_RIGHT) {
							currBall.x = MARGIN_RIGHT;
						}
						return true;

					} // up -> pause
					else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
						pause();
						return true;
					}
				}
				// New Game
				else if (mMode == STATE_OVER) {
					doStart();
					return true;
				}
				// Resume
				else if (mMode == STATE_PAUSE) {
					// paused -> running
					unpause();
					return true;
				}

				return false;
			}
		}

		/**
		 * Set points display.
		 */
		public void setPointsDisplay() {
			pointsDisplay = mContext.getString(R.string.points) + " " + points;

		}

		/**
		 * Set high score display.
		 */
		public void setHighScoreDisplay() {
			highScoreDisplay = mContext.getString(R.string.highScore) + " "
					+ highScore;

		}

		private void createNewBall() {

			// Get latest
			currBall = ballsInQueue.get(ballsInQueue.size() - 1);
			currBall.x = lastX;

			// Remove from queue
			ballsInQueue.remove(ballsInQueue.size() - 1);

			// Create new ball and add to back.
			Ball ball = new Ball();
			ball.number = generateRandom(NEW_BALL_MAX) + 2;
			ball.y = QUEUE_Y;
			ballsInQueue.add(0, ball);

			// Move up rest of queue
			int x = MARGIN_LEFT + STARTING_BALL_SPACING_LEFT;
			int size = ballsInQueue.size();
			for (int i = 0; i < size; i++) {
				ballsInQueue.get(i).x = x;
				x += BALL_SPACING;
			}
		}

		private void drawPaused(Canvas canvas) {
			// Text
			canvas.drawText(mContext.getString(R.string.paused), 110, 110,
					mTextColorMedium);
		}

		private void drawGameOver(Canvas canvas) {
			// Back ground
			canvas.drawRect(95, 80, 245, 130, mGreyColor);

			// Text
			canvas.drawText(mContext.getString(R.string.gameOver), 110, 110,
					mTextColorMedium);
		}

		/**
		 * Draw ball.
		 */
		private void drawBall(Canvas canvas, Ball ball) {
			canvas.drawCircle(ball.x, ball.y, BALL_RADIUS, mBallColor);

			// 0-9
			if (ball.number < 10) {
				canvas.drawText(Integer.toString(ball.number), ball.x - 6,
						ball.y + 8, mTextColorLargeBold);
			}
			// 10-99
			else if (ball.number < 100) {
				canvas.drawText(Integer.toString(ball.number), ball.x - 12,
						ball.y + 8, mTextColorMediumBold);
			}
			// 100 ->
			else {
				canvas.drawText(Integer.toString(ball.number), ball.x - 14,
						ball.y + 6, mTextColorSmallBold);
			}
		}

		/**
		 * Draw board
		 */
		private void doDraw(Canvas canvas) {

			// Clear screen
			canvas.drawPaint(mClearColor);

			// Left side
			canvas.drawLine(0, 0, 0, 300, mTextColorMedium);

			// Right side
			canvas.drawLine(300, 0, 300, 300, mTextColorMedium);

			// Bottom side
			canvas.drawLine(0, 300, 300, 300, mTextColorMedium);

			// Top side
			canvas.drawLine(0, 2, 300, 2, mTextColorMedium);

			// Draw balls on board
			int size = balls.size();
			for (int i = 0; i < size; i++) {
				drawBall(canvas, balls.get(i));
			}

			// Draw balls in queue
			size = ballsInQueue.size();
			for (int i = 0; i < size; i++) {
				drawBall(canvas, ballsInQueue.get(i));
			}

			// Draw current ball
			drawBall(canvas, currBall);

			// Draw points
			canvas.drawText(pointsDisplay, 1, 326, mTextColorMedium);

			// Draw high score
			canvas.drawText(highScoreDisplay, 1, 352, mTextColorMedium);
		}

		/**
		 * Generate random.
		 */
		private int generateRandom(int max) {

			return new Double(Math.floor(Math.random() * max)).intValue();
		}

		/**
		 * Create the board.
		 */
		private void createBoard() {

			balls.clear();
			ballsInQueue.clear();

			// Row 1
			int x = MARGIN_LEFT + STARTING_BALL_SPACING_LEFT;
			for (int i = 0; i < BALLS_IN_ROW; i++) {
				Ball ball = new Ball();
				ball.x = x;
				ball.y = MARGIN_TOP;
				ball.number = generateRandom(NEW_BALL_MAX) + 2;
				balls.add(ball);
				x += BALL_SPACING;
			}

			// Row 2
			x = MARGIN_LEFT + STARTING_BALL_SPACING_LEFT;
			for (int i = 0; i < BALLS_IN_ROW; i++) {
				Ball ball = new Ball();
				ball.x = x;
				ball.y = MARGIN_TOP + (BALL_RADIUS * 2); // Next row
				ball.number = generateRandom(NEW_BALL_MAX) + 2;
				balls.add(ball);
				x += BALL_SPACING;
			}

			// Balls in queue
			x = MARGIN_LEFT + STARTING_BALL_SPACING_LEFT;
			int y = QUEUE_Y;
			for (int i = 0; i < BALLS_IN_QUEUE; i++) {
				Ball ball = new Ball();
				ball.x = x;
				ball.y = y;
				ball.number = generateRandom(NEW_BALL_MAX) + 2;
				ballsInQueue.add(ball);
				x += BALL_SPACING;
			}
		}

		/**
		 * Increment ball.
		 */
		private void incrementBoard() {
			long now = System.currentTimeMillis();

			if (isGameOver) {
				stopGame();

				try {
					if (points > highScore) {
						highScore = points;

						setHighScoreDisplay();

						FileOutputStream fos = mContext.openFileOutput(
								RisingNumbers.HIGHSCORE_FILENAME,
								Context.MODE_PRIVATE);
						ObjectOutputStream out = new ObjectOutputStream(fos);
						out.writeObject(highScore);
						out.close();
					}

				} catch (Exception e) {
					Log.e(this.getClass().getName(),
							"Exception writing high score to file:"
									+ e.toString());
				}

				return;
			}

			// Keep ball movement at 20 milliseconds.
			if (mLastTime + ANIMATION_MILLIS >= now) {
				return;
			}

			if (!shooting) {
				currBall.x -= moveX;
				moveX = 0;

				currBall.y -= moveY;
				moveY = 0;

				// Keep in side
				if (currBall.x > MARGIN_RIGHT) {
					currBall.x = MARGIN_RIGHT;
				} else if (currBall.x < MARGIN_LEFT) {
					currBall.x = MARGIN_LEFT;
				}

				// Keep in top
				if (currBall.y < MARGIN_TOP) {
					currBall.y = MARGIN_TOP;
				}

				detectCollision();
			} else {
				boolean collision = false;
				while (!collision) {
					collision = detectCollision();
				}
				shooting = false;
			}

			mLastTime = now;
		}

		/**
		 * Detect collision between balls.
		 */
		private boolean detectBallCollision(Ball ball_1, Ball ball_2) {

			int dY = ball_1.y - ball_2.y;
			int dX = ball_1.x - ball_2.x;
			return Math.sqrt((dY * dY) + (dX * dX)) <= BALL_DISTANCE;
		}

		/**
		 * Detect collision with other balls or border.
		 */
		private boolean detectCollision() {

			boolean collision = false;

			// Detect collision with other balls
			for (int i = 0; i < balls.size(); i++) {
				Ball ball = balls.get(i);
				if (detectBallCollision(currBall, ball)) {
					collision = true;

					// If divides with no remainder, then divide target.
					if (ball.number % currBall.number == 0) {

						// Calculate new value
						int newValue = ball.number / currBall.number;

						// Update points
						points += ball.number - newValue;

						// Update ball
						ball.number = newValue;

						// If target is now 1, remove it as well.
						if (ball.number == 1) {
							balls.remove(i);

							// Add 1 to points
							points += 1;
						}

						setPointsDisplay();

						lastX = currBall.x;
						createNewBall();
					} else {
						// Add to current ball.
						currBall.number += ball.number;

						// Add to board.
						balls.add(currBall);

						// If over 99 or at bottom, game over!
						if (currBall.number > 99 || currBall.y > 260) {
							isGameOver = true;
						} else {
							lastX = currBall.x;
							createNewBall();
						}
					}

					break;
				}
			}

			// Check if collision with top
			if (!collision) {
				if (currBall.y <= MARGIN_TOP) {
					collision = true;
					balls.add(currBall);
					lastX = currBall.x;
					createNewBall();
				} else {
					currBall.y -= Y_DELTA;
				}
			}

			return collision;
		}
	}

	private GestureDetector gestureDetector;
	private View.OnTouchListener gestureListener;

	/** Handle to the application context, used to e.g. fetch Drawables. */
	private Context mContext;

	/** The thread that actually draws the animation */
	private GameThread thread;

	public GameView(Context context, AttributeSet attrs) {
		super(context, attrs);

		// Register our interest in hearing about changes to our surface
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);

		// Create thread only; it's started in surfaceCreated()
		thread = new GameThread(holder, context);

		setFocusable(true); // make sure we get key events
	}

	/**
	 * Fetches the animation thread corresponding to this view.
	 * 
	 * @return the animation thread
	 */
	public GameThread getThread() {
		return thread;
	}

	/**
	 * Standard override to get key-press events.
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent msg) {
		return thread.doKeyDown(keyCode, msg);
	}

	/**
	 * Standard window-focus override. Notice focus lost so we can pause on
	 * focus lost. e.g. user switches to take a call.
	 */
	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		if (!hasWindowFocus) {
			thread.pause();
		}
	}

	/*
	 * Callback invoked when the Surface has been created and is ready to be
	 * used.
	 */
	public void surfaceCreated(SurfaceHolder holder) {

		// If terminated, create a new thread and restore if there is a saved
		// game.
		if (thread.getState() == Thread.State.TERMINATED) {
			thread = new GameThread(holder, mContext);
			HashMap<String, Object> savedGame = RisingNumbers
					.getSavedGame(mContext);
			if (savedGame != null) {
				thread.restoreState(savedGame);
			}
			thread.mMode = GameThread.STATE_RUNNING;
		}

		// Gesture detection
		gestureDetector = new GestureDetector(new GameGestureDetector());
		gestureListener = new View.OnTouchListener() {

			public boolean onTouch(View v, MotionEvent event) {
				if (gestureDetector.onTouchEvent(event)) {
					return true;
				}
				return false;
			}
		};

		setOnTouchListener(gestureListener);

		// Start the thread here so that we don't busy-wait in run() waiting for
		// the surface to be created
		thread.setRunning(true);
		thread.start();
	}

	/*
	 * Callback invoked when the Surface has been destroyed and must no longer
	 * be touched. WARNING: after this method returns, the Surface/Canvas must
	 * never be touched again!
	 */
	public void surfaceDestroyed(SurfaceHolder holder) {
		thread.setRunning(false);
		try {
			thread.join();
		} catch (InterruptedException e) {
			Log.e(this.getClass().getName(), "Exception joining thread:"
					+ e.toString());
		}
	}

	public class GameGestureDetector extends SimpleOnGestureListener {

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			thread.setShooting(true);
			return false;
		}

		@Override
		public boolean onDown(MotionEvent e) {
			if (thread.mMode == GameThread.STATE_RUNNING) {

			}
			// If paused, resume.
			else if (thread.mMode == GameThread.STATE_PAUSE) {
				thread.unpause();
				return false;
			} // If game over, start new game.
			else if (thread.mMode == GameThread.STATE_OVER) {
				thread.setState(GameThread.STATE_RUNNING);
				thread.doStart();
				return false;
			}

			return true;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			// Don't let the ball go down.
			if (distanceY < 0) {
				distanceY = 0;
			}
			thread.setMove(distanceX, distanceY);
			return false;
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {

	}
}
