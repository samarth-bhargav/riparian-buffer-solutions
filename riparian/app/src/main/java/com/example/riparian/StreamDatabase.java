package com.example.riparian;

import android.content.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;
import java.util.TreeMap;

public class StreamDatabase {
    TreeMap<String, Stream> streamData;
    ArrayList<Stream> streams = new ArrayList<>();
    int num = 0;
    public StreamDatabase(Context context) throws FileNotFoundException {
        streamData = new TreeMap<>();
        InputStream is = null;
        try {
            is = context.getAssets().open("StreamNames.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
        Scanner in = new Scanner(is);
        in.nextLine();
        while (true){
            String s = in.nextLine().trim();
            if (!s.equals("Latitude")){
                streams.add(new Stream());
                streams.get(num).name = s.toUpperCase(Locale.ROOT);
                num++;
            }
            else{
                break;
            }
        }
        for (int i = 0; i < num; i++) {
            double lat = Double.parseDouble(in.nextLine().trim().split(" ")[0]);
            streams.get(i).latitude = lat;
        }
        in.nextLine();
        for (int i = 0; i < num; i++){
            double lng = Double.parseDouble(in.nextLine().trim().split(" ")[0]);
            streams.get(i).longitude = lng;
        }
        for (int i = 0; i < num; i++){
            ArrayList<Double> coords = new ArrayList<>();
            coords.add(streams.get(i).latitude);
            coords.add(streams.get(i).longitude);
            streams.get(i).coords = coords;
        }
        in.nextLine();
        for (int i = 0; i < num; i++){
            String macro = (in.nextLine().trim());
            if (macro.equals("")){
                macro = "No Macroinvertebrate Data Available";
            }
            try{
                int M = Integer.parseInt(macro);
                streams.get(i).MMI = M;
            }
            catch (Exception ignored){}
        }
        in.nextLine();
        for (int i = 0; i < num; i++){
            String s = in.nextLine().trim();
            if (s.equals("")){
                s = "No Temperature Data Found";
            }
            try{
                double x = Double.parseDouble(s);
                if (x < 30.0){
                    s += " (Assumed Celsius)";
                }
            }
            catch (Exception ignored){}
            streams.get(i).temperature = s;
        }
        for (int i = 0; i < num; i++){
            streamData.put(streams.get(i).name, streams.get(i));
        }
    }
    public boolean isStream(String s){
        s = s.toUpperCase(Locale.ROOT);
        return streamData.containsKey(s);
    }
    public ArrayList<Double> getCoordsForStream(String s){
        if (!isStream(s)){
            return null;
        }
        return streamData.get(s).coords;
    }
}
