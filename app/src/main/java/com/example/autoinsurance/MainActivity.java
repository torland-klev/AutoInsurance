package com.example.autoinsurance;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements AsyncResponse{

    public static final int CONNECTION_TEST_TIMEOUT = 4000;
    private EditText USERNAME;
    private EditText PASSWORD;
    private TextView LOGIN_STATUS;
    private TextView CONNECTION_STATUS;
    private static final int SDK_VERSION = Build.VERSION.SDK_INT;
    private AccountManager am;
    private Account[] googleAccounts;
    private Account[] allAccounts;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Sets a new toolbar with navigation menu button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);


        //Grab the different Views
        USERNAME = findViewById(R.id.username);
        PASSWORD = findViewById(R.id.password);
        LOGIN_STATUS = findViewById(R.id.login_status);
        CONNECTION_STATUS = findViewById(R.id.connectionStatus);

        //Check for different accounts
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

        drawerLayout = findViewById(R.id.drawer_layout);

        //creates a listener for the navigation menu
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        // set item as selected to persist highlight
                        menuItem.setChecked(true);
                        switch (menuItem.toString()) {
                            case "Home":
                                Log.d("NAVIGATION_MENU", "Home");
                                break;
                            case "History":
                                Log.d("NAVIGATION_MENU", "History");
                                break;
                            case "Chat":
                                Log.d("NAVIGATION_MENU", "Chat");
                                break;
                            case "New claim":
                                Log.d("NAVIGATION_MENU", "New claim");
                                break;
                            case "Log out":
                                Log.d("NAVIGATION_MENU", "Log out");
                                break;
                        }
                        // close drawer when item is tapped
                        drawerLayout.closeDrawers();

                        // Add code here to update the UI based on the item selected
                        // For example, swap UI fragments here

                        return true;
                    }
                });


        //Test connection
        class MyThread extends Thread implements AsyncResponse{
            private Boolean prevConnected = false;
            public void run(){
                while(true) {
                    Log.i("TestConnection", "Testing connection.\n");
                    AsyncWebServiceCaller testConnection = new AsyncWebServiceCaller();
                    testConnection.delegate = this;
                    testConnection.execute("TEST_CONNECTION");
                    try {
                        //If connection was previously established, take 3 times longer between testing
                        if (prevConnected){
                            Thread.sleep(CONNECTION_TEST_TIMEOUT * 2);
                        }
                        Thread.sleep(CONNECTION_TEST_TIMEOUT);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void processFinish(String output) {
                if (output.equals("0")) {
                    prevConnected = false;
                    CONNECTION_STATUS.setVisibility(View.VISIBLE);
                    CONNECTION_STATUS.setTextColor(Color.RED);
                    CONNECTION_STATUS.setText(getString(R.string.webServerUnavailable));
                } else {
                    prevConnected = true;
                    CONNECTION_STATUS.setVisibility(View.VISIBLE);
                    CONNECTION_STATUS.setTextColor(Color.GREEN);
                    CONNECTION_STATUS.setText(getString(R.string.webServerAvailable));
                }
            }
        }
        MyThread testConnectionThread = new MyThread();
        testConnectionThread.start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
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

        Log.d("Call Results", output);
        //Login return 0 if login failed.
        switch (output) {
            case "0":
                LOGIN_STATUS.setVisibility(View.VISIBLE);
                LOGIN_STATUS.setTextColor(Color.RED);
                LOGIN_STATUS.setText(getString(R.string.loginFailed));
                break;
            case "-1":
                LOGIN_STATUS.setVisibility(View.VISIBLE);
                LOGIN_STATUS.setTextColor(Color.RED);
                LOGIN_STATUS.setText(getString(R.string.webServerUnavailable));
                break;
            default:
                LOGIN_STATUS.setVisibility(View.VISIBLE);
                LOGIN_STATUS.setTextColor(Color.GREEN);
                LOGIN_STATUS.setText(getString(R.string.loginSuccess));
                navigateToHomeScreen(output);
                break;
        }
    }

    private void navigateToHomeScreen(){
        Intent homeScreenIntent = new Intent(this, HomeActivity.class);
        startActivity(homeScreenIntent);
    }

    private <T> void navigateToHomeScreen(T extra){
        Intent homeScreenIntent = new Intent(this, HomeActivity.class);
        final String EXTRA_MESSAGE =
                "com.example.android.autoinsurance.extra.MESSAGE";
        homeScreenIntent.putExtra(EXTRA_MESSAGE, extra.toString());
        startActivity(homeScreenIntent);
    }
}
