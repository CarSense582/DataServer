package com.example.michael.dataserverlib;

import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * Created by michael on 4/22/15.
 */
public class SensorData {
    public HashMap<String,Object> getFields() {
        HashMap<String,Object> map = new HashMap<String,Object>();
        Class c = this.getClass();
        for(Field f : c.getDeclaredFields()) {
            try {
                map.put(f.getName(), f.get(this));
            } catch (IllegalAccessException e) {
                System.out.println("not allowed to get field");
            }
        }
        return map;
    }
    public void setFields(HashMap<String,Object> map) {
        Class c = this.getClass();
        for(String k : map.keySet()) {
            try {
                Field f = c.getDeclaredField(k);
                try {
                    f.set(this, map.get(k));
                } catch (IllegalAccessException e) {
                    //Tried to set something we didn't have access to
                    System.out.println("Can't set " + k);
                }
            } catch (NoSuchFieldException e) {
                //Just drop on floor
                System.out.println("No field " + k);
            }
        }
    }
}
