package com.Misho.Chemistry.ui.activities;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.Misho.Chemistry.R;
import com.Misho.Chemistry.model.Tile;
import com.Misho.Chemistry.ui.views.MainView;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.gms.games.PlayersClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.Games;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.OnUserEarnedRewardListener;

public class GameActivity extends AppCompatActivity implements OnUserEarnedRewardListener {

  public static int REWARD = 1;
  public static final String TOTAL_GAMES = "total_games";
  private static final String WIDTH = "width";
  private static final String HEIGHT = "height";
  private static final String SCORE = "score";
  private static final String HIGH_SCORE = "high score temp";
  private static final String UNDO_SCORE = "undo score";
  private static final String CAN_UNDO = "can undo";
  private static final String UNDO_GRID = "undo";
  private static final String GAME_STATE = "game state";
  private static final String UNDO_GAME_STATE = "undo game state";
  private static final String FIRST_LAUNCH = "first_launch";
  private static final String GAME_OVER = "game_over";
  private static final String WIN = "win_game";
  private static final String CAN_CONTINUE = "can_continue";

  private MainView view;
  private MaterialDialog dialog;
  private Button gameOverButton;
  private TextView winTextView;
  private Typeface typeface;

  private FirebaseAnalytics mFirebaseAnalytics;
  private InterstitialAd mInterstitialAd;
  private AdView mAdView;
  private GoogleSignInClient mGoogleSignInClient;
  private LeaderboardsClient mLeaderboardsClient;
  private PlayersClient mPlayersClient;
  private RewardedInterstitialAd rewardedInterstitialAd;
  private String TAG1 = "MainActivity";
  private static final String TAG=GameActivity.class.getSimpleName();

  private static final int RC_SIGN_IN = 9001;
  private static final int RC_UNUSED = 5001;
  private static final int RC_LEADERBOARD_UI = 9004;

  private String greetingMsg="Welcome, ";
  private boolean greetingDisplayed;
  @Override
  public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
    Log.i(TAG, "onUserEarnedReward");
    REWARD = 3;
    // TODO: Reward the user!
  }
  @Override
  protected void onCreate(@NonNull Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_game);


    mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

    view = new MainView(this, this);

    typeface = Typeface.createFromAsset(getAssets(), "ClearSans-Bold.ttf");

    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayShowTitleEnabled(false);
    }
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
    view.hasSaveState = settings.getBoolean("save_state", false);

    if (savedInstanceState != null) {
      if (savedInstanceState.getBoolean("hasState")) {
        load();
      }
    }

    if (settings.getBoolean(FIRST_LAUNCH, true)) {
      openAboutDialog();
      settings.edit().putBoolean(FIRST_LAUNCH, false).apply();
    }
    greetingDisplayed=false;

    mGoogleSignInClient = GoogleSignIn.getClient(this,
            new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN).build());
    final FrameLayout gameContainer = (FrameLayout) findViewById(R.id.game_container);
    gameContainer.addView(view);
    MobileAds.initialize(this, new OnInitializationCompleteListener() {
      @Override
      public void onInitializationComplete(InitializationStatus initializationStatus) {
        loadAd();
      }
    });

    setupGameOverButton();
    setupWinViews();
    setupAdMobBanner();
    setupAdMobInterstitial();
    //  setupAdMobRewards();
  }

  private void setupAdMobBanner() {
    MobileAds.initialize(this, new OnInitializationCompleteListener() {
      @Override
      public void onInitializationComplete(InitializationStatus initializationStatus) {
      }
    });

    mAdView = findViewById(R.id.adView);
    AdRequest adRequest = new AdRequest.Builder().build();
    mAdView.loadAd(adRequest);
  }

  private void setupWinViews() {
    winTextView = (TextView) findViewById(R.id.win_text_view);
    winTextView.setTypeface(typeface);
  }

  private void setupGameOverButton() {
    gameOverButton = (Button) findViewById(R.id.game_over_button);

    gameOverButton.setTypeface(typeface);
    gameOverButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (view.isGameOver() || view.game.gameLost() || !view.game.isCanContinue()) {
          startNewGame();
        } else if (view.game.isCanContinue()) {
          gameOverButton.setText(R.string.try_again);
          view.game.gameState = 0;
          view.invalidate();
          view.refreshLastTime = true;
          winTextView.setVisibility(View.GONE);
          gameOverButton.setVisibility(View.GONE);
        }
      }
    });

  }

  public void startNewGame() {
    gameOverButton.setVisibility(View.GONE);
    winTextView.setVisibility(View.GONE);
    view.game.newGame();
    view.setGameOver(false);
    view.setGameWin(false);
  }

  private void setupAdMobInterstitial() {
    AdRequest adRequest = new AdRequest.Builder().build();

    InterstitialAd.load(this,getString(R.string.interstitial_ad_unit_id), adRequest, new InterstitialAdLoadCallback() {
      @Override
      public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
        // The mInterstitialAd reference will be null until
        // an ad is loaded.
        mInterstitialAd = interstitialAd;
        Log.d("TAG", "onAdLoaded");
      }

      @Override
      public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
        // Handle the error
        Log.d("TAG", loadAdError.getMessage());
        mInterstitialAd = null;
      }
    });

    // requestNewInterstitial();
  }

  public void loadAd() {
    // Use the test ad unit ID to load an ad.
    RewardedInterstitialAd.load(GameActivity.this, getString(R.string.reward_ad_unit_id),
            new AdRequest.Builder().build(),  new RewardedInterstitialAdLoadCallback() {
              @Override
              public void onAdLoaded(RewardedInterstitialAd ad) {
                rewardedInterstitialAd = ad;
                Log.e(TAG1, "onAdLoaded");
              }
              @Override
              public void onAdFailedToLoad(LoadAdError loadAdError) {
                Log.e(TAG1, "onAdFailedToLoad");
              }
            });
  }
  // public void submitScore(){
  //   Games.getLeaderboardsClient(this, GoogleSignIn.getLastSignedInAccount(this))
  //           .submitScore(getString(R.string.leaderboard_id), view.game.highScore);
  // }
  public boolean isSignedIn() {
    return GoogleSignIn.getLastSignedInAccount(this) != null;
  }
  private void signInSilently() {

    mGoogleSignInClient.silentSignIn().addOnCompleteListener(this,
            task -> {
              if (task.isSuccessful()) {
                greetingMsg="Welcome back, ";
                onConnected(task.getResult());
              } else {
                onDisconnected();
              }
            });
  }
  private void onConnected(GoogleSignInAccount googleSignInAccount) {

    mLeaderboardsClient = Games.getLeaderboardsClient(this, googleSignInAccount);
    mPlayersClient = Games.getPlayersClient(this, googleSignInAccount);

    mPlayersClient.getCurrentPlayer()
            .addOnCompleteListener(new OnCompleteListener<Player>() {
              @Override
              public void onComplete(@NonNull Task<Player> task) {
                String displayName;
                if (task.isSuccessful()) {
                  displayName = task.getResult().getDisplayName();
                } else {
                  Exception e = task.getException();
                  handleException(e, getString(R.string.players_exception));
                  displayName = "???";
                }

                if(!greetingDisplayed)
                  welcomeMessage(displayName);
              }
            });
  }
  private void welcomeMessage(String name){
    Context context = getApplicationContext();
    CharSequence text = greetingMsg + name;
    int duration = Toast.LENGTH_LONG;

    Toast toast = Toast.makeText(context, text, duration);
    toast.setGravity(Gravity.TOP, 0, 0);
    toast.show();
    greetingDisplayed=true;
  }

  public void startSignInIntent() {
    startActivityForResult(mGoogleSignInClient.getSignInIntent(), RC_SIGN_IN);
    Log.d("TAG", "Nece da se loguje");
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);

    if (requestCode == RC_SIGN_IN) {
      Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(intent);

      try {
        GoogleSignInAccount account = task.getResult(ApiException.class);
        greetingMsg="Welcome, ";
        onConnected(account);
      } catch (ApiException apiException) {
        String message = apiException.getMessage();
        if (message == null || message.isEmpty()) {
          message = getString(R.string.signin_other_error);
        }

        onDisconnected();
      }
    }
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_MENU) {
      //Do nothing
      return true;
    } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
      view.game.move(2);
      return true;
    } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
      view.game.move(0);
      return true;
    } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
      view.game.move(3);
      return true;
    } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
      view.game.move(1);
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  @Override
  public void onSaveInstanceState(Bundle savedInstanceState) {
    super.onSaveInstanceState(savedInstanceState);
    savedInstanceState.putBoolean("hasState", true);
    save();
  }

  protected void onPause() {
    super.onPause();
    save();
  }

  private void save() {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
    SharedPreferences.Editor editor = settings.edit();
    Tile[][] field = view.game.grid.field;
    Tile[][] undoField = view.game.grid.undoField;
    editor.putInt(WIDTH, field.length);
    editor.putInt(HEIGHT, field.length);
    for (int xx = 0; xx < field.length; xx++) {
      for (int yy = 0; yy < field[0].length; yy++) {
        if (field[xx][yy] != null) {
          editor.putInt(xx + " " + yy, field[xx][yy].getValue());
        } else {
          editor.putInt(xx + " " + yy, 0);
        }

        if (undoField[xx][yy] != null) {
          editor.putInt(UNDO_GRID + xx + " " + yy, undoField[xx][yy].getValue());
        } else {
          editor.putInt(UNDO_GRID + xx + " " + yy, 0);
        }
      }
    }
    editor.putLong(SCORE, view.game.score);
    editor.putLong(HIGH_SCORE, view.game.highScore);
    editor.putLong(UNDO_SCORE, view.game.lastScore);
    editor.putBoolean(CAN_UNDO, view.game.canUndo);
    editor.putInt(GAME_STATE, view.game.gameState);
    editor.putInt(UNDO_GAME_STATE, view.game.lastGameState);
    editor.putBoolean(GAME_OVER, view.isGameOver());
    editor.putBoolean(WIN, view.isGameWin());
    editor.putBoolean(CAN_CONTINUE, view.game.isCanContinue());
    editor.apply();
  }

  protected void onResume() {
    super.onResume();
    load();
    signInSilently();
  }

  private void load() {
    // Stopping all animations
    view.game.aGrid.cancelAnimations();

    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
    for (int xx = 0; xx < view.game.grid.field.length; xx++) {
      for (int yy = 0; yy < view.game.grid.field[0].length; yy++) {
        int value = settings.getInt(xx + " " + yy, -1);
        if (value > 0) {
          view.game.grid.field[xx][yy] = new Tile(xx, yy, value);
        } else if (value == 0) {
          view.game.grid.field[xx][yy] = null;
        }

        int undoValue = settings.getInt(UNDO_GRID + xx + " " + yy, -1);
        if (undoValue > 0) {
          view.game.grid.undoField[xx][yy] = new Tile(xx, yy, undoValue);
        } else if (value == 0) {
          view.game.grid.undoField[xx][yy] = null;
        }
      }
    }

    view.game.score = settings.getLong(SCORE, view.game.score);
    view.game.highScore = settings.getLong(HIGH_SCORE, view.game.highScore);
    view.game.lastScore = settings.getLong(UNDO_SCORE, view.game.lastScore);
    view.game.canUndo = settings.getBoolean(CAN_UNDO, view.game.canUndo);
    view.game.gameState = settings.getInt(GAME_STATE, view.game.gameState);
    view.game.lastGameState = settings.getInt(UNDO_GAME_STATE, view.game.lastGameState);
    view.setGameOver(settings.getBoolean(GAME_OVER, false));
    view.setGameWin(settings.getBoolean(WIN, false));
    view.game.setCanContinue(settings.getBoolean(CAN_CONTINUE, false));

    if (view.isGameOver() || (view.game.isCanContinue() && view.isGameWin())) {
      showGameOverViews();
    }

  }

  @Override
  public void onBackPressed() { /*Do nothing*/ }

  private void rateApp() {
    Uri uri = Uri.parse(getString(R.string.link_to_page));
    Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
    goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
            Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
    try {
      startActivity(goToMarket);
    } catch (ActivityNotFoundException e) {
      startActivity(new Intent(Intent.ACTION_VIEW,
              Uri.parse(getString(R.string.link_to_page))));
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    switch (id) {
      case R.id.action_login:
        startSignInIntent();
        // showLeaderBoard();
        return true;
      case R.id.action_logout:
        signOut();
        // showLeaderBoard();
        return true;
      case R.id.action_LeaderBoard:
        showLeaderBoard();
        return true;
      case R.id.action_rate:
        rateApp();
        return true;
      case R.id.action_info:
        openAboutDialog();
        return true;
      case R.id.action_share:
        shareText();
        return true;
    }
    return false;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu, menu);
    getSupportActionBar().setDisplayShowTitleEnabled(true);
    return true;
  }

  private void openAboutDialog() {
    if (dialog == null) {
      dialog = new MaterialDialog.Builder(this)
              .backgroundColor(ContextCompat.getColor(this, R.color.text_black))
              .customView(R.layout.activity_about_dilaog, false).build();

      Typeface typeface2 = Typeface.createFromAsset(getAssets(), "ClearSans-Regular.ttf");
      TextView myTextView = (TextView) dialog.findViewById(R.id.game_description);
      Button okButton = (Button) dialog.findViewById(R.id.ok_button);
      okButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          dialog.hide();
        }
      });
      okButton.setTypeface(typeface);
      myTextView.setTypeface(typeface2);
    }
    dialog.show();
  }

  private void shareText() {
    String shareBody = (getString(R.string.share_message_title) + "\n" + getString(
            R.string.current_points)
            + " " + view.game.getHighScore() + getString(R.string.try_beat_me) + "\n" + getString(
            R.string.share_message_here)
            + getString(
            R.string.share_app_link));
    Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
    sharingIntent.setType("text/plain");
    sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
    startActivity(
            Intent.createChooser(sharingIntent, getString(R.string.share_dialog_title)));
  }

  public void showGameOverViews() {
    if (view.isGameWin()) {
      if (view.game.isCanContinue()) {
        gameOverButton.setText(R.string.continue_button);
        winTextView.setText(R.string.you_win);
      } else {
        winTextView.setText(R.string.game_over);
        gameOverButton.setText(R.string.try_again);
      }
    } else if (view.isGameOver()) {
      winTextView.setText(R.string.game_over);
      gameOverButton.setText(R.string.try_again);
    }
    winTextView.setVisibility(View.VISIBLE);
    gameOverButton.setVisibility(View.VISIBLE);
  }

  public void showInterstitial() {
    rewardedInterstitialAd.show(/* Activity */ GameActivity.this,/*
    OnUserEarnedRewardListener */ GameActivity.this);
  }
  private void signOut() {

    if (!isSignedIn()) {
      Log.w(TAG, "signOut() called, but was not signed in!");
      return;
    }

    mGoogleSignInClient.signOut().addOnCompleteListener(this,
            new OnCompleteListener<Void>() {
              @Override
              public void onComplete(@NonNull Task<Void> task) {
                boolean successful = task.isSuccessful();
                Log.d(TAG, "signOut(): " + (successful ? "success" : "failed"));

                onDisconnected();
              }
            });
  }

  private void onDisconnected() {

    mLeaderboardsClient = null;
    mPlayersClient = null;
  }

  public void submitScore(int score){
    if(isSignedIn())
      mLeaderboardsClient.submitScore(getString(R.string.leaderboard_id), view.game.highScore);
  }

  public void showLeaderBoard() {
    if(isSignedIn())
      Log.d("TAG", "Mesto mulja");
    mLeaderboardsClient.getLeaderboardIntent(getString(R.string.leaderboard_id))
            .addOnSuccessListener(new OnSuccessListener<Intent>() {
              @Override
              public void onSuccess(Intent intent) {
                startActivityForResult(intent, RC_LEADERBOARD_UI);
              }
            });
  }

  private void handleException(Exception e, String details) {
    int status = 0;

    if (e instanceof ApiException) {
      ApiException apiException = (ApiException) e;
      status = apiException.getStatusCode();
    }

    @SuppressLint({"StringFormatInvalid", "LocalSuppress"}) String message = getString(R.string.status_exception_error, details, status, e);

    new AlertDialog.Builder(this)
            .setMessage(message)
            .setNeutralButton(android.R.string.ok, null)
            .show();
  }
}
