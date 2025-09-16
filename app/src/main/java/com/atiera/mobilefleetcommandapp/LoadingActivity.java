package com.atiera.mobilefleetcommandapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class LoadingActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent i = new Intent(LoadingActivity.this, DashboardActivity.class);
            startActivity(i);
            finish();
        }, 3000);
    }
}


