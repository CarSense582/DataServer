package com.example.michael.dataserver;

import android.content.Context;

import com.example.michael.dataserverlib.ContentManagerReceiver;
import com.example.michael.dataserverlib.SensorData;

/**
 * Created by michael on 4/23/15.
 */
public class ExampleSensorReceiver extends ContentManagerReceiver {
    @Override
    public SensorData getSensor() {
        return new ExampleSensor();
    }
    @Override
    public String getServiceId(Context context) {
        return context.getResources().getString(R.string.ds_cm_id);
    }
}
