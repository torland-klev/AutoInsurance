package com.example.auroinsurance;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements AsyncResponse{

    private EditText USERNAME;
    private EditText PASSWORD;
    private TextView LOGIN_STATUS;
    private static final int SDK_VERSION = Build.VERSION.SDK_INT;
    private AccountManager am;
    private Account[] googleAccounts;
    private Account[] allAccounts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Grab the different Views
        USERNAME = findViewById(R.id.username);
        PASSWORD = findViewById(R.id.password);
        LOGIN_STATUS = findViewById(R.id.login_status);

        am = AccountManager.get(this);
        googleAccounts = am.getAccountsByType("com.google");
        allAccounts = am.getAccounts();

        //If no Google/other accounts found, make the buttons invisible
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
            USERNAME.setAutofillHints(View.AUTOFILL_HINT_USERNAME);
            USERNAME.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_YES);
            PASSWORD.setAutofillHints(View.AUTOFILL_HINT_PASSWORD);
            PASSWORD.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_YES);
        }
        else {
            Log.i("MainActivity", "API version " +
                    SDK_VERSION + " does not support autofill.\n");
        }
    }

    /**
     * The loginButton calls this method.
     * Method creates a new Async task, and executes it with user-provided username and password.
     * @param view: View that the button is clicked from (unsure about this, read more)
     */
    public void loginButton(View view) {
        AsyncWebServiceCaller asyncTask = new AsyncWebServiceCaller();
        asyncTask.delegate = this;
        String[] args = {"login", USERNAME.getText().toString(), PASSWORD.getText().toString()};
        asyncTask.execute(args);
    }

    //TODO: login from Google account
    public void loginGoogle(View view) {
    }

    //TODO: login from Facebook account
    public void loginFacebook(View view) {
    }

    /** TODO: Switch to next activity
     * Do something with the result from the AsyncWebServiceCaller
     * @param output: Output returned from the Async task.
     */
    @Override
    public void processFinish(String output) {
        //Login return 0 if login failed.
        if (output.equals("0")) {
            LOGIN_STATUS.setVisibility(View.VISIBLE);
            LOGIN_STATUS.setTextColor(Color.RED);
            LOGIN_STATUS.setText(getString(R.string.loginFailed));
        } else {
            LOGIN_STATUS.setVisibility(View.VISIBLE);
            LOGIN_STATUS.setTextColor(Color.GREEN);
            LOGIN_STATUS.setText(getString(R.string.loginSuccess));
        }
        Log.d("Call Results", output);
    }
}
