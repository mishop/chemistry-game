package com.Misho.Chemistry.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class SplashScreenActivity extends AppCompatActivity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    startGameActivity();
  }

  private void startGameActivity() {
    Intent intent = new Intent(this, GameActivity.class);
    startActivity(intent);
  }

}
