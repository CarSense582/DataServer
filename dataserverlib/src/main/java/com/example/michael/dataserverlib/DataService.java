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

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

abstract public class DataService<T extends SensorData> extends Service {
    //All abstract functions
    //Driver modelled methods
    abstract public void open();
    abstract public void readAsync();
    abstract public void readPeriodic();
    abstract public void writeAsync();
    abstract public void writePeriodic();
    abstract public void close();

    //Setup times
    static final private long NO_SENSOR_PERIOD        = -1;
    static final private long DEFAULT_READ_RESP_TIME  = 100;
    static final private long DEFAULT_WRITE_RESP_TIME = 100;
    public class ServiceTimes {
        public long maxReadResponseTime, maxWriteResponseTime, sensorPeriod;
    }
    //Overwrite to specify own times
    public ServiceTimes setupTimes() {
        ServiceTimes times = new ServiceTimes();
        times.sensorPeriod         = NO_SENSOR_PERIOD;
        times.maxReadResponseTime  = DEFAULT_READ_RESP_TIME;
        times.maxWriteResponseTime = DEFAULT_WRITE_RESP_TIME;
        return times;
    }


    public T sensor;
    protected ServiceTimes serviceTimes;

    final Lock lock = new ReentrantLock(); //lock sensor
    final Condition newRead  = lock.newCondition();
    final Condition readFinished = lock.newCondition();
    public long last_read_time;
    final Condition newWrite  = lock.newCondition();
    final Condition writeFinished = lock.newCondition();
    public long last_write_time;

    public DataService() {
        serviceTimes    = setupTimes();
        sensor          = null;
        last_read_time  = 0;
        last_write_time = 0;
    }

    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            int msgType = msg.what;
            switch (msgType) {
                case DataServerLibConstants.WRITE_MSG: {
                    //try {
                    // Incoming data
                    Message resp = Message.obtain(null, DataServerLibConstants.WRITE_REPLY_MSG);
                    Bundle bResp = new Bundle();
                    boolean fresh = false;
                    lock.lock();
                    sensor.setFields((HashMap<String, Object>) msg.getData().getSerializable(DataServerLibConstants.WRITE_MAP));
                    newWrite.signal();
                    try {
                        fresh = writeFinished.await(serviceTimes.maxWriteResponseTime, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    lock.unlock();
                    bResp.putBoolean("fresh", fresh);
                    resp.setData(bResp);
                    //msg.replyTo.send(resp);
                    //}
                }
                    /*catch (RemoteException e) {
                        e.printStackTrace();
                    }*/
                    break;
                case DataServerLibConstants.READ_MSG:
                    try {
                        // Incoming data
                        Message resp = Message.obtain(null, 5);
                        Bundle bResp = new Bundle();
                        boolean fresh = false;
                        lock.lock();
                        newRead.signal();
                        try {
                            fresh = readFinished.await(serviceTimes.maxReadResponseTime, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        bResp.putSerializable(DataServerLibConstants.READ_REPLY_MAP, sensor.getFields());
                        lock.unlock();
                        bResp.putBoolean("fresh",fresh);
                        resp.setData(bResp);
                        msg.replyTo.send(resp);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    //super.handleMessage(msg);
                    break;
                default:
                    System.out.println("Unhandled Message");
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
                        if(diffTime > serviceTimes.sensorPeriod) {
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
                        if(diffTime > serviceTimes.sensorPeriod) {
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