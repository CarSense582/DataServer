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
        //Testing send response
        results.putString("broadCastResp","DS responding to CM broadcast");
        //Testing sending array of ids
        ArrayList<String> otherServices = results.getStringArrayList("dsServices");
        if(otherServices == null) {
            otherServices = new ArrayList<String>();
        }
        String serviceId = context.getResources().getString(R.string.ds_cm_id);
        otherServices.add(serviceId);
        System.out.println("DS id: " + serviceId);
        results.putStringArrayList("dsServices", otherServices);
        //Add service dependent map
        HashMap<String,Object> map = new HashMap<String,Object>();
        map.put("a", 1);
        map.put("b", 5);
        results.putSerializable(serviceId, map);
    }

}