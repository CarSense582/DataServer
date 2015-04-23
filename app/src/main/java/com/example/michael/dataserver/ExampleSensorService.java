package com.example.michael.dataserver;

import com.example.michael.dataserverlib.DataService;

import java.util.HashMap;

/**
 * Created by michael on 4/23/15.
 */
public class ExampleSensorService extends DataService<ExampleSensor> {
    @Override
    public ServiceTimes setupTimes() {
        ServiceTimes times = super.setupTimes();
        times.maxReadResponseTime  = 100;
        times.maxWriteResponseTime = 10;
        times.sensorPeriod         = 1000;
        return times;
    }
    //Driver modelled methods
    @Override
    public void open(){
        sensor = new ExampleSensor();
    } //need to initialize
    @Override
    public void readAsync() {
        sensor.time ++;
    }
    @Override
    public void readPeriodic() {
        ((ExampleSensor) sensor).time += 100;
    }
    @Override
    public void writeAsync() {
        //Write to sensor
    }
    @Override
    public void writePeriodic() {
        //Write to sensor periodically
    }
    @Override
    public void close() {
        //Do nothing
    }
}
