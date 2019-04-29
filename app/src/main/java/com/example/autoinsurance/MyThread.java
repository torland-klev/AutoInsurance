package com.example.autoinsurance;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

public class MyThread extends Thread implements AsyncResponse{

    private TextView CONNECTION_STATUS;
    private Context CONTEXT;
    private String CHANNEL_ID, SESSION_ID;
    private boolean checkMessages = false, getMessages = false;
    volatile boolean shutdown = false;
    private ArrayList<String> claimIDS;
    private int NumberOfMessages = 0, prevNumberOfMessages = 0, async_counter = 0, return_counter = 0;

    public MyThread(TextView cs, String id, Context con, String sID){
        CONNECTION_STATUS = cs;
        CHANNEL_ID = id;
        CONTEXT = con;
        SESSION_ID = sID;
        if (sID != null) {
            Log.d("THREAD", "Message checker thread created.");
            checkMessages = true;
        } else {
            Log.d("THREAD", "Connection checker thread created.");
        }
    }

    private Boolean prevConnected = false;
    private final int CONNECTION_TEST_TIMEOUT = 4000;
    public void run(){
        while(!shutdown) {
            if (!checkMessages){
                Log.i("THREAD", "Testing connection.");
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
            } else {
                Log.d("THREAD", "Getting history");
                checkMessages = true;
                getHistory();
                try {
                    Thread.sleep(CONNECTION_TEST_TIMEOUT * 5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    private void getHistory(){
        //Get all claims
        AsyncWebServiceCaller asyncTask = new AsyncWebServiceCaller();
        asyncTask.delegate = this;
        String[] args = {"listClaims", SESSION_ID};
        asyncTask.execute(args);
    }
    private void getClaim(){
        Log.d("THREAD", "getClaim was called.");
        getMessages = true;
        for (String s : claimIDS){
            AsyncWebServiceCaller asyncTask = new AsyncWebServiceCaller();
            asyncTask.delegate = this;
            String[] args = {"listClaimMessages", SESSION_ID, s};
            async_counter++;
            asyncTask.execute(args);
        }
    }

    @Override
    public void processFinish(String output) {
        //Message-checker thread
        if (checkMessages){
            if (getMessages){
                return_counter++;
                if ((output.length() > 6) && output.substring(3, 6).equals("msg")) {
                    //Puts output from form [{.1.},{.2.},...,{.N.}]
                    //into array[0] = {.1.}, array[1] = {.2.}, ..., array[N-1] = {.N.}
                    output = output.substring(1, output.length() - 1);
                    String msgs[] = output.split("\\},");
                    NumberOfMessages += msgs.length;
                    Log.d("THREAD", Integer.toString(NumberOfMessages));
                }
                //All claims sent and retreived
                if (return_counter == async_counter){

                    getMessages = false;
                    if (prevNumberOfMessages == 0){
                        //Do not send notification, first time running app
                        Log.d("THREAD", "First time getting messages.");
                    }
                    //TODO? Find a way to tell which claim has new messages.
                    //TODO? You get notification when you send messages too.
                    else if(prevNumberOfMessages < NumberOfMessages){
                        //New messages found
                        Log.d("THREAD", "Notification!");
                        //Provide notification
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(CONTEXT, CHANNEL_ID)
                                .setSmallIcon(R.drawable.google_logo)
                                .setContentTitle("AutoInsurance")
                                .setContentText("You have new messages.")
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(CONTEXT);
                        notificationManager.notify(1, builder.build());

                    }
                    else{
                        //No new messages found
                        Log.d("THREAD", "No new messages.");
                    }
                    async_counter = 0;
                    return_counter = 0;
                    prevNumberOfMessages = NumberOfMessages;
                    NumberOfMessages = 0;
                }
            }
            else {
                Log.d("THREAD", "Starting to compute messages.");
                //Output is list of all claims.
                //Puts output from form [{.1.},{.2.},...,{.N.}]
                //into array[0] = {.1.}, array[1] = {.2.}, ..., array[N-1] = {.N.}
                output = output.substring(1, output.length() - 1);
                String newOutput[] = output.split("\\},");
                claimIDS = new ArrayList<>();
                for (int i = 0; i < newOutput.length; i++) {
                    newOutput[i] = newOutput[i] + "}";
                }

                try {
                    for (String is : newOutput) {
                        JSONObject obj = new JSONObject(is);
                        Iterator<String> keys = obj.keys();
                        while (keys.hasNext()) {
                            //Get the claimID
                            Object temp = obj.get(keys.next());
                            String claimID;
                            if (temp instanceof Integer) {
                                claimID = Integer.toString((Integer) temp);
                            } else {
                                claimID = (String) temp;
                            }
                            //Skip over the Claim Title
                            if (keys.hasNext()){
                                keys.next();
                            }
                            //Add all ClaimIDs to ArrayList
                            claimIDS.add(claimID);
                        }
                    }
                    //Call getMessages, and set flag
                    getClaim();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        //Connection-checker thread
        else if (output.equals("0")) {
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