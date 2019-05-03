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

    /**
     *
     * @param cs The textview to display the status message. Can be blank, or non-existent.
     * @param id The Channel-ID used for notificaiton. As there is only on notifier, this ID
     *           is of little use and hardcoded.
     * @param con Pass "this" from caller, because its required.
     * @param sID Session ID. If null, the thread will deem itself a connection checker thread. If
     *            not-null, it will check for new messages given the Session ID.
     */
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

    /**
     *  Runs the created thread.
     *
     *  The SHUTDOWN-flag can be set from anywhere within the package, and
     *  will shut down the thread.
     *
     *  The CheckMessages-flag is set on creation by the constructor, and is based on if the
     *  SessionID is null or not-null.
     *
     *  The prevConnected-flag is set when there has been a connection, meaning that while the
     *  client is connected to the server, it will check less often if there is still a connection.
     *  This is because when first connected, it is assumed you are in a place where you will
     *  stay connected for a while.
     *
     *  The integer CONNECTION_TEST_TIMEOUT is the wait-time inbetween connection-attemps in ms.
     *  If the prevConnected-flag is set, it will wait for 3x as long.
     *
     */
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

    /**
     * This method gets all the claims given a sessionID.
     * This method is only called if the thread is a message-checker thread.
     * As with the other calls to the web-server,
     * the result will be handled in the processFinished()-method.
     */
    private void getHistory(){
        //Get all claims
        AsyncWebServiceCaller asyncTask = new AsyncWebServiceCaller();
        asyncTask.delegate = this;
        String[] args = {"listClaims", SESSION_ID};
        asyncTask.execute(args);
    }

    /**
     * The getClaim()-method gets all messages for all the claims received.
     * The getMessages-flag is set, telling processFinished()-method that the results it is about
     * to be given is messages for a given claim.
     */
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

    /**
     * This method is part of all classes that takes advantage of async ws-calls.
     * If the thread is a connection-checker, it ignores the entire mess that is the
     * checkMessages if-test. The async-caller returns 0 if the webserver is unavailanble, therefore
     * if the checkMessages-flag is not set, then all return calls that is not 0 is considered
     * positive (meaning that the server is available).
     *
     * If the checkMessages-flag is set, then we are either going to get the messages for a given
     * claim, or check to see if there are any new messages for a given claim.
     *
     * Further, if the getMessages-flag is set, then the @param output is a list of messages given a
     * specific claim. For every claim, we take the messages and count them. This is done using
     * a method that you will see/have seen throughout this project; its basically splitting the
     * JSON-object into a String-array. The number of messages is then grabbed by getting the
     * length of said String-array. This number is incrementet to a current counter.
     * After return_counter == async_counter (the number of claims we've received messages for
     * equals the number of claims we've done a async ws-call for), then we can check if there
     * are more messages now than for the previous check. If the prevNumberOfMessages-integer is 0,
     * then we assume that its the first time the thread is running, and there we wont notify the user.
     * Otherwise, if NumberOfMessages exceeds the prevNumberOfMessages, then there are new messages.
     * Will then send a notification. Note: does not distinguish between messages sent by user
     * and server. This is rather simple (not complex), but time-consuming and out of scope.
     * This would also make testing harder, as there are no simple way of adding messages
     * to be sent from the server in runtime.
     * For concurrency reasons, there is no simple way of telling what claim
     * we're currently handling messages for. If we could always assume that the list of claims
     * starts with 1, and then iterates up until end end, then we could solve this with a counter.
     * Otherwise, we have the possibility of still using a counter, and use that as indexing in an
     * array, but this is not done as it seems to be out of scope.
     *
     * @param output The output from the calls the the Web-Server made in this class.
     *
     */
    @Override
    public void processFinish(String output) {
        //Message-checker thread
        if (checkMessages){
            if (getMessages){
                //All messages are retreived.
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