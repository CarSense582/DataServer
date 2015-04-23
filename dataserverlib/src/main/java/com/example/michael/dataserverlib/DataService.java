package com.example.michael.dataserverlib;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.Toast;

import com.example.michael.dataserverlib.SensorData;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

abstract public class DataService extends Service {
    //All abstract functions
    abstract public long maxReadResponseTime();
    abstract public long maxWriteResponseTime();
    abstract public long sensorPeriod();
    //Driver modelled methods
    abstract public void open();
    abstract public void readAsync();
    abstract public void readPeriodic();
    abstract public void writeAsync();
    abstract public void writePeriodic();
    abstract public void close();


    public SensorData sensor;
    public long max_read_response_time;
    public long max_write_response_time;
    public long sensor_period;

    final Lock lock = new ReentrantLock(); //lock sensor
    final Condition newRead  = lock.newCondition();
    final Condition readFinished = lock.newCondition();
    public long last_read_time;
    final Condition newWrite  = lock.newCondition();
    final Condition writeFinished = lock.newCondition();
    public long last_write_time;

    public DataService() {
        max_read_response_time = maxReadResponseTime();
        max_write_response_time = maxWriteResponseTime();
        sensor_period          = sensorPeriod();
        last_read_time         = 0;
        last_write_time        = 0;
    }

    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            int msgType = msg.what;
            switch (msgType) {
                case 23:
                    try {
                        // Incoming data
                        Message resp = Message.obtain(null, 4);
                        Bundle bResp = new Bundle();
                        boolean fresh = false;
                        lock.lock();
                        sensor.setFields((HashMap<String,Object>)msg.getData().getSerializable("map"));
                        newWrite.signal();
                        try {
                            fresh = writeFinished.await(max_write_response_time, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        lock.unlock();
                        bResp.putBoolean("fresh",fresh);
                        resp.setData(bResp);
                        msg.replyTo.send(resp);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
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
                    //Read
                    boolean readUpdated = false;
                    try {
                        long curTime= System.currentTimeMillis();
                        long diffTime = curTime - last_read_time;
                        if(diffTime > sensor_period) {
                            last_read_time = curTime;
                            readPeriodic();
                        }
                        if(newRead.await(1, TimeUnit.MILLISECONDS)) { //If not instantaneous, move on
                            //Got signal
                            readAsync();
                            readUpdated = true;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(readUpdated) {
                        readFinished.signal();
                    }

                    //Write
                    boolean writeUpdated = false;
                    try {
                        long curTime= System.currentTimeMillis();
                        long diffTime = curTime - last_write_time;
                        if(diffTime > sensor_period) {
                            last_write_time = curTime;
                            writePeriodic();
                        }
                        if(newWrite.await(1, TimeUnit.MILLISECONDS)) { //If not instantaneous, move on
                            //Got signal
                            writeAsync();
                            writeUpdated = true;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(writeUpdated) {
                        writeFinished.signal();
                    }
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