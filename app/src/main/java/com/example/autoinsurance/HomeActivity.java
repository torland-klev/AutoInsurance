package com.example.autoinsurance;

import android.content.Intent;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kobjects.util.Strings;
import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HomeActivity extends AppCompatActivity implements AsyncResponse{

    private String SESSION_ID;
    private String RESULT;

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
                                break;
                        }

                        return true;
                    }
                });


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);

        Intent mIntent = getIntent();
        SESSION_ID = mIntent.getStringExtra("SESSION_ID");
        Log.i("HOME_SESSION_ID", SESSION_ID);
        getCustomerInfo();
    }

    public void logout(View view) {
        AsyncWebServiceCaller asyncTask = new AsyncWebServiceCaller();
        asyncTask.delegate = this;
        String[] args = {"logout", SESSION_ID};
        asyncTask.execute(args);
    }

    private void getCustomerInfo(){
        AsyncWebServiceCaller asyncTask = new AsyncWebServiceCaller();
        asyncTask.delegate = this;
        String[] args = {"getCustomerInfo", SESSION_ID};
        asyncTask.execute(args);
    }

    @Override
    public void processFinish(String output) {
        //User clicks LogOut
        if (output.equals("true")) {
            setResult(RESULT_OK, new Intent());
            finish();
        }
        //Called from onCreate()

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

        RelativeLayout rl = findViewById(R.id.home_layout);
        int c = 0;
        for (Map.Entry<String, String> pair : customer.entrySet()) {

            //C is used for TextView ID's
            c++;

            //Declare new TextViews
            TextView value = new TextView(this);
            TextView key = new TextView(this);

            //Set properties for value
            value.setText(pair.getValue());
            value.setId(c);
            value.setGravity(Gravity.CENTER_HORIZONTAL);

            //Set properties for key
            key.setTypeface(null, Typeface.BOLD);
            key.setText(pair.getKey());

            //Set layout params
            RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            RelativeLayout.LayoutParams p2 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (c == 1){
                p.addRule(RelativeLayout.BELOW, R.id.home_title);
                p2.addRule(RelativeLayout.BELOW, R.id.home_title);
            } else {
                p.addRule(RelativeLayout.BELOW, c-1);
                p2.addRule(RelativeLayout.BELOW, c-1);
            }
            p.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            p2.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

            int size_dp = 8;

            int dp = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, size_dp, getResources()
                            .getDisplayMetrics());

            p.setMargins(dp, dp,dp*2,dp);
            p2.setMargins(dp*2, dp,dp,0);

            //Set layout params
            value.setLayoutParams(p);
            key.setLayoutParams(p2);
            rl.addView(value);
            rl.addView(key);
        }
    }

    public void startHistoryActivity (){
        Log.d("NAVIGATION_MENU", "Tries to open history activity");
        Intent intent = new Intent(this, HistoryActivity.class);
        startActivity(intent);
    }

    public void startChatActivity (){
        Log.d("NAVIGATION_MENU", "Tries to open Chat activity");
        Intent intent = new Intent(this, ChatActivity.class);
        startActivity(intent);
    }

    public void startNewClaimActivity (){
        Log.d("NAVIGATION_MENU", "Tries to open New Claim activity");
        Intent intent = new Intent(this, NewClaimActivity.class);
        startActivity(intent);
    }

}
