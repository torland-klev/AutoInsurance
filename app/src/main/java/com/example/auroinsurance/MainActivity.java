package com.example.auroinsurance;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

public class MainActivity extends AppCompatActivity {

    private EditText username;
    private EditText password;
    private static final int SDK_VERSION = Build.VERSION.SDK_INT;
    private AccountManager am;
    private Account[] googleAccounts;
    private Account[] allAccounts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        am = AccountManager.get(this);
        googleAccounts = am.getAccountsByType("com.google");
        allAccounts = am.getAccounts();

        //If no Google Accounts found, remove the button
        ImageButton ib_google = findViewById(R.id.googleLogo);
        ImageButton ib_facebook = findViewById(R.id.facebookLogo);
        if (googleAccounts.length == 0){
            ib_google.setVisibility(View.INVISIBLE);
        }
        if (allAccounts.length == 0) {
            ib_facebook.setVisibility(View.INVISIBLE);
        }

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

    public void loginButton(View view) {
    }

    public void loginGoogle(View view) {
    }

    public void loginFacebook(View view) {
    }
}
