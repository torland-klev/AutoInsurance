package com.example.autoinsurance;

import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class MyThread extends Thread implements AsyncResponse{

    private TextView CONNECTION_STATUS;
    volatile boolean shutdown = false;

    public MyThread(TextView cs){
        CONNECTION_STATUS = cs;
    }

    private Boolean prevConnected = false;
    private final int CONNECTION_TEST_TIMEOUT = 4000;
    public void run(){
        while(!shutdown) {
            Log.i("TestConnection", "Testing connection.\n");
            AsyncWebServiceCaller testConnection = new AsyncWebServiceCaller();
            testConnection.delegate = this;
            testConnection.execute("TEST_CONNECTION");
            try {
                //If connection was previously established, take 3 times longer between testing
                if (prevConnected){
                    Thread.sleep(CONNECTION_TEST_TIMEOUT * 2);
                }
                Thread.sleep(CONNECTION_TEST_TIMEOUT);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void processFinish(String output) {
        if (output.equals("0")) {
            prevConnected = false;
            CONNECTION_STATUS.setVisibility(View.VISIBLE);
            CONNECTION_STATUS.setTextColor(Color.RED);
            CONNECTION_STATUS.setText("Web Server Unavailable");
        } else {
            prevConnected = true;
            CONNECTION_STATUS.setVisibility(View.VISIBLE);
            CONNECTION_STATUS.setTextColor(Color.GREEN);
            CONNECTION_STATUS.setText("Web Server Available");
        }
    }
}