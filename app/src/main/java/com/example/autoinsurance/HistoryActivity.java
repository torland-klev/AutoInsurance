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
import android.view.View;

public class HistoryActivity extends AppCompatActivity implements AsyncResponse{
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

    public void startChatActivity (){
        Log.d("NAVIGATION_MENU", "Tries to open Chat activity");
        Intent intent = new Intent(this, ChatActivity.class);
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
    public void processFinish(String output) {
        //User clicks LogOut
        if (output.equals("true")) {
            setResult(RESULT_OK, new Intent());
            finish();
        }
    }
}
