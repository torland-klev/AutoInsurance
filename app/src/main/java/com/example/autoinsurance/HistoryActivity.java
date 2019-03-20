package com.example.autoinsurance;

import android.app.Activity;
import android.content.Intent;
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
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HistoryActivity extends AppCompatActivity implements AsyncResponse {
    private DrawerLayout drawerLayout;
    private final int LOGOUT_CODE = 5;
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
                            case "Chat":
                                Log.d("NAVIGATION_MENU", "Chat");
                                startChatActivity();
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

    public void startChatActivity() {
        Log.d("NAVIGATION_MENU", "Tries to open Chat activity");
        Intent intent = new Intent(this, ChatActivity.class);
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

    @Override
    public void processFinish(String output) {
        //User clicks LogOut
        if (output.equals("true")) {
            setResult(RESULT_OK, new Intent());
            finish();
        }

        //Puts output from form [{.1.},{.2.},...,{.N.}]
        //into array[0] = {.1.}, array[1] = {.2.}, ..., array[N-1] = {.N.}
        output = output.substring(1, output.length()-1);
        String newOutput[] = output.split("\\},");


        //Storing customer data in a HashMap. May be useful to have it stored later.
        for (int i = 0; i < newOutput.length; i++){
            newOutput[i] = newOutput[i] + "}";
        }

        Log.d("FINISH", output);
        for (String s : newOutput){
            Log.d("FINISH", s);
        }
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
        fillActivity(claims);
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

            //Set properties for key
            key.setTypeface(null, Typeface.BOLD);
            key.setText(pair.getKey());
            key.setId(c+KEY);
            key.setClickable(true);

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
                set.connect(c, ConstraintSet.TOP, R.id.history_title, ConstraintSet.BOTTOM, dp*2);
                set.connect(c+KEY, ConstraintSet.TOP, R.id.history_title, ConstraintSet.BOTTOM, dp*2);
            } else {
                set.connect(c, ConstraintSet.TOP, c-1, ConstraintSet.BOTTOM, dp*2);
                set.connect(c+KEY, ConstraintSet.TOP, c+KEY-1, ConstraintSet.BOTTOM, dp*2);
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
