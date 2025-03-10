package com.example.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.gms.common.api.Status;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

//changed fragmentactivity to appcombatactivity
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener{

    //search bar




    //for navigation menu
    private DrawerLayout myDrawer;
    private ActionBarDrawerToggle myToggle;

    private GoogleMap mMap;
    LocationManager locationManager;
    LocationListener locationListener;
    Location lastKnownLocation;
    LatLng myCurrentlocation;
    private EditText editLocation = null;



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if ( requestCode == 1){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1200,10,locationListener);
                }
            }
        }
    }




    public class DownloadTask extends AsyncTask<String,Void, String>{

        @Override
        protected String doInBackground(String... urls) {
            String result = "";
            URL url;
            HttpURLConnection urlConnection;


            try {
                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in  = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);
                int data = reader.read();
                while (data!= -1){
                    char current = (char) data;
                    result += current;
                    data= reader.read();
                }
                return result;

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            String message = "";
            try {
                JSONObject jsonObject = new JSONObject(result);
                JSONArray a = jsonObject.getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates");
                Log.i("Coordinates", a.toString());
                Double latitudeRange = 0.02;
                Double longtitudeRange = 0.02;
                for (int i = 0; i < a.length(); i++) {
                    LatLng mylocation = new LatLng(a.getJSONArray(i).getDouble(1), a.getJSONArray(i).getDouble(0));
                    if (Math.abs(mylocation.latitude - myCurrentlocation.latitude) >= latitudeRange) {
                        Log.i("Coordinates", "latitude");
                        continue;
                    }
                    if (Math.abs(mylocation.longitude - myCurrentlocation.longitude) >= longtitudeRange) {
                        Log.i("Coordinates", "longitude");
                        continue;
                    }
                    //mMap.clear();

                    Geocoder gcd = new Geocoder(getApplicationContext(),
                            Locale.getDefault());
                    List<Address> addresses;
                    addresses = gcd.getFromLocation(mylocation.latitude,
                            mylocation.longitude, 1);

                    if (addresses.size() > 0) {
                        String s = addresses.get(0).getAddressLine(0) + "\n"
                                + addresses.get(0).getLocality() + "\n"
                                + addresses.get(0).getPostalCode() + "\n"
                                + addresses.get(0).getThoroughfare() +  "\n";


                        mMap.addMarker(new MarkerOptions().position(mylocation).title(addresses.get(0).getThoroughfare()).snippet(addresses.get(0).getPostalCode()));
                        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mylocation,10));
                        Log.i("Coordinates", mylocation.toString() + s);
                    }
                }
            }catch(JSONException e){
                    e.printStackTrace();
            } catch(IOException e){
                    e.printStackTrace();
            }

        }
    }

    //logout function
    private void logout(){
        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);



        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        String result;
        DownloadTask task =new DownloadTask();
        try {
            task.execute("https://api.data.gov.sg/v1/transport/taxi-availability?date_time=2018-09-09T09%3A09%3A09");
            //Log.i("info",result);
        } catch (Exception e) {
            e.printStackTrace(); }

        //for navigation menu

        myDrawer = (DrawerLayout) findViewById(R.id.myDrawer);
        myToggle = new ActionBarDrawerToggle(this, myDrawer, R.string.open, R.string.close);
        myDrawer.addDrawerListener(myToggle);
        myToggle.syncState();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);





    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(myToggle.onOptionsItemSelected(item)){
            return true;}



        return super.onOptionsItemSelected(item);
    }

    //jumping to next activity of menu items
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            //clicking "Settings" from menu bar, insert "settings" code to SettingsActivity
            case R.id.menuSettings:
                Intent settings = new Intent(MapsActivity.this, SettingsActivity.class);
                startActivity(settings);
                break;

            //clicking "Taxi Fare Calculator" from menu bar, insert "Taxi Fare Calculator" code to TaxiFareActivity
            case R.id.menuTaxiFare:
                Intent taxifare = new Intent(MapsActivity.this, TaxiFareActivity.class);
                startActivity(taxifare);
                break;

            //clicking "Favourites" from menu bar, insert "favourites" code to FavouriteActivity
            case R.id.menuFavourites:
                Intent favourites = new Intent(MapsActivity.this, FavouritesActivity.class);
                startActivity(favourites);
                break;

            //clicking "Feedback" from menu bar, insert "feedback" code to FeedbackActivity
            case R.id.menuFeedback:
                Intent feedback = new Intent(MapsActivity.this, FeedbackActivity.class);
                startActivity(feedback);
                break;

            //click "Logout" at menu bar to logout
            case R.id.menuLogout:
                logout();

        }

        return false;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                mMap.clear();
                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                //myCurrentlocation = new LatLng(Double.valueOf(1.3521),Double.valueOf(103.8198));
                myCurrentlocation = new LatLng(lastKnownLocation.getLatitude(),lastKnownLocation.getLongitude());
                //mMap.clear();
                mMap.addMarker(new MarkerOptions().position(myCurrentlocation).title("My location").snippet("and snippet")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myCurrentlocation,17));
                Log.i("Location",location.toString());
                //Toast.makeText(MapsActivity.this,location.toString(),Toast.LENGTH_SHORT).show();
                DownloadTask task =new DownloadTask();
                try {
                    task.execute("https://api.data.gov.sg/v1/transport/taxi-availability?date_time=2018-09-09T09%3A09%3A09");
                    //Log.i("info",result);
                } catch (Exception e) {
                    e.printStackTrace(); }
                //LatLng mylocation = new LatLng(location.getLatitude(),location.getLongitude());
                //mMap.clear();
                //mMap.addMarker(new MarkerOptions().position(mylocation).title("New location"));
                //mMap.moveCamera(CameraUpdateFactory.newLatLng(mylocation));

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
        }
        else{
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1200,10,locationListener);
//            long time= System.currentTimeMillis();
//            long duration = 5000;
//            while(System.currentTimeMillis()< time + duration){
//                continue;
//            }
            lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            //myCurrentlocation = new LatLng(Double.valueOf(1.3521),Double.valueOf(103.8198));
            myCurrentlocation = new LatLng(lastKnownLocation.getLatitude(),lastKnownLocation.getLongitude());
            //mMap.clear();
            mMap.addMarker(new MarkerOptions().position(myCurrentlocation).title("My location").snippet("and snippet")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myCurrentlocation,17));

        }

    }
}