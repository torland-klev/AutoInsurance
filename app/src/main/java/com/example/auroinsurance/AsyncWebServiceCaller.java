package com.example.auroinsurance;

import android.os.AsyncTask;
import android.util.Log;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.util.Arrays;

public class AsyncWebServiceCaller extends AsyncTask<String, Void, String> {

    private final String NAMESPACE = "http://pt.ulisboa.tecnico.sise.autoinsure.ws/";
    private final String URL = "http://10.0.2.2:8080/AutoInSureWS?WSDL";
    private static final String serviceName = "AutoInsureWS";
    public AsyncResponse delegate = null;

    @Override
    protected String doInBackground(String... params) {
        String s;
        String[] rest = Arrays.copyOfRange(params, 1, params.length );
        try {
            s = makeRequest(params[0], rest);
        } catch (Exception e){
            s = "Call did not work";
            e.printStackTrace();
        }
        Log.i("AWSC.doInBackground", s);
        return s;
    }

    protected void onPostExecute(String result){
        delegate.processFinish(result);
    }

    private String makeRequest(String method, String... args) throws Exception{
        SoapObject request = new SoapObject(NAMESPACE, method);
        int paramCounter = 0;
        for(String arg : args){
            request.addProperty("arg" + paramCounter++, arg);
        }
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.setOutputSoapObject(request);
        HttpTransportSE androidHttpTransport = new HttpTransportSE(URL);
        String SOAP_ACTION = "\"" + NAMESPACE + serviceName + "/" + method + "\"";
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
