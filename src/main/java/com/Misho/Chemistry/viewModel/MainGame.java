package com.Misho.Chemistry.viewModel;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;

import com.Misho.Chemistry.model.Cell;
import com.Misho.Chemistry.model.Grid;
import com.Misho.Chemistry.model.Tile;
import com.Misho.Chemistry.ui.activities.GameActivity;
import com.Misho.Chemistry.ui.views.MainView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainGame {

  private AppCompatActivity activity;

  public static final int SPAWN_ANIMATION = -1;
  public static final int MOVE_ANIMATION = 0;
  public static final int MERGE_ANIMATION = 1;

  public static final int FADE_GLOBAL_ANIMATION = 0;
  private static final long MOVE_ANIMATION_TIME = MainView.BASE_ANIMATION_TIME;
  private static final long SPAWN_ANIMATION_TIME = MainView.BASE_ANIMATION_TIME;
  private static final long NOTIFICATION_DELAY_TIME = MOVE_ANIMATION_TIME + SPAWN_ANIMATION_TIME;
  private static final long NOTIFICATION_ANIMATION_TIME = MainView.BASE_ANIMATION_TIME * 5;
  private static final int startingMaxValue = 2048;

  // Odd state = game is not active
  // Even state = game is active
  // Win state = active state + 1
  private static final int GAME_WIN = 1;
  private static final int GAME_LOST = -1;
  private static final int GAME_NORMAL = 0;
  public int gameState = GAME_NORMAL;
  public int lastGameState = GAME_NORMAL;
  private int bufferGameState = GAME_NORMAL;
  private static final String HIGH_SCORE = "high score";
  public final int numSquaresX = 4;
  public final int numSquaresY = 4;
  private final Context mContext;
  private final MainView mView;
  public Grid grid = null;
  public AnimationGrid aGrid;
  public boolean canUndo;
  public long score = 0;
  public long highScore = 0;
  public long lastScore = 0;
  private long bufferScore = 0;
  private boolean canContinue;

  public MainGame(Context context, AppCompatActivity activity, MainView view) {
    mContext = context;
    mView = view;
    this.activity = activity;
  }

  public void newGame() {
    if (grid == null) {
      grid = new Grid(numSquaresX, numSquaresY);
    } else {
      prepareUndoState();
      saveUndoState();
      grid.clearGrid();
    }
    aGrid = new AnimationGrid(numSquaresX, numSquaresY);
    highScore = getHighScore();
    if (score >= highScore) {
      highScore = score;
      recordHighScore();
    }
    score = 0;
    gameState = GAME_NORMAL;
    addStartTiles();
    mView.refreshLastTime = true;
    mView.resyncTime();
    mView.invalidate();
  }

  private void addStartTiles() {
    int startTiles = 2;
    for (int xx = 0; xx < startTiles; xx++) {
      this.addRandomTile();
    }
  }

  private void addRandomTile() {
    if (grid.isCellsAvailable()) {
      int value = Math.random() < 0.9 ? 2 : 4;
      Tile tile = new Tile(grid.randomAvailableCell(), value);
      spawnTile(tile);
    }
  }

  private void spawnTile(Tile tile) {
    grid.insertTile(tile);
    aGrid.startAnimation(tile.getX(), tile.getY(), SPAWN_ANIMATION,
        SPAWN_ANIMATION_TIME, MOVE_ANIMATION_TIME, null); //Direction: -1 = EXPANDING
  }

  private void recordHighScore() {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
    SharedPreferences.Editor editor = settings.edit();
    editor.putLong(HIGH_SCORE, highScore);
    editor.apply();
  }

  public long getHighScore() {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
    return settings.getLong(HIGH_SCORE, -1);
  }

  private void prepareTiles() {
    for (Tile[] array : grid.field) {
      for (Tile tile : array) {
        if (grid.isCellOccupied(tile)) {
          tile.setMergedFrom(null);
        }
      }
    }
  }

  private void moveTile(Tile tile, Cell cell) {
    grid.field[tile.getX()][tile.getY()] = null;
    grid.field[cell.getX()][cell.getY()] = tile;
    tile.updatePosition(cell);
  }

  private void saveUndoState() {
    grid.saveTiles();
    canUndo = true;
    lastScore = bufferScore;
    lastGameState = bufferGameState;
  }

  private void prepareUndoState() {
    grid.prepareSaveTiles();
    bufferScore = score;
    bufferGameState = gameState;
  }

  public boolean gameWon() {
    return (gameState > 0 && gameState % 2 != 0);
  }

  public boolean gameLost() {
    return (gameState == GAME_LOST);
  }

  public boolean isActive() {
    return !(gameWon() || gameLost());
  }

  public void move(int direction) {
    aGrid.cancelAnimations();
    // 0: up, 1: right, 2: down, 3: left
    if (!isActive()) {
      return;
    }
    prepareUndoState();
    Cell vector = getVector(direction);
    List<Integer> traversalsX = buildTraversalsX(vector);
    List<Integer> traversalsY = buildTraversalsY(vector);
    boolean moved = false;

    prepareTiles();

    for (int xx : traversalsX) {
      for (int yy : traversalsY) {
        Cell cell = new Cell(xx, yy);
        Tile tile = grid.getCellContent(cell);

        if (tile != null) {
          Cell[] positions = findFarthestPosition(cell, vector);
          Tile next = grid.getCellContent(positions[1]);

          if (next != null && next.getValue() == tile.getValue() && next.getMergedFrom() == null) {
            Tile merged = new Tile(positions[1], tile.getValue() * 2);
            Tile[] temp = { tile, next };
            merged.setMergedFrom(temp);

            grid.insertTile(merged);
            grid.removeTile(tile);

            // Converge the two tiles' positions
            tile.updatePosition(positions[1]);

            int[] extras = { xx, yy };
            aGrid.startAnimation(merged.getX(), merged.getY(), MOVE_ANIMATION,
                MOVE_ANIMATION_TIME, 0, extras); //Direction: 0 = MOVING MERGED
            aGrid.startAnimation(merged.getX(), merged.getY(), MERGE_ANIMATION,
                SPAWN_ANIMATION_TIME, MOVE_ANIMATION_TIME, null);

            // Update the score
            score = score + merged.getValue();
            highScore = Math.max(score, highScore);

            // The mighty 2048 tile
            if (merged.getValue() >= winValue() && !gameWon()) {
              gameState = gameState + GAME_WIN; // Set win state
              mView.setGameWin(true);
              if (movesAvailable()) {
                canContinue = true;
                pauseGame();
              } else {
                canContinue = false;
                endGame();
              }
            }
          } else {
            moveTile(tile, positions[0]);
            int[] extras = { xx, yy, 0 };
            aGrid.startAnimation(positions[0].getX(), positions[0].getY(), MOVE_ANIMATION,
                MOVE_ANIMATION_TIME, 0, extras); // Direction: 1 = MOVING NO MERGE
          }

          if (!positionsEqual(cell, tile)) {
            moved = true;
          }
        }
      }
    }

    if (moved) {
      saveUndoState();
      addRandomTile();
      checkLose();
    }
    mView.resyncTime();
    mView.invalidate();
  }

  private void checkLose() {
    if (!movesAvailable() && !gameWon()) {
      gameState = GAME_LOST;
      mView.setGameOver(true);
      endGame();
    }
  }

  public boolean isCanContinue() {
    return canContinue;
  }

  private void endGame() {
    playAnimation();
    recordScores();

    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
    SharedPreferences.Editor editor = settings.edit();
    int totalGamesCount = settings.getInt(GameActivity.TOTAL_GAMES, 0) + 1;
    editor.putInt(GameActivity.TOTAL_GAMES, totalGamesCount);
    editor.apply();
    int totalReward = GameActivity.REWARD - 1;
    ((GameActivity) activity).submitScore((int) highScore);
    if (totalReward <= 0) {
      ((GameActivity) activity).showInterstitial();
    }
    ((GameActivity) activity).showGameOverViews();
    mView.setGameWin(false);
  }

  private void pauseGame() {
    playAnimation();
    recordScores();
    ((GameActivity) activity).showGameOverViews();
  }

  private void recordScores() {
    if (score >= highScore) {
      highScore = score;
      recordHighScore();
    }
  }

  private void playAnimation() {
    aGrid.startAnimation(-1, -1, FADE_GLOBAL_ANIMATION, NOTIFICATION_ANIMATION_TIME,
        NOTIFICATION_DELAY_TIME, null);
  }

  public void setCanContinue(boolean canContinue) {
    this.canContinue = canContinue;
  }

  private Cell getVector(int direction) {
    Cell[] map = {
        new Cell(0, -1), // up
        new Cell(1, 0),  // right
        new Cell(0, 1),  // down
        new Cell(-1, 0)  // left
    };
    return map[direction];
  }

  private List<Integer> buildTraversalsX(Cell vector) {
    List<Integer> traversals = new ArrayList<>();

    for (int xx = 0; xx < numSquaresX; xx++) {
      traversals.add(xx);
    }
    if (vector.getX() == 1) {
      Collections.reverse(traversals);
    }

    return traversals;
  }

  private List<Integer> buildTraversalsY(Cell vector) {
    List<Integer> traversals = new ArrayList<>();

    for (int xx = 0; xx < numSquaresY; xx++) {
      traversals.add(xx);
    }
    if (vector.getY() == 1) {
      Collections.reverse(traversals);
    }

    return traversals;
  }

  private Cell[] findFarthestPosition(Cell cell, Cell vector) {
    Cell previous;
    Cell nextCell = new Cell(cell.getX(), cell.getY());
    do {
      previous = nextCell;
      nextCell = new Cell(previous.getX() + vector.getX(),
          previous.getY() + vector.getY());
    } while (grid.isCellWithinBounds(nextCell) && grid.isCellAvailable(nextCell));

    return new Cell[] { previous, nextCell };
  }

  private boolean movesAvailable() {
    return grid.isCellsAvailable() || tileMatchesAvailable();
  }

  private boolean tileMatchesAvailable() {
    Tile tile;

    for (int xx = 0; xx < numSquaresX; xx++) {
      for (int yy = 0; yy < numSquaresY; yy++) {
        tile = grid.getCellContent(new Cell(xx, yy));

        if (tile != null) {
          for (int direction = 0; direction < 4; direction++) {
            Cell vector = getVector(direction);
            Cell cell = new Cell(xx + vector.getX(), yy + vector.getY());

            Tile other = grid.getCellContent(cell);

            if (other != null && other.getValue() == tile.getValue()) {
              return true;
            }
          }
        }
      }
    }

    return false;
  }

  private boolean positionsEqual(Cell first, Cell second) {
    return first.getX() == second.getX() && first.getY() == second.getY();
  }

  private int winValue() {
    return startingMaxValue;
  }

}
