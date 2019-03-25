package com.example.autoinsurance;

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
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ClaimActivity extends AppCompatActivity implements AsyncResponse{
    private DrawerLayout drawerLayout;
    private final int LOGOUT_CODE = 5;
    private String SESSION_ID;
    private String CLAIM_ID;

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
        getClaim();
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

        //Storing customer data in a HashMap. May be useful to have it stored later.
        HashMap<String, String> customer = new HashMap<>();
        try {
            JSONObject obj  = new JSONObject(output);
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()){
                String s = keys.next();
                customer.put(s, obj.getString(s));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        fillActivity(customer);
    }

    /**
     * Fills the activity with information about the customer.
     *
     * @param customer HashMap containing the Key-Value pairs of the customer retrieved from the
     *                 web server.
     */
    private void fillActivity(HashMap<String, String> customer) {

        int c = 400;
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
                set.connect(c, ConstraintSet.TOP, R.id.claim_title, ConstraintSet.BOTTOM, dp*2);
                set.connect(c+KEY, ConstraintSet.TOP, R.id.claim_title, ConstraintSet.BOTTOM, dp*2);
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
}
