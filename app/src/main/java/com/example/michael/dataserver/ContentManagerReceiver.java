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

import java.util.ArrayList;
import java.util.HashMap;

abstract public class ContentManagerReceiver extends BroadcastReceiver {
    abstract public SensorData getSensor();
    abstract public String getServiceId(Context c);
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
        String serviceId = getServiceId(context);
        otherServices.add(serviceId);
        results.putStringArrayList("dsServices", otherServices);
        //Add service dependent map
        results.putSerializable(serviceId, getSensor().getFields());
    }

}