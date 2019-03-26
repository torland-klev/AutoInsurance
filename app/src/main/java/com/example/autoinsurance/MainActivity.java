package com.example.autoinsurance;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements AsyncResponse{

    private final int BOOLEAN_REQUEST = 1;
    private final int ACCOUNT_PICKER = 2;
    private MyThread testConnectionThread;
    private EditText USERNAME;
    private EditText PASSWORD;
    private TextView LOGIN_STATUS;
    private TextView CONNECTION_STATUS;
    private static final int SDK_VERSION = Build.VERSION.SDK_INT;
    private AccountManager am;
    private Account[] auAccounts;
    private String username;
    private String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Grab the different Views
        USERNAME = findViewById(R.id.username);
        PASSWORD = findViewById(R.id.password);
        LOGIN_STATUS = findViewById(R.id.login_status);
        CONNECTION_STATUS = findViewById(R.id.connectionStatus);

        //Set focus on username
        USERNAME.requestFocus();



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

        //Check for accounts
        am = AccountManager.get(this);
        auAccounts = am.getAccountsByType("com.AutoInsurance");
        if (auAccounts.length != 0){
            loginAuto();
        }

        //Test connection

        testConnectionThread = new MyThread(CONNECTION_STATUS);
        testConnectionThread.start();
    }


    /**
     * The loginButton calls this method.
     * Method creates a new Async task, and executes it with user-provided username and password.
     * @param view: View that the button is clicked from (unsure about this, read more)
     */
    public void loginButton(View view) {
        AsyncWebServiceCaller asyncTask = new AsyncWebServiceCaller();
        asyncTask.delegate = this;
        password = PASSWORD.getText().toString();
        username = USERNAME.getText().toString();
        boolean f = false;

        if (username.length() == 0){
            USERNAME.setError("Username empty");
            f = true;
        }
        if (password.length() == 0){
            PASSWORD.setError("Password empty");
            f = true;
        }
        if (f){
            return;
        }
        String[] args = {"login", username, password};
        asyncTask.execute(args);
    }

    private void loginAuto(){
        //Displays dialog to user, where user can choose to pick account
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Login");
        builder.setMessage("Would you like to log in with stored account?");

        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = AccountManager.newChooseAccountIntent(null, null,
                        new String[] { "com.AutoInsurance" }, true, null, null,
                        null, null);
                startActivityForResult(intent, ACCOUNT_PICKER);
                dialog.dismiss();
            }
        });

        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }

    /** TODO: Switch to next activity
     * Do something with the result from the AsyncWebServiceCaller
     * @param output: Output returned from the Async task.
     */
    @Override
    public void processFinish(String output) {

        Log.d("Call Results", output);
        //Login return 0 if login failed.
        USERNAME.getText().clear();
        PASSWORD.getText().clear();
        USERNAME.requestFocus();
        switch (output) {
            case "0":
                LOGIN_STATUS.setVisibility(View.VISIBLE);
                LOGIN_STATUS.setTextColor(Color.RED);
                LOGIN_STATUS.setText(getString(R.string.loginFailed));
                break;
            case "-1":
                LOGIN_STATUS.setVisibility(View.VISIBLE);
                LOGIN_STATUS.setTextColor(Color.RED);
                LOGIN_STATUS.setText(getString(R.string.webServerUnavailableLogin));
                break;
            default:
                LOGIN_STATUS.setVisibility(View.VISIBLE);
                LOGIN_STATUS.setTextColor(Color.GREEN);
                LOGIN_STATUS.setText(getString(R.string.loginSuccess));
                addAccount();
                testConnectionThread.shutdown = true;
                navigateToHomeScreen(output);
                break;
        }
    }

    private void addAccount(){
        boolean exists = false;
        for (int i = 0; i < auAccounts.length; i++){
            if(auAccounts[i].name.equals(username)){
                exists = true;
                Log.d("ACCOUNT", "Account exists");
                break;
            }
        }
        if (!exists && (username != null)) {
            Account account = new Account(username, "com.AutoInsurance");
            am.addAccountExplicitly(account, password, null);
        }
    }

    private void navigateToHomeScreen(String sessionID) {
        Intent homeScreenIntent = new Intent(this, HomeActivity.class);
        homeScreenIntent.putExtra("SESSION_ID", sessionID);
        startActivityForResult(homeScreenIntent, BOOLEAN_REQUEST);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("onActivityResult", "Was run");
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BOOLEAN_REQUEST) {
            if (resultCode == RESULT_OK) {
                LOGIN_STATUS.setText(getString(R.string.logout_success));
                LOGIN_STATUS.setTextColor(Color.BLUE);
            } else {
                LOGIN_STATUS.setText(getString(R.string.logout_unsuccess));
                LOGIN_STATUS.setTextColor(Color.YELLOW);
            }
            loginAuto();
        }
        else if (requestCode == ACCOUNT_PICKER){
            if (resultCode == RESULT_OK) {
                AsyncWebServiceCaller asyncTask = new AsyncWebServiceCaller();
                asyncTask.delegate = this;
                Account ac = null;

                for (Account a : auAccounts){
                    if (a.name.equals(data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME))){
                        ac = a;
                        break;
                    }
                }

                if (ac == null){
                    return;
                }

                String[] args = {"login",
                        ac.name,
                        am.getPassword(ac)};
                Log.d("ACCOUNT", args[1] + " " + args[2]);
                asyncTask.execute(args);
            } else {
                Log.d("ACCOUNT", "Account picking error");
            }
        }
    }
}
