package com.example.michael.dataserver;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.Toast;

public class DataService extends Service {
    public DataService() {
    }

    /** Command to the service to display a message */
    static final int MSG_SAY_HELLO = 1;

    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            int msgType = msg.what;
            switch (msgType) {
                /*
                case MSG_SAY_HELLO:
                    Toast.makeText(getApplicationContext(), "hello!", Toast.LENGTH_SHORT).show();
                    break;
                 */
                default:
                    System.out.println(msg);
                    try {
                        // Incoming data
                        String data = msg.getData().getString("data");
                        Toast.makeText(getApplicationContext(), "DS received " + msg.getData().getString("data"),
                                Toast.LENGTH_SHORT).show();
                        Message resp = Message.obtain(null, 5);
                        Bundle bResp = new Bundle();
                        bResp.putString("respData", data.toUpperCase());
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
        return mMessenger.getBinder();
    }

    //Stop when unbound
    @Override
    public boolean onUnbind(Intent intent) {
        Toast.makeText(getApplicationContext(), "DS unbinding", Toast.LENGTH_SHORT).show();
        return false; //Don't use rebind
    }
}