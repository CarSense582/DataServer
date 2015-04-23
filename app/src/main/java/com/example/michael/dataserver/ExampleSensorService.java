package com.example.michael.dataserver;

/**
 * Created by michael on 4/23/15.
 */
public class ExampleSensorService extends DataService{
    @Override
    public long maxReadResponseTime() {
        return 100;
    }
    @Override
    public long maxWriteResponseTime() {
        return 10;
    }
    @Override
    public long sensorPeriod() {
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
