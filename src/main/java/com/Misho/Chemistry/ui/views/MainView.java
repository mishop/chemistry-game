package com.Misho.Chemistry.ui.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.Misho.Chemistry.R;
import com.Misho.Chemistry.input.InputListener;
import com.Misho.Chemistry.model.Tile;
import com.Misho.Chemistry.ui.activities.GameActivity;
import com.Misho.Chemistry.utils.ChemistryElementsHolder;
import com.Misho.Chemistry.viewModel.AnimationCell;
import com.Misho.Chemistry.viewModel.MainGame;

import java.util.ArrayList;

@SuppressWarnings("deprecation")
public class MainView extends View {

  // Internal Constants
  public static final int BASE_ANIMATION_TIME = 100000000;
  private static final String TAG = MainView.class.getSimpleName();
  private static final float MERGING_ACCELERATION = (float) -0.5;
  private static final float INITIAL_VELOCITY = (1 - MERGING_ACCELERATION) / 4;
  public final int numCellTypes = 21;
  public final MainGame game;
  private final BitmapDrawable[] bitmapCell = new BitmapDrawable[numCellTypes];
  // Internal variables
  private final Paint paint = new Paint();
  public boolean hasSaveState = false;
  public boolean continueButtonEnabled = false;
  public int startingX;
  public int startingY;
  public int endingX;
  public int endingY;
  // Icon variables
  public int sYIcons;
  public int sXNewGame;
  public int iconSize;
  // Misc
  public boolean refreshLastTime = true;
  private boolean isGameOver;
  private boolean isGameWin;
  // Timing
  private long lastFPSTime = System.nanoTime();

  // Text
  private float titleTextSize;
  private float bodyTextSize;
  private float gameOverTextSize;

  // Layout variables
  private int cellSize = 0;
  private float textSize = 0;
  private float cellTextSize = 0;
  private int gridWidth = 0;
  private int textPaddingSize;
  private int iconPaddingSize;

  // Assets
  private Drawable backgroundRectangle;
  private Drawable fadeRectangle;
  private Drawable winRectangle;
  private Bitmap background = null;
  private BitmapDrawable loseGameOverlay;
  private BitmapDrawable winGameFinalOverlay;

  // Text variables
  private int sYAll;
  private int titleStartYAll;
  private int bodyStartYAll;
  private int eYAll;
  private int titleWidthHighScore;
  private int titleWidthScore;

  private Typeface boldFont;
  private Typeface regularFont;

  private GameActivity activity;

  public MainView(Context context, AppCompatActivity activity) {
    super(context);

    Resources resources = context.getResources();
    //Loading resources
    game = new MainGame(context, activity, this);
    this.activity = (GameActivity) activity;
    try {
      //Getting assets
      backgroundRectangle = resources.getDrawable(R.drawable.cells_background);
      fadeRectangle = resources.getDrawable(R.drawable.cell_fade);
      winRectangle = resources.getDrawable(R.drawable.cell_win);
      this.setBackgroundColor(resources.getColor(R.color.color_background));
      boldFont = Typeface.createFromAsset(resources.getAssets(), "ClearSans-Bold.ttf");
      regularFont = Typeface.createFromAsset(resources.getAssets(), "ClearSans-Regular.ttf");
      paint.setAntiAlias(true);
    } catch (Exception e) {
      Log.e(TAG, "Error getting assets?", e);
    }
    setOnTouchListener(new InputListener(this));
    game.newGame();
  }

  private static int log2(int n) {
    if (n <= 0) {
      throw new IllegalArgumentException();
    }
    return 31 - Integer.numberOfLeadingZeros(n);
  }

  public GameActivity getActivity() {
    return activity;
  }

  @Override
  public void onDraw(Canvas canvas) {
    //Reset the transparency of the screen

    canvas.drawBitmap(background, 0, 0, paint);

    drawScoreText(canvas);

    if (!game.isActive() && !game.aGrid.isAnimationActive()) {
      drawNewGameButton(canvas);
    }

    drawCells(canvas);

    if (!game.isActive()) {
      drawEndGameState(canvas);
    }

    //Refresh the screen if there is still an animation running
    if (game.aGrid.isAnimationActive()) {
      invalidate(startingX, startingY, endingX, endingY);
      tick();
      //Refresh one last time on game end.
    } else if (!game.isActive() && refreshLastTime) {
      invalidate();
      refreshLastTime = false;
    }
  }

  @Override
  protected void onSizeChanged(int width, int height, int oldW, int oldH) {
    super.onSizeChanged(width, height, oldW, oldH);
    getLayout(width, height);
    createBitmapCells();
    createBackgroundBitmap(width, height);
    createOverlays();
  }

  private void drawDrawable(Canvas canvas, Drawable draw, int startingX, int startingY, int endingX,
      int endingY) {
    draw.setBounds(startingX, startingY, endingX, endingY);
    draw.draw(canvas);
  }

  private void drawCellText(Canvas canvas, int value) {
    int textShiftY = centerText();
    paint.setTypeface(regularFont);
    if (value >= 8) {
      paint.setColor(getResources().getColor(R.color.text_white));
    } else {
      paint.setColor(getResources().getColor(R.color.text_black));
    }
    canvas.drawText(ChemistryElementsHolder.getElementByTwoPow(value), cellSize / 2,
        cellSize / 2 - textShiftY, paint);
  }

  private void drawScoreText(Canvas canvas) {
    // Drawing the score text: Ver 2
    paint.setTextSize(bodyTextSize);
    paint.setTypeface(boldFont);
    paint.setTextAlign(Paint.Align.CENTER);

    int bodyWidthHighScore = (int) (paint.measureText("" + game.highScore));
    int bodyWidthScore = (int) (paint.measureText("" + game.score));

    int textWidthHighScore =
        Math.max(titleWidthHighScore, bodyWidthHighScore) + textPaddingSize * 2;
    int textWidthScore = Math.max(titleWidthScore, bodyWidthScore) + textPaddingSize * 2;

    int textMiddleHighScore = textWidthHighScore / 2;
    int textMiddleScore = textWidthScore / 2;

    int eXHighScore = endingX - 25;
    int sXHighScore = eXHighScore - textWidthHighScore;

    int eXScore = sXHighScore - textPaddingSize - 25;
    int sXScore = eXScore - textWidthScore;

    //Outputting high-scores box
    backgroundRectangle.setBounds(sXHighScore, sYAll - 25, eXHighScore, eYAll - 25);
    backgroundRectangle.draw(canvas);
    paint.setTextSize(titleTextSize);
    paint.setColor(getResources().getColor(R.color.text_brown));
    canvas.drawText(getResources().getString(R.string.high_score),
        sXHighScore + textMiddleHighScore, titleStartYAll - 25, paint);
    paint.setTextSize(bodyTextSize);
    paint.setColor(getResources().getColor(R.color.text_white));
    canvas.drawText(String.valueOf(game.highScore), sXHighScore + textMiddleHighScore,
        bodyStartYAll - 25, paint);

    // Outputting scores box
    backgroundRectangle.setBounds(sXScore + 15, sYAll - 25, eXScore + 15, eYAll - 25);
    backgroundRectangle.draw(canvas);
    paint.setTextSize(titleTextSize);
    paint.setColor(getResources().getColor(R.color.text_brown));
    canvas.drawText(getResources().getString(R.string.score), sXScore + textMiddleScore + 15,
        titleStartYAll - 25, paint);
    paint.setTextSize(bodyTextSize);
    paint.setColor(getResources().getColor(R.color.text_white));
    canvas.drawText(String.valueOf(game.score), sXScore + textMiddleScore + 15, bodyStartYAll - 25,
        paint);
  }

  private void drawNewGameButton(Canvas canvas) {

    drawDrawable(canvas,
        backgroundRectangle,
        sXNewGame,
        sYIcons, sXNewGame + iconSize,
        sYIcons + iconSize
    );

    drawDrawable(canvas,
        getResources().getDrawable(R.drawable.ic_refresh_white_48dp),
        sXNewGame + iconPaddingSize,
        sYIcons + iconPaddingSize,
        sXNewGame + iconSize - iconPaddingSize,
        sYIcons + iconSize - iconPaddingSize
    );
  }

  // Renders the set of 16 background squares.
  private void drawBackgroundGrid(Canvas canvas) {
    Resources resources = getResources();
    Drawable backgroundCell = resources.getDrawable(R.drawable.cell);
    // Outputting the game grid
    for (int xx = 0; xx < game.numSquaresX; xx++) {
      for (int yy = 0; yy < game.numSquaresY; yy++) {
        int sX = startingX + gridWidth + (cellSize + gridWidth) * xx;
        int eX = sX + cellSize;
        int sY = startingY + gridWidth + (cellSize + gridWidth) * yy;
        int eY = sY + cellSize;

        drawDrawable(canvas, backgroundCell, sX, sY, eX, eY);
      }
    }
  }

  private void drawCells(Canvas canvas) {
    paint.setTextSize(textSize);
    paint.setTextAlign(Paint.Align.CENTER);
    // Outputting the individual cells
    for (int xx = 0; xx < game.numSquaresX; xx++) {
      for (int yy = 0; yy < game.numSquaresY; yy++) {
        int sX = startingX + gridWidth + (cellSize + gridWidth) * xx;
        int eX = sX + cellSize;
        int sY = startingY + gridWidth + (cellSize + gridWidth) * yy;
        int eY = sY + cellSize;

        Tile currentTile = game.grid.getCellContent(xx, yy);
        if (currentTile != null) {
          //Get and represent the value of the tile
          int value = currentTile.getValue();
          int index = log2(value);

          //Check for any active animations
          ArrayList<AnimationCell> aArray = game.aGrid.getAnimationCell(xx, yy);
          boolean animated = false;
          for (int i = aArray.size() - 1; i >= 0; i--) {
            AnimationCell aCell = aArray.get(i);
            //If this animation is not active, skip it
            if (aCell.getAnimationType() == MainGame.SPAWN_ANIMATION) {
              animated = true;
            }
            if (!aCell.isActive()) {
              continue;
            }

            if (aCell.getAnimationType() == MainGame.SPAWN_ANIMATION) { // Spawning animation
              double percentDone = aCell.getPercentageDone();
              float textScaleSize = (float) (percentDone);
              paint.setTextSize(textSize * textScaleSize);

              float cellScaleSize = cellSize / 2 * (1 - textScaleSize);
              bitmapCell[index].setBounds((int) (sX + cellScaleSize), (int) (sY + cellScaleSize),
                  (int) (eX - cellScaleSize), (int) (eY - cellScaleSize));
              bitmapCell[index].draw(canvas);
            } else if (aCell.getAnimationType() == MainGame.MERGE_ANIMATION) { // Merging Animation
              double percentDone = aCell.getPercentageDone();
              float textScaleSize = (float) (1 + INITIAL_VELOCITY * percentDone
                  + MERGING_ACCELERATION * percentDone * percentDone / 2);
              paint.setTextSize(textSize * textScaleSize);

              float cellScaleSize = cellSize / 2 * (1 - textScaleSize);
              bitmapCell[index].setBounds((int) (sX + cellScaleSize), (int) (sY + cellScaleSize),
                  (int) (eX - cellScaleSize), (int) (eY - cellScaleSize));
              bitmapCell[index].draw(canvas);
            } else if (aCell.getAnimationType() == MainGame.MOVE_ANIMATION) {  // Moving animation
              double percentDone = aCell.getPercentageDone();
              int tempIndex = index;
              if (aArray.size() >= 2) {
                tempIndex = tempIndex - 1;
              }
              int previousX = aCell.extras[0];
              int previousY = aCell.extras[1];
              int currentX = currentTile.getX();
              int currentY = currentTile.getY();
              int dX = (int) ((currentX - previousX) * (cellSize + gridWidth) * (percentDone - 1)
                  * 1.0);
              int dY = (int) ((currentY - previousY) * (cellSize + gridWidth) * (percentDone - 1)
                  * 1.0);
              bitmapCell[tempIndex].setBounds(sX + dX, sY + dY, eX + dX, eY + dY);
              bitmapCell[tempIndex].draw(canvas);
            }
            animated = true;
          }

          // No active animations? Just draw the cell
          if (!animated) {
            bitmapCell[index].setBounds(sX, sY, eX, eY);
            bitmapCell[index].draw(canvas);
          }
        }
      }
    }
  }

  private void drawEndGameState(Canvas canvas) {
    double alphaChange = 1;
    continueButtonEnabled = false;
    for (AnimationCell animation : game.aGrid.globalAnimation) {
      if (animation.getAnimationType() == MainGame.FADE_GLOBAL_ANIMATION) {
        alphaChange = animation.getPercentageDone();
      }
    }
    BitmapDrawable displayOverlay = null;
    if (game.gameWon()) {
      displayOverlay = winGameFinalOverlay;
    } else if (game.gameLost()) {
      isGameOver = true;
      displayOverlay = loseGameOverlay;
    }

    if (displayOverlay != null) {
      displayOverlay.setBounds(startingX, startingY, endingX, endingY);
      displayOverlay.setAlpha((int) (255 * alphaChange));
      displayOverlay.draw(canvas);
    }
  }

  private void createEndGameStates(Canvas canvas, boolean win, boolean showButton) {
    int width = endingX - startingX;
    int length = endingY - startingY;
    if (win) {
      winRectangle.setAlpha(127);
      drawDrawable(canvas, winRectangle, 10, 10, width - 10, length - 10);
      winRectangle.setAlpha(255);
    } else {
      fadeRectangle.setAlpha(127);
      drawDrawable(canvas, fadeRectangle, 10, 10, width - 10, length - 10);
      fadeRectangle.setAlpha(255);
    }
  }

  public boolean isGameOver() {
    return isGameOver;
  }

  public void setGameOver(boolean gameOver) {
    isGameOver = gameOver;
  }

  public boolean isGameWin() { return isGameWin; }

  public void setGameWin(boolean gameWin) {isGameWin = gameWin; }

  private void createBackgroundBitmap(int width, int height) {
    background = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(background);
    drawNewGameButton(canvas);
    drawBackgroundGrid(canvas);
  }

  private void createBitmapCells() {
    Resources resources = getResources();
    int[] cellRectangleIds = getCellRectangleIds();
    paint.setTextAlign(Paint.Align.CENTER);
    for (int xx = 1; xx < bitmapCell.length; xx++) {
      int value = (int) Math.pow(2, xx);
      paint.setTextSize(cellTextSize);
      float tempTextSize = cellTextSize * cellSize * 0.9f / Math.max(cellSize * 0.9f,
          paint.measureText(String.valueOf(value)));
      paint.setTextSize(tempTextSize);
      Bitmap bitmap = Bitmap.createBitmap(cellSize, cellSize, Bitmap.Config.ARGB_8888);
      Canvas canvas = new Canvas(bitmap);
      drawDrawable(canvas, resources.getDrawable(cellRectangleIds[xx]), 0, 0, cellSize, cellSize);
      drawCellText(canvas, value);
      bitmapCell[xx] = new BitmapDrawable(resources, bitmap);
    }
  }

  private int[] getCellRectangleIds() {
    int[] cellRectangleIds = new int[numCellTypes];
    cellRectangleIds[0] = R.drawable.cell;
    cellRectangleIds[1] = R.drawable.cell_2;
    cellRectangleIds[2] = R.drawable.cell_4;
    cellRectangleIds[3] = R.drawable.cell_8;
    cellRectangleIds[4] = R.drawable.cell_16;
    cellRectangleIds[5] = R.drawable.cell_32;
    cellRectangleIds[6] = R.drawable.cell_64;
    cellRectangleIds[7] = R.drawable.cell_128;
    cellRectangleIds[8] = R.drawable.cell_256;
    cellRectangleIds[9] = R.drawable.cell_512;
    cellRectangleIds[10] = R.drawable.cell_1024;
    cellRectangleIds[11] = R.drawable.cell_2048;
    for (int xx = 12; xx < cellRectangleIds.length; xx++) {
      cellRectangleIds[xx] = R.drawable.cell_4096;
    }
    return cellRectangleIds;
  }

  private void createOverlays() {
    Resources resources = getResources();
    //Initialize overlays
    Bitmap bitmap = Bitmap.createBitmap(endingX - startingX, endingY - startingY,
        Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    createEndGameStates(canvas, true, true);
    bitmap = Bitmap.createBitmap(endingX - startingX, endingY - startingY, Bitmap.Config.ARGB_8888);
    canvas = new Canvas(bitmap);
    createEndGameStates(canvas, true, false);
    winGameFinalOverlay = new BitmapDrawable(resources, bitmap);
    bitmap = Bitmap.createBitmap(endingX - startingX, endingY - startingY, Bitmap.Config.ARGB_8888);
    canvas = new Canvas(bitmap);
    createEndGameStates(canvas, false, false);
    loseGameOverlay = new BitmapDrawable(resources, bitmap);
  }

  private void tick() {
    long currentTime = System.nanoTime();
    game.aGrid.tickAll(currentTime - lastFPSTime);
    lastFPSTime = currentTime;
  }

  public void resyncTime() {
    lastFPSTime = System.nanoTime();
  }

  private void getLayout(int width, int height) {
    cellSize = Math.min(width / (game.numSquaresX + 1), height / (game.numSquaresY + 3));
    gridWidth = cellSize / 7;
    int screenMiddleX = width / 2;
    int screenMiddleY = height / 2;
    int boardMiddleY = screenMiddleY + cellSize / 2;
    iconSize = cellSize / 2;

    //Grid Dimensions
    double halfNumSquaresX = game.numSquaresX / 2d;
    double halfNumSquaresY = game.numSquaresY / 2d;
    startingX = (int) (screenMiddleX - (cellSize + gridWidth) * halfNumSquaresX - gridWidth / 2);
    endingX = (int) (screenMiddleX + (cellSize + gridWidth) * halfNumSquaresX + gridWidth / 2);
    startingY = (int) (boardMiddleY - (cellSize + gridWidth) * halfNumSquaresY - gridWidth / 2);
    endingY = (int) (boardMiddleY + (cellSize + gridWidth) * halfNumSquaresY + gridWidth / 2);

    float widthWithPadding = endingX - startingX;

    // Text Dimensions
    paint.setTextSize(cellSize);
    textSize = cellSize * cellSize / Math.max(cellSize, paint.measureText("0000"));

    paint.setTextAlign(Paint.Align.CENTER);
    paint.setTextSize(1000);
    gameOverTextSize = Math.min(
        Math.min(
            1000f * ((widthWithPadding - gridWidth * 2) / (paint.measureText(
                getResources().getString(R.string.game_over)))),
            textSize * 2
        ),
        1000f * ((widthWithPadding - gridWidth * 2) / (paint.measureText(
            getResources().getString(R.string.you_win))))
    );

    paint.setTextSize(cellSize);
    cellTextSize = textSize;
    titleTextSize = textSize / 3;
    bodyTextSize = (int) (textSize / 1.5);
    textPaddingSize = (int) (textSize / 3);
    iconPaddingSize = (int) (textSize / 5);

    paint.setTextSize(titleTextSize);

    int textShiftYAll = centerText();
    // static variables
    sYAll = (int) (startingY - cellSize * 1.5);
    titleStartYAll = (int) (sYAll + textPaddingSize + titleTextSize / 2 - textShiftYAll);
    bodyStartYAll = (int) (titleStartYAll + textPaddingSize + titleTextSize / 2 + bodyTextSize / 2);

    titleWidthHighScore = (int) (paint.measureText(getResources().getString(R.string.high_score)));
    titleWidthScore = (int) (paint.measureText(getResources().getString(R.string.score)));
    paint.setTextSize(bodyTextSize);
    textShiftYAll = centerText();
    eYAll = (int) (bodyStartYAll + textShiftYAll + bodyTextSize / 2 + textPaddingSize);

    sYIcons = (startingY + eYAll) / 2 - iconSize / 2;
    sXNewGame = (endingX - iconSize) - 25;
    resyncTime();
  }

  private int centerText() {
    return (int) ((paint.descent() + paint.ascent()) / 2);
  }

}