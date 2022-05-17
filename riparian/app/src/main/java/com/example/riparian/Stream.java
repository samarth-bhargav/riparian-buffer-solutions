package com.example.riparian;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class Stream {
    public double latitude;
    public double longitude;
    public ArrayList<Double> coords;
    public int MMI;
    public String temperature;
    public String name;
    public Stream (String name, double latitude, double longitude, ArrayList<Double> coords, int MMI, String temperature){
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.coords = coords;
        this.MMI = MMI;
        this.temperature = temperature;
    }
    public Stream(){}
    @Override
    public String toString(){
        String ret = "Stream Name: " + name +
                "\nStream Coordinates: " + coords
                + "\nStream MMI: " + MMI +
                "\nStream Temperature: " + temperature;
        return ret;
    }
}
