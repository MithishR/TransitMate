package com.example.transitmate;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.*;

import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import org.json.*;

import java.io.*;
import java.net.*;
import java.util.List;


public class ConfirmTripActivity extends AppCompatActivity implements OnMapReadyCallback{

    TextView pickup, dest, dandt, name, cost;
    MapView mapView;
    Polyline routePolyline;
    LatLng OL, DL;
    GoogleMap gmap;
    Bundle driverInfo;
    ImageButton confirmBack;
    Button confirm;
    Marker originMarker, destMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_trip);
        OL = TripPassengerFragment.originLatLng;
        DL = TripPassengerFragment.destinationLatLng;
        pickup = (TextView) findViewById(R.id.pickup);
        dest = (TextView) findViewById(R.id.destin);
        dandt = (TextView) findViewById(R.id.dandt);
        name = (TextView) findViewById(R.id.driver);
        cost = (TextView) findViewById(R.id.price);
        mapView = (MapView) findViewById(R.id.mapConfirm);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
//        mapView.getMapAsync(new OnMapReadyCallback() {
//            @Override
//            public void onMapReady(@NonNull GoogleMap googleMap) {
//                gmap = googleMap;
//                startMap();
//            }
//        });

        confirmBack = (ImageButton) findViewById(R.id.confirmBack);
        confirm = (Button) findViewById(R.id.confirm);

        driverInfo = getIntent().getExtras();
        pickup.setText(driverInfo.getString("pickup"));
        dest.setText(driverInfo.getString("dest"));
        name.setText(driverInfo.getString("name"));
        dandt.setText(driverInfo.getString("dandt"));
        cost.setText("CAD " + driverInfo.getString("cost"));

        // TODO: edit map

        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent goToPaymentMethods = new Intent(ConfirmTripActivity.this, PaymentMethodsActivity.class);
                goToPaymentMethods.putExtras(driverInfo);
                startActivity(goToPaymentMethods);
            }
        });

        confirmBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent backToMessages = new Intent(ConfirmTripActivity.this, MainActivity.class);
                Bundle info = new Bundle();
                info.putString("fragment", "messages");
                backToMessages.putExtras(info);
                startActivity(backToMessages);
            }
        });



    }
    public void onMapReady(GoogleMap googleMap) {
        gmap = googleMap;

        // Add markers for origin and destination
        originMarker = gmap.addMarker(new MarkerOptions().position(OL).title("Origin"));
        destMarker = gmap.addMarker(new MarkerOptions().position(DL).title("Destination"));

        // Move camera to show both markers
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(originMarker.getPosition());
        builder.include(destMarker.getPosition());
        LatLngBounds bounds = builder.build();
        gmap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));

        String directionsUrl = "https://maps.googleapis.com/maps/api/directions/json" +
                "?origin=" + OL +"&destination=" + DL + "&key=AIzaSyCGzZ4JWj8C2SMqGkvkuCpbZiIj0lzM9QY";
        new FetchDirectionsTask().execute(directionsUrl);
    }
    private class FetchDirectionsTask extends AsyncTask<String, Void, String> {
//        @SuppressLint("StaticFieldLeak")
        @Override
        protected String doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = urlConnection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder result = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                return result.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        @SuppressLint("StaticFieldLeak")
        @Override
        protected void onPostExecute(String directionsData) {
            if (directionsData != null) {
                drawRoute(directionsData);
            }
        }
    }
    private void drawRoute(String directionsData) {
        try {
            JSONObject jsonObject = new JSONObject(directionsData);
            JSONArray routes = jsonObject.getJSONArray("routes");

            if (routes.length() > 0) {
                JSONObject route = routes.getJSONObject(0);
                JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                String encodedPolyline = overviewPolyline.getString("points");
                List<LatLng> decodedPolyline = PolyUtil.decode(encodedPolyline);
                PolylineOptions options = new PolylineOptions().addAll(PolyUtil.decode(encodedPolyline));
                routePolyline = gmap.addPolyline(options);
                for (LatLng point : decodedPolyline) {
                    Log.d("PolylinePoint", "Lat: " + point.latitude + ", Lng: " + point.longitude);
                }

                // Move camera to show both markers and the route
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(originMarker.getPosition());
                builder.include(destMarker.getPosition());
                for (LatLng point : decodedPolyline) {
                    builder.include(point);
                }
                LatLngBounds bounds = builder.build();
                gmap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}

}