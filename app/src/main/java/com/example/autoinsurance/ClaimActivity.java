package com.example.autoinsurance;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.renderscript.Sampler;
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
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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

public class ClaimActivity extends AppCompatActivity implements AsyncResponse{
    private DrawerLayout drawerLayout;
    private final int LOGOUT_CODE = 5;
    private String SESSION_ID;
    private String CLAIM_ID;
    private String[] messages = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_claim);
        // Sets a new toolbar with navigation menu button
        Toolbar toolbar = findViewById(R.id.claim_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);
        drawerLayout = findViewById(R.id.claim_drawer_layout);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        Intent mIntent = getIntent();
        CLAIM_ID = mIntent.getStringExtra("CLAIM_ID");
        SESSION_ID = mIntent.getStringExtra("SESSION_ID");
        Log.i("CLAIM_SESSION_ID", SESSION_ID);
        Log.i("CLAIM_ID", CLAIM_ID);

        //creates a listener for the navigation menu
        NavigationView navigationView = findViewById(R.id.claim_nav_view);
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
                                logout();
                                break;
                        }

                        return true;
                    }
                }
        );
        getClaim();
        getChat();
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

    private void getClaim(){
        AsyncWebServiceCaller asyncTask = new AsyncWebServiceCaller();
        asyncTask.delegate = this;
        String[] args = {"getClaimInfo", SESSION_ID, CLAIM_ID};
        asyncTask.execute(args);
    }

    private void getChat(){
        AsyncWebServiceCaller asyncTask = new AsyncWebServiceCaller();
        asyncTask.delegate = this;
        String[] args = {"listClaimMessages", SESSION_ID, CLAIM_ID};
        asyncTask.execute(args);
    }

    public void logout() {
        AsyncWebServiceCaller asyncTask = new AsyncWebServiceCaller();
        asyncTask.delegate = this;
        String[] args = {"logout", SESSION_ID};
        asyncTask.execute(args);
    }

    private void startHomeActivity() {
        Log.d("NAVIGATION_MENU", "Tries to open home activity");
        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra("SESSION_ID", SESSION_ID);
        startActivityForResult(intent, LOGOUT_CODE);
    }

    public void startHistoryActivity() {
        Log.d("NAVIGATION_MENU", "Tries to open history activity");
        Intent intent = new Intent(this, HistoryActivity.class);
        intent.putExtra("SESSION_ID", SESSION_ID);
        startActivityForResult(intent, LOGOUT_CODE);
    }

    public void startNewClaimActivity() {
        Log.d("NAVIGATION_MENU", "Tries to open New Claim activity");
        Intent intent = new Intent(this, NewClaimActivity.class);
        intent.putExtra("SESSION_ID", SESSION_ID);
        startActivityForResult(intent, LOGOUT_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LOGOUT_CODE) {
            if (resultCode == RESULT_OK) {
                setResult(RESULT_OK, new Intent());
                finish();
            }
        }
    }

    @SuppressLint("ResourceType")
    @Override
    public void processFinish(String output) {
        //Server went offline
        String filename = "/claimcache" + CLAIM_ID + ".tmp";
        if (output.equals("-1")){

            ConstraintLayout layout = findViewById(R.id.claim_layout);
            ConstraintSet set = new ConstraintSet();
            TextView status = new TextView(this);
            status.setId(R.id.connectionStatus);
            status.setVisibility(View.VISIBLE);
            status.setTextColor(Color.RED);
            layout.addView(status, 0);
            set.clone(layout);
            set.connect(status.getId(), ConstraintSet.END, R.id.claim_title, ConstraintSet.START);
            set.connect(status.getId(), ConstraintSet.START, R.id.claim_title, ConstraintSet.END);
            set.connect(status.getId(), ConstraintSet.TOP, R.id.claim_title, ConstraintSet.BOTTOM);
            set.applyTo(layout);

            try {
                FileInputStream fis = new FileInputStream(this.getCacheDir() + filename);
                Log.d("CLAIM CACHE", "Cache was opened");
                ObjectInputStream obj = new ObjectInputStream(fis);
                status.setText(getString(R.string.webServerUnavailableCache));
                fillActivity((HashMap<String, String>) obj.readObject());
            } catch (Exception e) {
                Log.d("CLAIM CACHE", "Cache open failed");
                status.setText(getString(R.string.webServerUnavailable));
                e.printStackTrace();
            }
        }
        //User clicks LogOut
        if (output.equals("true")) {
            setResult(RESULT_OK, new Intent());
            SESSION_ID = null;
            finish();
        }

        //Storing claim data in a HashMap. May be useful to have it stored later.
        HashMap<String, String> claim = new HashMap<>();
        try {

            // Get chat history of claim
            if ((output.length() > 6) && output.substring(3, 6).equals("msg")) {
                //Puts output from form [{.1.},{.2.},...,{.N.}]
                //into array[0] = {.1.}, array[1] = {.2.}, ..., array[N-1] = {.N.}
                output = output.substring(1, output.length() - 1);
                String msgs[] = output.split("\\},");
                for (int i = 0; i < msgs.length; i++) {
                    msgs[i] = msgs[i] + "}";
                }
                messages = msgs;
                // Display how many messages are in the claim
                TextView tv = findViewById(R.id.nrOfMessages);
                String displayText = getString(R.string.nrOfMessages) + messages.length;
                tv.setText(displayText);
            }
            // No messages found for claim
            else if (output.length() < 6){
                TextView tv = findViewById(R.id.nrOfMessages);
                String displayText = getString(R.string.nrOfMessages) + "0";
                tv.setText(displayText);
            }
            // Get the information of the claim
            else {

                JSONObject obj = new JSONObject(output);
                Iterator<String> keys = obj.keys();
                while (keys.hasNext()) {
                    String s = keys.next();
                    claim.put(s, obj.getString(s));
                }

                try {
                    File f = new File(this.getCacheDir() + filename);
                    FileOutputStream out = new FileOutputStream(f);
                    ObjectOutputStream obj2 = new ObjectOutputStream(out);
                    obj2.writeObject(claim);
                    obj2.close();
                    Log.d("CLAIM CACHE", "Cache was written " + f.getAbsolutePath());
                } catch (IOException e) {
                    Log.d("CLAIM CACHE", "Cache writing failed");
                    e.printStackTrace();
                }
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        //Store claims in cache

        fillActivity(claim);
    }

    /**
     * Fills the activity with information about the claim.
     *
     * @param claim HashMap containing the Key-Value pairs of the claim retrieved from the
     *                 web server.
     */
    private void fillActivity(HashMap<String, String> claim) {

        int c = 400;
        final int KEY = 100;
        for (Map.Entry<String, String> pair : claim.entrySet()) {

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
            ConstraintLayout layout = findViewById(R.id.claim_layout);
            ConstraintSet set = new ConstraintSet();

            layout.addView(value, 0);
            layout.addView(key, 0);
            set.clone(layout);

            int size_dp = 8;

            int dp = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, size_dp, getResources()
                            .getDisplayMetrics());

            set.connect(c+KEY, ConstraintSet.END, R.id.claim_title, ConstraintSet.START);
            set.connect(c, ConstraintSet.START, R.id.claim_title, ConstraintSet.END);
            set.connect(R.id.back_button, ConstraintSet.TOP, c, ConstraintSet.BOTTOM);
            if (c == 401) {
                set.connect(c, ConstraintSet.TOP, R.id.claim_title, ConstraintSet.BOTTOM, dp*5);
                set.connect(c+KEY, ConstraintSet.TOP, R.id.claim_title, ConstraintSet.BOTTOM, dp*5);
            } else {
                set.connect(c, ConstraintSet.TOP, c-1, ConstraintSet.BOTTOM, dp*2);
                set.connect(c+KEY, ConstraintSet.TOP, c+KEY-1, ConstraintSet.BOTTOM, dp*2);
            }
            set.applyTo(layout);
        }
    }

    public void goBack(View view) {
        finish();
    }

    public void toChat(View view) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("SESSION_ID", SESSION_ID);
        intent.putExtra("CLAIM_ID", CLAIM_ID);
        intent.putExtra("messages", messages);
        startActivityForResult(intent, LOGOUT_CODE);
    }
}
