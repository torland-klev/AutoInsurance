package com.example.autoinsurance;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements AsyncResponse{

    private final int BOOLEAN_REQUEST = 1;
    private final int ACCOUNT_PICKER = 2;
    private final String CHANNEL_ID = "notify", accountType = "com.AutoInsurance";
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
    private String filename = "/sessionid.tmp";

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

        //Create notification-channel for notifications when new messages are detected.
        createNotificationChannel();
        //Test connection
        testConnectionThread = new MyThread(CONNECTION_STATUS, CHANNEL_ID, this, null);
        testConnectionThread.start();

        //Check if user has previous sessionID stored in cache. If so, log the user in using
        //that sessionID. If it turns out that the SessionID is broken, the user will be logged
        //out, and the cache cleared.
        boolean cacheHIT = false;
        try {
            File f = new File(this.getCacheDir() + filename);
            BufferedReader in = new BufferedReader(new FileReader(f));
            Log.d("MAIN CACHE", "Cache was opened");
            String sessionID = in.readLine();
            in.close();
            Log.d("SESSIONID", "Offline login using sessinID: " + sessionID);
            cacheHIT = true;
            navigateToHomeScreen(sessionID);

        } catch (Exception e) {
            Log.d("MAIN CACHE", "Cache open failed");
        }

        //Check for accounts
        am = AccountManager.get(this);
        auAccounts = am.getAccountsByType(accountType);
        if ((auAccounts.length != 0) && !cacheHIT) {
            loginAuto();
        }
    }


    /**
     * The loginButton calls this method.
     * Method creates a new Async task, and executes it with user-provided username and password.
     * @param view: View that the button is clicked from.
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

    /**
     * Login-auto is called if no SessionID is stored in cache, and if the Account Manager has
     * accounts stored. It then provides the user with a display where the user can choose between
     * the accounts stored.
     * NOTE: there is currently no other way to delete accounts than to wipe the phone, or delete
     * the account through settings.
     *
     * If the user chooses an account, the login will be completed by the onActivityResult()-method.
     * If the user does not choose an account, do nothing and dismiss the dialog windows.
     *
     * Note: there is currently an issue where the dialog box shows twice. This has been proven
     * to be more a graphical problem, and gives no other known practical issues.
     */
    private void loginAuto(){
        //Displays dialog to user, where user can choose to pick account
        Log.d("LOGIN AUTO", "Login auto was called");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Login");
        builder.setMessage("Would you like to log in with stored account?");

        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = AccountManager.newChooseAccountIntent(null, null,
                        new String[] { accountType }, true, null, null,
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
        alert.closeOptionsMenu();
    }

    /**
     * If the output from the async ws-caller is 0, then the login failed. Meaning wrong username
     * and/or password.
     *
     * If the output from the async ws-caller is -1, then the async caller failed to connect to the
     * server. This is an inconsistency on my part, as in the other cases the async caller returns
     * 0 when the server is unavailable. (Update: it now returns -1 most cases).
     *
     * If neither of these outputs, then it is assumes that login was a success. It will then
     * call addAccount(), which adds the account to the device, and then write the SessionID to cache.
     * Finally, in this successful scenario, calls the navigateToHomeScreen()-method with the SessionID.
     *
     * @param output: Output returned from the Async task.
     */
    @Override
    public void processFinish(String output) {

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
                // Write sessionID to cache
                try {
                    File f = new File(this.getCacheDir() + filename);
                    BufferedWriter out = new BufferedWriter(new FileWriter(f));
                    out.write(output);
                    out.append("\n");
                    out.close();
                    Log.d("MAIN CACHE", "Cache was written " + f.getAbsolutePath());
                } catch (IOException e) {
                    Log.d("MAIN CACHE", "Cache writing failed");
                    e.printStackTrace();
                }
                navigateToHomeScreen(output);
                break;
        }
    }

    /**
     * Starts by checking if the account already exists, by comparing the accounts to the username.
     * If exists, break and set the exists-flag. Else, do nothing.
     * Then, if the exists-flag is set, and the username is non-empty, create a new account
     * using the provided username and password. Finally, add the newly created account
     * to the account-manager.
     */
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
            Account account = new Account(username, accountType);
            am.addAccountExplicitly(account, password, null);
        }
    }

    /**
     * Navigates to homescreen given the SessionID, using the returncode BOOLEAN_REQUEST. This
     * requestcode was planned to use throughout the project, but is only used here.
     * @param sessionID The sessionID retreived from server when logging in.
     */
    private void navigateToHomeScreen(String sessionID) {
        Intent homeScreenIntent = new Intent(this, HomeActivity.class);
        homeScreenIntent.putExtra("SESSION_ID", sessionID);
        startActivityForResult(homeScreenIntent, BOOLEAN_REQUEST);
    }

    /**
     * Start by starting the testConnectingThread again, if it was somehow stopped. Unsure if this
     * is necessary? I'm too afraid to remove it.
     *
     * If the requestCode is BOOLEAN_REQUEST, then the user pressed logout. If the result is OK
     * (which it should always be, as the app is designed with robustness in mind), delete the
     * cache-file containing the SessionID, else do nothing but warn user. After, call loginAuto()
     * again, which will display an account-manager dialog box again.
     *
     * If the requestCode is ACCOUNT_PICKER, then it is the loginAuto()-method that is trying
     * to log in using an account from account-manager. It then iterates through all accounts until
     * it finds the one matching the provided username, and adds username and password of the
     * given account as params to a login-request.
     *
     * If neither request-codes, then do nothing.
     *
     * @param requestCode Required.
     * @param resultCode Required.
     * @param data Required.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        testConnectionThread.shutdown = false;
        Log.d("onActivityResult", "Was run");
        super.onActivityResult(requestCode, resultCode, data);
        // Logout
        if (requestCode == BOOLEAN_REQUEST) {
            if (resultCode == RESULT_OK) {
                LOGIN_STATUS.setText(getString(R.string.logout_success));
                File f = new File(this.getCacheDir() + filename);
                f.delete();
                LOGIN_STATUS.setTextColor(Color.BLUE);
            } else {
                Log.d("LOGOUT_RESULT", Integer.toString(resultCode));
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
                if (auAccounts != null) {
                    for (Account a : auAccounts) {
                        if (a.name.equals(data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME))) {
                            ac = a;
                            break;
                        }
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

    /**
     * Needed on API 26+ to send notifications.
     */
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Notification Channel";
            String description = "Notification channel description";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
