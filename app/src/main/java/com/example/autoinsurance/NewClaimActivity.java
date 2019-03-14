package com.example.autoinsurance;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class NewClaimActivity extends AppCompatActivity implements AsyncResponse{

    private EditText TITLE, DATE, PLATE, DESCRIPTION;
    private Calendar myCalendar;
    private Intent mIntent;
    private String SESSION_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_claim);
        mIntent = getIntent();
        SESSION_ID = mIntent.getStringExtra("SESSION_ID");
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
}
