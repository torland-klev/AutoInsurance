package com.example.auroinsurance;

import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    private EditText username;
    private EditText password;
    private static final int SDK_VERSION = Build.VERSION.SDK_INT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        username = findViewById(R.id.username);
        password = findViewById(R.id.password);

        //Check SDK-version.
        //Autofill only works for versions 26 and up
        if (SDK_VERSION >= 26){
            Log.i("MainActivity","API version is " + SDK_VERSION + "\n");
            username.setAutofillHints(View.AUTOFILL_HINT_USERNAME);
            username.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_YES);
            password.setAutofillHints(View.AUTOFILL_HINT_PASSWORD);
            password.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_YES);
        }
        else {
            Log.i("MainActivity", "API version " +
                    SDK_VERSION + " does not support autofill.\n");
        }
    }
}
