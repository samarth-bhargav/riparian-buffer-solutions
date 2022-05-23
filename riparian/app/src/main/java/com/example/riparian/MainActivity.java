package com.example.riparian;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private EditText streamName;
    private TextView riparianData;
    private Button enterButton;
    StreamDatabase db;
    ProgressBar progressBar;
    private OkHttpClient client = new OkHttpClient();
    int increments = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            db = new StreamDatabase(getApplicationContext());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        progressBar = findViewById(R.id.progressBar);

        streamName = findViewById(R.id.streamName);
        riparianData = findViewById(R.id.riparianData);
        enterButton = (Button) findViewById(R.id.enterButton);
        enterButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view){
        switch (view.getId()){
            case R.id.enterButton:
                String stream = streamName.getText().toString();
                if (!db.isStream(stream)){
                    streamName.setError("Invalid Stream Name");
                    streamName.requestFocus();
                    streamName.setText("");
                    break;
                }
                stream = stream.toUpperCase(Locale.ROOT);
                ArrayList<Double> streamCoords = db.getCoordsForStream(stream);
                ArrayList<Double> coords = null;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.VISIBLE);
                    }
                });
                String dist = "", data = "";

                try {
                    coords = getNearestCoords(stream);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (coords == null){
                    dist = "Greater than 60";
                    data = "null";
                }
                else{
                    dist = ((Double)distance(streamCoords.get(0),
                            coords.get(0), streamCoords.get(1), coords.get(1), 0.0, 0.0)).toString();
                    data = coords.toString();
                }
                String display = "Nearest Impervious Surface " + data
                        + "\n" + "Buffer Width: " + dist + " meters\n"
                        + db.streamData.get(stream);
                riparianData.setText(display);
                break;
        }
    }
    public ArrayList<Double> getNearestCoords(String stream) throws org.json.JSONException, IOException {
        final ArrayList<Double>[] returnCoords = new ArrayList[]{null};
        stream = stream.toUpperCase(Locale.ROOT);
        if (!db.isStream(stream)) {
            streamName.setText("Please Enter a Valid Stream Name");
        }
        final ArrayList<Double>[] coords = new ArrayList[]{db.getCoordsForStream(stream)};
        Double latitude = coords[0].get(0);
        Double longitude = coords[0].get(1);

        return getCoordsFromJSON(run(latitude, longitude));
    }
    public ArrayList<Double> getCoordsFromJSON(String response) throws org.json.JSONException{
        JSONObject points = new JSONObject(response);
        if (points.isNull("snappedPoints")){
            return null;
        }
        JSONArray roads = (JSONArray)points.get("snappedPoints");
        JSONObject firstRoad = (JSONObject) ((JSONObject) roads.get(0)).get("location");
        ArrayList<Double> coord = new ArrayList<>();
        coord.add((Double) firstRoad.get("latitude"));
        coord.add((Double) firstRoad.get("longitude"));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
            }
        });
        return coord;
    }
    public String run(double latitude, double longitude) throws IOException {
        final String[] inputLine = new String[1];
        final String[] result = new String[1];

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean successful = false;
                while (!successful){
                    String qry = (latitude+0.0001*increments) + "," + longitude + "|"
                            + (latitude-0.0001*increments) + "," + longitude + "|"
                            + latitude + "," + (longitude+0.0001*increments) + "|"
                            + latitude + "," + (longitude-0.0001*increments);
                    String url = "https://roads.googleapis.com/v1/nearestRoads?points="
                            + qry
                            + "&key="
                            + "USE_YOUR_ROADS_API_KEY";
                    try{
                        URL api = new URL(url);
                        HttpURLConnection connection = (HttpURLConnection) api.openConnection();
                        connection.connect();

                        InputStreamReader streamReader = new InputStreamReader(connection.getInputStream());
                        BufferedReader reader = new BufferedReader(streamReader);
                        StringBuilder stringBuilder = new StringBuilder();

                        while ((inputLine[0] = reader.readLine()) != null) {
                            stringBuilder.append(inputLine[0]);
                        }

                        reader.close();
                        streamReader.close();

                        result[0] = stringBuilder.toString();
                        if (getCoordsFromJSON(result[0]) == null){
                            increments++;
                        }
                        else{
                            successful = true;
                        }
                    }
                    catch (Exception ex){
                        ex.printStackTrace();
                    }
                }
            }
        });
        thread.start();
        try{
            thread.join();
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return result[0];
    }
    public static double distance(double lat1, double lat2, double lon1,
                                  double lon2, double el1, double el2) {

        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return BigDecimal.valueOf(Math.sqrt(distance)).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

}
