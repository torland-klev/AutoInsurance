package com.example.autoinsurance;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

//Needed to authenticate accounts with account manager.

public class AuthenticatorService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        AccountAuthenticator authenticator = new AccountAuthenticator(this);
        return authenticator.getIBinder();
    }
}