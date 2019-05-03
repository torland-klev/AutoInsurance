package com.example.autoinsurance;

import android.os.AsyncTask;
import android.util.Log;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.util.Arrays;

//This class is more or less equal the code-snippet we were provided.

public class AsyncWebServiceCaller extends AsyncTask<String, Void, String> {

    AsyncResponse delegate = null; //Package private
    private final String URL = "http://10.0.2.2:8080/AutoInSureWS?WSDL";

    @Override
    protected String doInBackground(String... params) {

        //Check if Async thread is for connection testing.
        if (params[0].equals("TEST_CONNECTION")){
            if (testConnection()){
                return "1";
            }
            return "0";
        }

        String s;

        //Assumes METHOD is first param, rest of params are specific to a method
        String[] rest = Arrays.copyOfRange(params, 1, params.length );
        try {
            s = makeRequest(params[0], rest);
        } catch (Exception e){
            e.printStackTrace();
            return "-1";
        }
        // Sometimes the sessionID fucks up. Will then logout user.
        if (s.equals("invalid sessionId")){
            return "true";
        }
        Log.i("AWSC.doInBackground", s);
        return s;
    }

    protected void onPostExecute(String result){
        delegate.processFinish(result);
    }

    //Simple connection checker;
    private Boolean testConnection(){
        HttpTransportSE androidHttpTransport = new HttpTransportSE(URL, 2000);
        try {
            androidHttpTransport.getServiceConnection().getResponseCode();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String makeRequest(String method, String... args) throws Exception{

        //Using SOAP for making requests as WebServer is WDSL.
        String NAMESPACE = "http://pt.ulisboa.tecnico.sise.autoinsure.ws/";
        SoapObject request = new SoapObject(NAMESPACE, method);
        int paramCounter = 0;
        for(String arg : args){
            request.addProperty("arg" + paramCounter++, arg);
        }
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.setOutputSoapObject(request);
        int TIMEOUT = 4000;
        HttpTransportSE androidHttpTransport = new HttpTransportSE(URL, TIMEOUT);
        String SERVICE_NAME = "AutoInsureWS";
        String SOAP_ACTION = "\"" + NAMESPACE + SERVICE_NAME + "/" + method + "\"";
        Log.i("AWSC.makeRequest", SOAP_ACTION);
        try {
            androidHttpTransport.call(SOAP_ACTION, envelope);
        } catch (Exception e) {
            e.printStackTrace();
        }
        SoapPrimitive resultsRequestSOAP = (SoapPrimitive) envelope.getResponse();
        return resultsRequestSOAP.toString();
    }
}
