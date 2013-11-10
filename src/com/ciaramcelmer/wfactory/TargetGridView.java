package com.ciaramcelmer.wfactory;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class TargetGridView extends View implements OnTouchListener {

	public TargetGridView.LetterTouchedHandler _letterTouchedHandler = null;

	public abstract class LetterTouchedHandler {
		public abstract void handleLetterTouched(int index);
	}

	private static final int backgroundColor = 0xff800000;
	private static final int gridColor = Color.WHITE;
	private static final int centerBackgroundColor = 0xffff6060;
	private static final int centerLetterColor = Color.WHITE;
	private static final int letterColor = Color.WHITE;
	private static final int letterHighlightColor =  0xfff2d857;
	private static final int middleHighlightColor =  0xfff2d857;

	// Set paint objects
	private Paint backgroundPaint; // Overall background
	private Paint gridPaint; // Grid lines
	private Paint letterPaint; // Outside letters
	private Paint centerLetterPaint; // Center letter
	private Paint centerPaint; // Center background
	private Paint highlightPaint; // Square highlight
	private Paint middleHighlightPaint; // Middle square's highlight
	private int currentWidth; // Current width of view

	// An array to indicate which letters are displayed as highlighted
	private boolean[] highlights = { false, false, false, false, false, false,
			false, false, false };

	// Array of grid indices of selected letters
	private int[] selectedword = new int[] { -1, -1, -1, -1, -1, -1, -1, -1, -1 };

	// Variable to indicate the letters on the grid
	private String letters = "";

	public boolean gameActive = false;

	public Activity mContext;

	public TargetGridView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		initTargetView();
	}

	public TargetGridView(Context context, AttributeSet attrs) {
		// TODO Auto-generated constructor stub
		super(context, attrs);
		initTargetView();
	}

	public TargetGridView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initTargetView();
	}

	protected void initTargetView() {
		this.letters = "";
		this.gridPaint = new Paint();
		this.gridPaint.setColor(gridColor);

		this.backgroundPaint = new Paint();
		this.backgroundPaint.setColor(backgroundColor);

		this.letterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		this.letterPaint.setColor(letterColor);
		this.letterPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF,
				Typeface.BOLD));

		this.centerPaint = new Paint();
		this.centerPaint.setColor(centerBackgroundColor);

		this.centerLetterPaint = new Paint();
		this.centerLetterPaint.setColor(centerLetterColor);
		this.centerLetterPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF,
				Typeface.BOLD));

		this.highlightPaint = new Paint();
		this.highlightPaint.setColor(letterHighlightColor);

		this.middleHighlightPaint = new Paint();
		this.middleHighlightPaint.setColor(middleHighlightColor);

		this.currentWidth = 0;

		this.setOnTouchListener((OnTouchListener) this);
		this.gameActive = false;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// TODO Auto-generated method stub
		// super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		// Our target grid is a square, measuring 80% of the minimum dimension
		int measuredWidth = measure(widthMeasureSpec);
		int measuredHeight = measure(heightMeasureSpec);

		int dim = Math.min(measuredWidth, measuredHeight);

		setMeasuredDimension(dim, dim);
	}

	private int measure(int measureSpec) {
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);

		if (specMode == MeasureSpec.UNSPECIFIED)
			return 180;
		else
			return (int) (specSize * 0.8);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		// super.onDraw(canvas);
		int width = getMeasuredWidth();
		int height = getMeasuredHeight();

		if (width != this.currentWidth) {
			this.centerLetterPaint.setTextSize((int) (width / 5.5));
			this.letterPaint.setTextSize((int) (width / 5.5));
			this.gridPaint.setStrokeWidth(width / 50 + 1);
			this.currentWidth = width;
		}

		canvas.drawARGB(0, 255, 255, 255);

		if (!this.letters.equals(""))
			for (int index = 0; index < 9; index++)
				drawLetter(canvas, index, this.highlights[index]);

		for (float x = 0; x <= width; x += width / 3)
			canvas.drawLine(x, 0, x, height - 1, this.gridPaint);

		for (float y = 0; y <= height; y += height / 3)
			canvas.drawLine(0, y, width - 1, y, this.gridPaint);

	}

	public void drawLetter(Canvas canvas, int index, boolean highlighted) {
		Paint textPaint;
		Paint squarePaint;

		int size = getMeasuredWidth(); // Measure one as its a square

		String letter = this.letters.substring(index, index + 1);
		float squareLeft = (index % 3) * size / 3;
		float squareTop = (float) Math.floor(index / 3) * size / 3;
		float squareSize = size / 3;
		float letterWidth = this.letterPaint.measureText(letter);
		float letterHeight = this.letterPaint.ascent();

		if (!highlighted) {
		
			// Modified by me 10-23-2013
			//	if (index == 4) {
			if (index == NineLetterWord.index1) {
				textPaint = this.centerLetterPaint;
				squarePaint = this.centerPaint;
			} else {
				textPaint = this.letterPaint;
				squarePaint = this.backgroundPaint;
			}
		} else {
			// Modified by me 10-23-2013
			//	if (index == 4) 
			if (index == NineLetterWord.index1)
				squarePaint = this.middleHighlightPaint;
			else
				squarePaint = this.highlightPaint;
			textPaint = this.letterPaint;
		}

		canvas.drawRect(squareLeft, squareTop, squareLeft + squareSize,
				squareTop + squareSize, squarePaint);
		canvas.drawText(letter, squareLeft + squareSize / 2 - letterWidth / 2,
				squareTop + squareSize / 2 - letterHeight / 2, textPaint);
	}

	// Supplies a new word to the grid
	// Arg must be a 9 letter string, filling the grid L-R, T-B
	// Words that aren't 9 letters are ignored

	public void setLetters(String word) {
		// this.letters = letters;
		if (word.length() == 9) {
			this.letters = word;
			clearGrid();
		}
	}

	// Unhighlights the entire grid
	public void clearGrid() {
		// TODO Auto-generated method stub
		this.highlights = new boolean[] { false, false, false, false, false,
				false, false, false, false };
		this.selectedword = new int[] { -1, -1, -1, -1, -1, -1, -1, -1, -1

		};
		invalidate();
	}

	// Clears (unhighlights) the most recently selected letter from the grid.
	public void clearLastLetter() {
		int gridIndex;
		for (int i = 8; i >= 0; i--) {
			gridIndex = this.selectedword[i];
			if (gridIndex != -1) {
				this.highlights[gridIndex] = false;
				this.selectedword[i] = -1;
				invalidate();
				return;
			}
		}
	}

	public String getSelectedWord() {
		String word = "";
		int gridIndex;
		for (int i = 0; i < 9; i++) {
			gridIndex = this.selectedword[i];
			if (gridIndex > -1)
				word += this.letters.substring(gridIndex, gridIndex + 1);
			else
				return word;
		}
		return word;
	}

	public void setLetterTouchedListener(LetterTouchedHandler handler) {
		this._letterTouchedHandler = handler;
	}

	// Handles touch events to the grid
	// Marks the letter to be highlighted, and updates the
	// Ordered list of selected letters
	// Finally calls the LetterTouchedHandler for further actions

	public boolean onTouch(View v, MotionEvent event) {
		if (this.gameActive == false)
			return false;
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN: {
			int gridIndex = eventToLetterIndex(event);
			if (this.highlights[gridIndex]) {
				unSelectIfLastLetter(gridIndex);
				return true; // return if letter already highlighted
			}
			this.highlights[gridIndex] = true;
			invalidate();
			for (int i = 0; i < 9; i++) {
				if (this.selectedword[i] == -1) {
					this.selectedword[i] = gridIndex;
					break;
				}
			}
			if (this._letterTouchedHandler != null)
				this._letterTouchedHandler.handleLetterTouched(gridIndex);
			return true;
		}
		}
		return false;
	}

	private int eventToLetterIndex(MotionEvent event) {
		// TODO Auto-generated method stub
		float x = event.getX();
		float y = event.getY();
		int size = getMeasuredWidth(); // Measure one side only as its a square

		int row = (int) ((size - (size - y)) / (size / 3));
		if (row > 2)
			row = 2;
		if (row < 0)
			row = 0;

		int col = (int) ((size - (size - x)) / (size / 3));
		if (col > 2)
			col = 2;
		if (col < 0)
			col = 0;

		int index = row * 3 + col;

		return index;
	}

	private void unSelectIfLastLetter(int gridIndex) {
		int lastIndex = -2;
		int i;
		for (i = 0; i < 9; i++) {
			if (this.selectedword[i] == -1) {
				lastIndex = i - 1;
				break;
			}
		}
		if (lastIndex == -2 && i == 9)
			lastIndex = 8;

		if (this.selectedword[lastIndex] == gridIndex) {
			clearLastLetter();
			this._letterTouchedHandler.handleLetterTouched(gridIndex);
		}

	}

}
