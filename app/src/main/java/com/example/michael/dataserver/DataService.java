package com.example.michael.dataserver;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.Toast;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

abstract public class DataService extends Service {
    //All abstract functions
    abstract public int maxReadResponseTime();
    abstract public int sensorPeriod();
    //Driver modelled methods
    abstract public void open();
    abstract public void readAsync();
    abstract public void readPeriodic();
    abstract public void close();


    public SensorData sensor;
    public int max_read_response_time;
    public int sensor_period;
    final Lock lock = new ReentrantLock();
    final Condition newRead  = lock.newCondition();
    final Condition readFinished = lock.newCondition();
    public DataService() {
        max_read_response_time = maxReadResponseTime();
        sensor_period          = sensorPeriod();
    }

    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            int msgType = msg.what;
            switch (msgType) {
                default:
                    try {
                        // Incoming data
                        Message resp = Message.obtain(null, 5);
                        Bundle bResp = new Bundle();
                        boolean fresh = false;
                        lock.lock();
                        newRead.signal();
                        try {
                            fresh = readFinished.await(max_read_response_time, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        bResp.putSerializable("respMap", sensor.getFields());
                        lock.unlock();
                        bResp.putBoolean("fresh",fresh);
                        resp.setData(bResp);
                        msg.replyTo.send(resp);
                    } catch (RemoteException e) {

                        e.printStackTrace();
                    }
                    //super.handleMessage(msg);
                    break;
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        Toast.makeText(getApplicationContext(), "DS binding", Toast.LENGTH_SHORT).show();
        open();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    lock.lock();
                    try {
                        if(newRead.await(sensor_period, TimeUnit.MILLISECONDS)) {
                            //Got signal within period
                            readAsync();
                        } else {
                            //Time elapsed
                            readPeriodic();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    readFinished.signal();
                    lock.unlock();
                }
            }
        }).start();
        return mMessenger.getBinder();
    }

    //Stop when unbound
    @Override
    public boolean onUnbind(Intent intent) {
        Toast.makeText(getApplicationContext(), "DS unbinding", Toast.LENGTH_SHORT).show();
        close();
        return false; //Don't use rebind
    }
}