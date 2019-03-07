package com.example.autoinsurance;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class HomeActivity extends AppCompatActivity implements AsyncResponse{

    private int SESSION_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Intent mIntent = getIntent();
        SESSION_ID = mIntent.getIntExtra("SESSION_ID", 0);
        Log.i("HOME_SESSION_ID", Integer.toString(SESSION_ID));
    }

    public void logout(View view) {
        AsyncWebServiceCaller asyncTask = new AsyncWebServiceCaller();
        asyncTask.delegate = this;
        String[] args = {"logout", Integer.toString(SESSION_ID)};
        asyncTask.execute(args);
    }

    @Override
    public void processFinish(String output) {
        Log.i("Logout", output);
        setResult(RESULT_OK, new Intent());
        finish();

    }
}
