package com.example.autoinsurance;

import android.content.Intent;
import android.graphics.Color;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HomeActivity extends AppCompatActivity implements AsyncResponse{
    private DrawerLayout drawerLayout;

    private String SESSION_ID;
    private final int LOGOUT_CODE = 5, ERROR_CODE = -10;
    private boolean LOGOUT = false;
    private final String CHANNEL_ID = "notify";
    private MyThread checkMessagesThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        // Sets a new toolbar with navigation menu button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);
        drawerLayout = findViewById(R.id.drawer_layout);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        //Get the intent, and the provided SessionID
        Intent mIntent = getIntent();
        SESSION_ID = mIntent.getStringExtra("SESSION_ID");
        Log.i("HOME_SESSION_ID", SESSION_ID);

        //creates a listener for the navigation menu
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        // set item as selected to persist highlight
                        menuItem.setChecked(true);

                        // close drawer when item is tapped
                        drawerLayout.closeDrawers();

                        // Add code here to update the UI based on the item selected
                        // For example, swap UI fragments here
                        switch (menuItem.toString()) {
                            case "Home":
                                Log.d("NAVIGATION_MENU", "Home");
                                startHomeActivity();
                                break;
                            case "History":
                                Log.d("NAVIGATION_MENU", "History");
                                startHistoryActivity();
                                break;
                            case "New claim":
                                Log.d("NAVIGATION_MENU", "New claim");
                                startNewClaimActivity();
                                break;
                            case "Log out":
                                Log.d("NAVIGATION_MENU", "Log out");
                                LOGOUT = true;
                                logout();
                                break;
                        }

                        return true;
                    }
                });

        //Create a thread to check for new messages.

        checkMessagesThread = new MyThread(new TextView(this), CHANNEL_ID, this, SESSION_ID);
        checkMessagesThread.start();

        //Call get method that grabs the customer information from the server, given the Session ID.
        getCustomerInfo();

    }

    /**
     *
     * @param item MenuItem clicked by user.
     * @return Return-flag
     */
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
     * Logs out user by nulling SessionID (remove the pointer, memory not overwritten. Possible
     * security issue, but who cares?), shutting down the CheckMessagesThread, and finishing
     * the activity.
     */
    public void logout() {
        AsyncWebServiceCaller asyncTask = new AsyncWebServiceCaller();
        asyncTask.delegate = this;
        String[] args = {"logout", SESSION_ID};
        asyncTask.execute(args);
        SESSION_ID = null;
        checkMessagesThread.shutdown = true;
    }

    /**
     * Uses the async ws-caller to get customer info given the SessionID. Results are handled
     * in processFinished().
     */
    public void getCustomerInfo(){
        AsyncWebServiceCaller asyncTask = new AsyncWebServiceCaller();
        asyncTask.delegate = this;
        String[] args = {"getCustomerInfo", SESSION_ID};
        asyncTask.execute(args);
    }

    /**
     * Start by checking if the server is offline. If that is the case, display a warning to the user.
     * Then, look for cache. If cache found, read the data from cache and fill it using the
     * fillActivity()-method. Else, display to the user that no cache was found.
     *
     * If output is true, the user has clicket log out. Logs out user.
     *
     * If output is false, something went wrong with either sessionID, or on the server side. If
     * this happens, log out the user. The following result-code is not handled, but displays
     * a warning to the user.
     *
     * If none of these three, then the output is the customer info.
     * Store the JSON-object in a <Property/Key , Value> HashMap, write said HashMap to cache,
     * and use the HashMap as a parameter for fillActivity().
     *
     * @param output the data received from the Async ws-caller
     */
    @Override
    public void processFinish(String output) {

        String filename = "/homecache.tmp";
        //Something went wrong
        if (output.equals("false") || (output.equals("invalid sessionId"))){
            setResult(ERROR_CODE, new Intent());
            SESSION_ID = null;
            checkMessagesThread.shutdown = true;
            finish();
        }
        //Server went offline
        else if (output.equals("-1") && !LOGOUT){

            ConstraintLayout layout = findViewById(R.id.home_layout);
            ConstraintSet set = new ConstraintSet();
            TextView status = new TextView(this);
            status.setId(R.id.connectionStatus);
            status.setVisibility(View.VISIBLE);
            status.setTextColor(Color.RED);
            layout.addView(status, 0);
            set.clone(layout);
            set.connect(status.getId(), ConstraintSet.END, R.id.home_title, ConstraintSet.START);
            set.connect(status.getId(), ConstraintSet.START, R.id.home_title, ConstraintSet.END);
            set.connect(status.getId(), ConstraintSet.BOTTOM, R.id.home_layout, ConstraintSet.BOTTOM, 100);
            set.applyTo(layout);

            try {
                FileInputStream fis = new FileInputStream(this.getCacheDir() + filename);
                Log.d("HOME CACHE", "Cache was opened");
                ObjectInputStream obj = new ObjectInputStream(fis);
                status.setText(getString(R.string.webServerUnavailableCache));
                fillActivity((HashMap<String, String>) obj.readObject());
                obj.close();
                fis.close();
            } catch (Exception e) {
                Log.d("HOME CACHE", "Cache open failed");
                status.setText(getString(R.string.webServerUnavailable));
                e.printStackTrace();
            }
        }

        //User clicks LogOut
        else if (output.equals("true") || LOGOUT) {
            Log.d("HOME", "Logout Success");
            SESSION_ID = null;
            setResult(RESULT_OK, new Intent());
            finish();
        }
        else {
            //Storing customer data in a HashMap. Makes it easier to store in cache.
            HashMap<String, String> customer = new HashMap<>();
            try {
                JSONObject obj = new JSONObject(output);
                Iterator<String> keys = obj.keys();
                while (keys.hasNext()) {
                    String s = keys.next();
                    customer.put(s, obj.getString(s));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            //Store customer in cache

            try {
                File f = new File(this.getCacheDir() + filename);
                FileOutputStream out = new FileOutputStream(f);
                ObjectOutputStream obj = new ObjectOutputStream(out);
                obj.writeObject(customer);
                obj.close();
                out.close();
                Log.d("HOME CACHE", "Cache was written " + f.getAbsolutePath());
            } catch (IOException e) {
                Log.d("HOME CACHE", "Cache writing failed");
                e.printStackTrace();
            }

            fillActivity(customer);
        }
    }

    /**
     * Fills the activity with information about the customer. 
     *
     * @param customer HashMap containing the Key-Value pairs of the customer retrieved from the
     *                 web server.
     */
    private void fillActivity(HashMap<String, String> customer) {

        int c = 0;
        final int KEY = 100;
        for (Map.Entry<String, String> pair : customer.entrySet()) {

            //C is used for TextView ID's
            c++;

            //Declare new TextViews
            TextView value = new TextView(this);
            TextView key = new TextView(this);

            //Set properties for value
            value.setText(pair.getValue());
            value.setId(c);

            //Set properties for key
            key.setTypeface(null, Typeface.BOLD);
            key.setText(pair.getKey());
            key.setId(c+KEY);

            //Add new views
            ConstraintLayout layout = findViewById(R.id.home_layout);
            ConstraintSet set = new ConstraintSet();

            layout.addView(value, 0);
            layout.addView(key, 0);
            set.clone(layout);

            int size_dp = 8;

            int dp = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, size_dp, getResources()
                            .getDisplayMetrics());

            set.connect(c+KEY, ConstraintSet.END, R.id.home_title, ConstraintSet.START);
            set.connect(c, ConstraintSet.START, R.id.home_title, ConstraintSet.END);
            if (c == 1) {
                set.connect(c, ConstraintSet.TOP, R.id.home_title, ConstraintSet.BOTTOM, dp*2);
                set.connect(c+KEY, ConstraintSet.TOP, R.id.home_title, ConstraintSet.BOTTOM, dp*2);
            } else {
                set.connect(c, ConstraintSet.TOP, c-1, ConstraintSet.BOTTOM, dp*2);
                set.connect(c+KEY, ConstraintSet.TOP, c+KEY-1, ConstraintSet.BOTTOM, dp*2);
            }
            set.applyTo(layout);
        }
    }

    public void startHistoryActivity (){
        Log.d("NAVIGATION_MENU", "Tries to open history activity");
        Intent intent = new Intent(this, HistoryActivity.class);
        intent.putExtra("SESSION_ID", SESSION_ID);
        startActivityForResult(intent, LOGOUT_CODE);
    }

    public void startNewClaimActivity (){
        Log.d("NAVIGATION_MENU", "Tries to open New Claim activity");
        Intent intent = new Intent(this, NewClaimActivity.class);
        intent.putExtra("SESSION_ID", SESSION_ID);
        startActivityForResult(intent, LOGOUT_CODE);
    }
    private void startHomeActivity() {
        Log.d("NAVIGATION_MENU", "Tries to open home activity");
        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra("SESSION_ID", SESSION_ID);
        startActivityForResult(intent, LOGOUT_CODE);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == LOGOUT_CODE){
            if (resultCode == RESULT_OK){
                setResult(RESULT_OK, new Intent());
                finish();
            }
            else{
                setResult(ERROR_CODE, new Intent());
                finish();
            }
        }
    }

}
