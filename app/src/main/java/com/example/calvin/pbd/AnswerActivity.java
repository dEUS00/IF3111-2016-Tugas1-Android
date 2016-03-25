package com.example.calvin.pbd;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.ext.Locator2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class AnswerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_answer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.answer_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        getSupportActionBar().setTitle("Submit answer");

        ((Button) findViewById(R.id.submitButton)).setOnClickListener(
            new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String answer = ((Spinner) findViewById(R.id.spinner)).getSelectedItem().toString();
                    new SubmitAnswer().execute(answer);
                }
            }
        );
    }

    private class SubmitAnswer extends AsyncTask<String, Void, String> {
        private double latitude;
        private double longitude;

        @Override
        protected String doInBackground(String... params) {
            Looper.prepare();
            SharedPreferences sp = getSharedPreferences("PBD", Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();

            try {
                Socket socket = new Socket("167.205.34.132", 3111);

                try {
                    LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    LocationListener ll = new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {}

                        @Override
                        public void onStatusChanged(String provider, int status, Bundle extras) {}

                        @Override
                        public void onProviderEnabled(String provider) {}

                        @Override
                        public void onProviderDisabled(String provider) {}
                    };
                    lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, ll);
                    Location loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    latitude = loc.getLatitude();
                    longitude = loc.getLongitude();
                }
                catch (SecurityException e) {
                    Log.d("MyApp", e.toString());
                }

                JSONObject request = new JSONObject();
                request.put("com", "answer");
                request.put("nim", "13513077");
                request.put("answer", params[0]);
                request.put("latitude", latitude);
                request.put("longitude", longitude);
                request.put("token", sp.getString("token", null));

                InputStream inputStream = socket.getInputStream();
                OutputStream outputStream = socket.getOutputStream();

                PrintWriter out = new PrintWriter(outputStream);
                BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));

                out.println(request.toString());
                out.flush();
                String response = in.readLine();
                socket.close();
                return response;
            }
            catch (UnknownHostException e) {
                Log.d("MyApp", e.toString());
            }
            catch (IOException e) {
                Log.d("MyApp", e.toString());
            }
            catch (JSONException e) {
                Log.d("MyApp", e.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String response) {
            try {
                JSONObject responseJSON = new JSONObject(response);

                SharedPreferences sp = getSharedPreferences("PBD", Activity.MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();

                String status = responseJSON.getString("status");

                editor.putString("status", status);
                if (status.equals("ok")) {
                    editor.putLong("latitude", Double.doubleToLongBits(responseJSON.getDouble("latitude")));
                    editor.putLong("longitude", Double.doubleToLongBits(responseJSON.getDouble("longitude")));
                    editor.commit();
                    setResult(MapsActivity.OK);
                }
                else if (status.equals("wrong_answer")) {
                    setResult(MapsActivity.WRONG);
                }
                else if (status.equals("finish")) {
                    setResult(MapsActivity.FINISH);
                }

                Toast.makeText(AnswerActivity.this, response, Toast.LENGTH_LONG).show();
                editor.putString("token", responseJSON.getString("token"));
                editor.commit();
                finish();
            }
            catch (JSONException e) {
                Log.d("PostExecute", e.toString());
            }
        }
    }
}