package com.example.michael.dataserver;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DataService extends Service {
    public ExampleSensor sensor;
    final Lock lock = new ReentrantLock();
    final Condition newRead  = lock.newCondition();
    final Condition readFinished = lock.newCondition();
    public DataService() {
    }

    static public class ContentManagerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println(intent);
            if(intent.getAction() == "com.example.michael.contentmanager.broadcaster") {
                System.out.println("oh yeah");
            }
            Bundle results = getResultExtras(true);
            //Add this service id to list
            ArrayList<String> otherServices = results.getStringArrayList("dsServices");
            if(otherServices == null) {
                otherServices = new ArrayList<String>();
            }
            String serviceId = context.getResources().getString(R.string.ds_cm_id);
            otherServices.add(serviceId);
            results.putStringArrayList("dsServices", otherServices);
            //Add service dependent map
            results.putSerializable(serviceId, (new ExampleSensor()).getFields());
        }

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
                        lock.lock();
                        newRead.signal();
                        try {
                            readFinished.await(100, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        bResp.putSerializable("respMap", sensor.getFields());
                        lock.unlock();
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
        sensor = new ExampleSensor();
        Toast.makeText(getApplicationContext(), "DS binding", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    lock.lock();
                    try {
                        if(newRead.await(1000, TimeUnit.MILLISECONDS)) {
                            //Got signal within a second
                            sensor.time ++;
                        } else {
                            //Time elapsed
                            sensor.time += 100;
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
        return false; //Don't use rebind
    }
}