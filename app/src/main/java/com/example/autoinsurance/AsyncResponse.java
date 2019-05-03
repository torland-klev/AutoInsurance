package com.example.autoinsurance;


//Interface needed to receive output from async web-server calls
public interface AsyncResponse {
    void processFinish(String output);
}
