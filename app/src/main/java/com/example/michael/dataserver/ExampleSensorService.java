package com.example.michael.dataserver;

/**
 * Created by michael on 4/23/15.
 */
public class ExampleSensorService extends DataService{
    @Override
    public int maxReadResponseTime() {
        return 100;
    }
    @Override
    public int sensorPeriod() {
        return 1000;
    }
    //Driver modelled methods
    @Override
    public void open(){
        sensor = new ExampleSensor();
    }
    @Override
    public void readAsync() {
        ((ExampleSensor) sensor).time ++;
    }
    @Override
    public void readPeriodic() {
        ((ExampleSensor) sensor).time += 100;
    }
    @Override
    public void close() {
        //Do nothing
    }
}
