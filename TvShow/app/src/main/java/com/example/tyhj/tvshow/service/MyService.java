package com.example.tyhj.tvshow.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.IntDef;

import com.example.tyhj.tvshow.utils.Connect;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EService;


@EService
public class MyService extends Service {

    public static String IP="115.28.16.220";
    public static String IP3="192.168.43.18";

    public MyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }


    @Background
    void start(){
        Connect.getInstance(IP,9890,null);
    }

}
