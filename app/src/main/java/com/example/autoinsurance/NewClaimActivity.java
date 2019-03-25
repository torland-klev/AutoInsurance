package com.example.autoinsurance;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
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
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class NewClaimActivity extends AppCompatActivity implements AsyncResponse{


    private DrawerLayout drawerLayout;
    private EditText TITLE, DATE, PLATE, DESCRIPTION;
    private Calendar myCalendar;
    private Intent mIntent;
    private String SESSION_ID;
    private final int LOGOUT_CODE = 5;
    private boolean NEW_CLAIM_SUBMITTED = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_claim);
        mIntent = getIntent();
        SESSION_ID = mIntent.getStringExtra("SESSION_ID");

        // Sets a new toolbar with navigation menu button
        Toolbar toolbar = findViewById(R.id.nc_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);
        drawerLayout = findViewById(R.id.nc_drawer_layout);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        //creates a listener for the navigation menu
        NavigationView navigationView = findViewById(R.id.nc_nav_view);
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


        TITLE = findViewById(R.id.nc_et_title);
        DATE = findViewById(R.id.nc_et_date);
        PLATE = findViewById(R.id.nc_et_plate);
        DESCRIPTION = findViewById(R.id.nc_et_description);

        /* These are for picking date */
        /*----------------------------*/
        //https://stackoverflow.com/questions/14933330/datepicker-how-to-popup-datepicker-when-click-on-edittext
        myCalendar = Calendar.getInstance();
        final DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {

            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear,
                                  int dayOfMonth) {
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, monthOfYear);
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateLabel();
            }

        };
        DATE.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                new DatePickerDialog(NewClaimActivity.this, date, myCalendar
                        .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                        myCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });
        /*      Date picking done     */
        /*----------------------------*/


    }

    public void submitNewClaim(View view) {
        String title = TITLE.getText().toString();
        String date  = DATE.getText().toString();
        String plate = PLATE.getText().toString();
        String desc  = DESCRIPTION.getText().toString();
        NEW_CLAIM_SUBMITTED = true;
        Boolean f = false;

        if (title.length() < 6){
            TITLE.setError("Title too short (<6)");
            f = true;
        }
        if (date.length() != 10){
            DATE.setError("Please choose a date");
            f = true;
        }
        if (plate.length() < 6){
            PLATE.setError("Plate number too short (<6)");
            f = true;
        }
        if (plate.length() > 10){
            PLATE.setError("Plate number too long (>10)");
            f = true;
        }
        if (desc.length() < 20){
            DESCRIPTION.setError("Description too short (<20)");
            f = true;
        }
        if (f){
            return;
        }
        AsyncWebServiceCaller asyncTask = new AsyncWebServiceCaller();
        asyncTask.delegate = this;
        String[] args = {"submitNewClaim", SESSION_ID, title, date, plate, desc};
        asyncTask.execute(args);

    }

    private void updateLabel() {
        String myFormat = "dd-MM-yyyy"; //In which you need put here
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
        DATE.setError(null);
        DATE.setText(sdf.format(myCalendar.getTime()));
    }

    @Override
    public void processFinish(String output) {
        //User clicks LogOut
        if (output.equals("true") && !NEW_CLAIM_SUBMITTED) {
            setResult(RESULT_OK, new Intent());
            finish();
        }
        NEW_CLAIM_SUBMITTED = false;
        TextView tv = findViewById(R.id.nc_result);
        if(output.equals("true")){
            tv.setText(getString(R.string.claim_submitted));
            tv.setTextColor(Color.GREEN);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
