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
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

public class ChatActivity extends AppCompatActivity implements AsyncResponse{

    private Intent mIntent;
    private String SESSION_ID;
    private String CLAIM_ID;
    private DrawerLayout drawerLayout;
    private EditText MESSAGE;
    private final int LOGOUT_CODE = 5, SENT_MESSAGE_CODE = 6;
    private ArrayList<JSONObject> messages = new ArrayList<>();
    private boolean NEW_CLAIM_SUBMITTED = false;
    private String filename;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        mIntent = getIntent();
        SESSION_ID = mIntent.getStringExtra("SESSION_ID");
        CLAIM_ID = mIntent.getStringExtra("CLAIM_ID");
        MESSAGE = findViewById(R.id.new_chat);
        filename = "/chatcache" + CLAIM_ID + ".tmp";

        //Create JSON object of the String-array from Intent
        try {
            for (String s : mIntent.getStringArrayExtra("messages")) {
                try {
                    messages.add(new JSONObject(s));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e){
            Log.d("CHAT ACTIVITY", "No String Extras to get.");
        }

        // Sets a new toolbar with navigation menu button
        Toolbar toolbar = findViewById(R.id.chat_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);
        drawerLayout = findViewById(R.id.chat_drawer_layout);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        //creates a listener for the navigation menu
        NavigationView navigationView = findViewById(R.id.chat_nav_view);
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
                });
        displayMessages();
    }

    private void displayMessages(){

        int c = 1234;
        boolean first = true;
        Iterator<JSONObject> i = messages.iterator();
        while (i.hasNext()) {
            JSONObject obj = i.next();
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()){
                String key = keys.next();
                String value = null;
                try {
                    value = obj.getString(key);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //C is used for TextView ID's
                c++;

                //Declare new TextViews
                TextView tv = new TextView(this);

                //Set ID for textview. ID is incremental.
                tv.setId(c);

                //Set text
                tv.setText(value);

                //Add new view
                ConstraintLayout layout = findViewById(R.id.chat_layout);
                ConstraintSet set = new ConstraintSet();
                layout.addView(tv, 0);
                set.clone(layout);

                //Easiest way to programatically set DP
                int size_dp = 8;
                int dp = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, size_dp, getResources()
                                .getDisplayMetrics());

                if (key.equals("sender")){
                    //Set the senders name to bold
                    tv.setTypeface(null, Typeface.BOLD);
                    //If first chat, constrain it to top of title
                    if (first){
                        set.connect(c, ConstraintSet.TOP, R.id.chat_title, ConstraintSet.BOTTOM, dp);
                        first = false;
                    }
                    //Else constrain it to the previous chat message
                    else {
                        set.connect(c, ConstraintSet.TOP, c-5, ConstraintSet.BOTTOM, dp*2);
                    }
                    //Set Server (aka sender) on the left
                    if (value.equals("AutoInSure")){
                        set.connect(c, ConstraintSet.START, c-1, ConstraintSet.END, dp);
                        set.connect(c-1, ConstraintSet.START, layout.getId(), ConstraintSet.START, dp);
                        set.connect(c-2, ConstraintSet.START, layout.getId(), ConstraintSet.START, dp);
                    } else {
                        set.connect(c, ConstraintSet.END, c-1, ConstraintSet.START, dp);
                        set.connect(c-1, ConstraintSet.END, layout.getId(), ConstraintSet.END, dp);
                        set.connect(c-2, ConstraintSet.END, layout.getId(), ConstraintSet.END, dp);
                    }
                    set.connect(c-1, ConstraintSet.TOP, c, ConstraintSet.TOP);
                    set.connect(c-2, ConstraintSet.TOP, c-1, ConstraintSet.BOTTOM);
                    set.connect(R.id.new_chat, ConstraintSet.TOP, c-2, ConstraintSet.BOTTOM, dp);
                } else if (key.equals("msg")){
                    //Set width to half of parent
                    DisplayMetrics displayMetrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                    int width = displayMetrics.widthPixels;
                    Log.d("TOTAL", Integer.toString(width));
                    set.constrainWidth(c, width/2);
                }
                set.applyTo(layout);
            }
        }
    }

    @Override
    public void processFinish(String output) {

        //No cache needed, as the messages are not retrieved from server directly, but given from
        //the claim activity.

        //User clicks LogOut
        if (output.equals("true") && !NEW_CLAIM_SUBMITTED) {
            setResult(RESULT_OK, new Intent());
            SESSION_ID = null;
            finish();
        }
        //Message was sent
        NEW_CLAIM_SUBMITTED = false;
        TextView tv = findViewById(R.id.chat_result);
        if(output.equals("true")){
            tv.setText(getString(R.string.message_sent));
            tv.setTextColor(Color.GREEN);
            MESSAGE.setText("");
            setResult(SENT_MESSAGE_CODE);
            finish();
        }
        else{
            tv.setText(getString(R.string.something_went_wrong));
            tv.setTextColor(Color.RED);
        }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == LOGOUT_CODE){
            if (resultCode == RESULT_OK){
                setResult(RESULT_OK, new Intent());
                finish();
            }
        }
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

    public void goBack(View view) {
        finish();
    }

    public void newMessage(View view) {

        //Get message-body from EditText-field, and send the message to server.
        String msg_body  = MESSAGE.getText().toString();

        if (msg_body.length() < 20){
            MESSAGE.setError("Message too short (<20)");
        } else {
            NEW_CLAIM_SUBMITTED = true;
            AsyncWebServiceCaller asyncTask = new AsyncWebServiceCaller();
            asyncTask.delegate = this;
            String[] args = {"submitNewMessage", SESSION_ID, CLAIM_ID, msg_body};
            asyncTask.execute(args);
        }
    }
}
