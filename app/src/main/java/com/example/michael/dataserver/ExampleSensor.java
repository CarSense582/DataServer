package com.example.michael.dataserver;

import com.example.michael.dataserverlib.SensorData;

/**
 * Created by michael on 4/22/15.
 */
public class ExampleSensor  extends SensorData {
    public int time;
    public int factor;
    ExampleSensor() {
        time = 0;
        factor = 1;
    }
}
