package com.example.autoinsurance;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
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

public class HistoryActivity extends AppCompatActivity implements AsyncResponse {
    private DrawerLayout drawerLayout;
    private final int LOGOUT_CODE = 5, ERROR_CODE = -10;
    private boolean LOGOUT = false;
    private String SESSION_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        // Sets a new toolbar with navigation menu button
        Toolbar toolbar = findViewById(R.id.history_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);
        drawerLayout = findViewById(R.id.history_drawer_layout);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        Intent mIntent = getIntent();
        SESSION_ID = mIntent.getStringExtra("SESSION_ID");
        Log.i("HISTORY_SESSION_ID", SESSION_ID);

        //creates a listener for the navigation menu
        NavigationView navigationView = findViewById(R.id.history_nav_view);
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
                }
        );
        getHistory();
    }

    private void getHistory() {
        AsyncWebServiceCaller asyncTask = new AsyncWebServiceCaller();
        asyncTask.delegate = this;
        String[] args = {"listClaims", SESSION_ID};
        asyncTask.execute(args);
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
            else {
                setResult(ERROR_CODE, new Intent());
                finish();
            }
        }
    }

    @Override
    public void processFinish(String output) {
        //Something went wrong
        if (output.equals("false") || (output.equals("invalid sessionId"))){
            setResult(ERROR_CODE, new Intent());
            SESSION_ID = null;
            finish();
        }
        //Server went offline
        String filename = "/historycache.tmp";
        if (output.equals("-1") && !LOGOUT){
            ConstraintLayout layout = findViewById(R.id.history_layout);
            ConstraintSet set = new ConstraintSet();
            TextView status = new TextView(this);
            status.setId(R.id.connectionStatus);
            status.setVisibility(View.VISIBLE);
            status.setTextColor(Color.RED);
            layout.addView(status, 0);
            set.clone(layout);
            set.connect(status.getId(), ConstraintSet.END, R.id.history_title, ConstraintSet.START);
            set.connect(status.getId(), ConstraintSet.START, R.id.history_title, ConstraintSet.END);
            set.connect(status.getId(), ConstraintSet.TOP, R.id.history_title, ConstraintSet.BOTTOM);
            set.applyTo(layout);

            try {
                FileInputStream fis = new FileInputStream(this.getCacheDir() + filename);
                Log.d("HISTORY CACHE", "Cache was opened");
                ObjectInputStream obj = new ObjectInputStream(fis);
                status.setText(getString(R.string.webServerUnavailableCache));
                fillActivity((HashMap<String, String>) obj.readObject());
                obj.close();
            } catch (Exception e) {
                Log.d("HISTORY CACHE", "Cache open failed");
                status.setText(getString(R.string.webServerUnavailable));
                e.printStackTrace();
            }
        }
        //User clicks LogOut
        else if (output.equals("true") && LOGOUT) {
            setResult(RESULT_OK, new Intent());
            SESSION_ID = null;
            finish();
        }
        else {
            //Puts output from form [{.1.},{.2.},...,{.N.}]
            //into array[0] = {.1.}, array[1] = {.2.}, ..., array[N-1] = {.N.}
            output = output.substring(1, output.length() - 1);
            String newOutput[] = output.split("\\},");

            for (int i = 0; i < newOutput.length; i++) {
                newOutput[i] = newOutput[i] + "}";
            }

            //Storing history data in a HashMap. May be useful to have it stored later.
            HashMap<String, String> claims = new HashMap<>();
            try {
                for (String is : newOutput) {
                    JSONObject obj = new JSONObject(is);
                    Iterator<String> keys = obj.keys();
                    while (keys.hasNext()) {
                        String s = keys.next();
                        String s2 = keys.next();
                        claims.put(obj.getString(s), obj.getString(s2));
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            //Store claims in cache
            try {
                File f = new File(this.getCacheDir() + filename);
                FileOutputStream out = new FileOutputStream(f);
                ObjectOutputStream obj = new ObjectOutputStream(out);
                obj.writeObject(claims);
                obj.close();
                out.close();
                Log.d("HISTORY CACHE", "Cache was written " + f.getAbsolutePath());
            } catch (IOException e) {
                Log.d("HISTORY CACHE", "Cache writing failed");
                e.printStackTrace();
            }

            fillActivity(claims);
        }
    }
    private void fillActivity(HashMap<String, String> customer) {

        final int s = 200;
        final int KEY = 100;
        for (Map.Entry<String, String> pair : customer.entrySet()) {
            Log.d("FILL", "key: " + pair.getKey() + ". Value: " + pair.getValue());
            //C is used for TextView ID's
            int c = s + Integer.parseInt(pair.getKey());
            //Declare new TextViews
            TextView value = new TextView(this);
            TextView key = new TextView(this);

            //Set properties for value
            value.setText(pair.getValue());
            value.setId(c);
            value.setClickable(true);
            setOnClick(value, this, value.getId()-s);

            //Set max-width to 1/4 of parent.
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int width = displayMetrics.widthPixels;
            Log.d("TOTAL", Integer.toString(width));
            value.setMaxWidth(width/4);

            //Set properties for key
            key.setTypeface(null, Typeface.BOLD);
            key.setText(pair.getKey());
            key.setId(c+KEY);
            key.setClickable(true);
            setOnClick(value, this, value.getId()-s);

            //Add new views

            ConstraintLayout layout = findViewById(R.id.history_layout);
            ConstraintSet set = new ConstraintSet();

            layout.addView(value, 0);
            layout.addView(key, 0);
            set.clone(layout);

            int size_dp = 8;

            int dp = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, size_dp, getResources()
                            .getDisplayMetrics());

            set.connect(c+KEY, ConstraintSet.END, R.id.history_title, ConstraintSet.START);
            set.connect(c, ConstraintSet.START, c+KEY, ConstraintSet.END, dp);
            set.connect(c, ConstraintSet.END, R.id.history_title, ConstraintSet.END, dp);
            if (Integer.parseInt(pair.getKey()) == 1) {
                set.connect(c, ConstraintSet.TOP, R.id.history_title, ConstraintSet.BOTTOM, dp*5);
                set.connect(c+KEY, ConstraintSet.TOP, R.id.history_title, ConstraintSet.BOTTOM, dp*5);
            } else {
                set.connect(c, ConstraintSet.TOP, c-1, ConstraintSet.BOTTOM, dp);
                set.connect(c+KEY, ConstraintSet.TOP, c, ConstraintSet.TOP);
            }
            set.applyTo(layout);
        }
    }
    private void setOnClick(View view, final HistoryActivity t, final int id){
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(t, ClaimActivity.class);
                intent.putExtra("SESSION_ID", SESSION_ID);
                intent.putExtra("CLAIM_ID", Integer.toString(id));
                startActivityForResult(intent, LOGOUT_CODE);
            }
        });
    }
}
