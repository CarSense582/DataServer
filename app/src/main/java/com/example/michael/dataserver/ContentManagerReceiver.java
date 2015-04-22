/**
 * Created by michael on 4/22/15.
 */

package com.example.michael.dataserver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

public class ContentManagerReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        System.out.println(intent);
        if(intent.getAction() == "com.example.michael.contentmanager.broadcaster") {
            System.out.println("oh yeah");
        }
        CharSequence text = "Hello toast!";
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
        Bundle results = getResultExtras(true);
        results.putString("broadCastResp","DS responding to CM broadcast");
    }

}