package com.example.ricardo.testingmaps;

import com.example.ricardo.testingmaps.ViewComponents.locationPopUp;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.IndoorBuilding;
import com.google.android.gms.maps.model.IndoorLevel;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import android.*;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.PopupMenu;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;

public class MapsActivity extends AppCompatActivity  implements OnMapReadyCallback, LocationListener {

    public static final String TAG = "GoogleMapsLog";


    private MapView mMapView;
    private GoogleMap map;

    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";

    int TAG_CODE_PERMISSION_LOCATION;

    private Integer mMockGpsProviderIndex = 0;

    Intent mServiceIntent;
    private BluetoothAdapter BTAdapter = BluetoothAdapter.getDefaultAdapter();
    private final static int REQUEST_ENABLE_BT = 1;

    private static final int DELAY_TIME_IN_MILLI = 500;
    Handler mHandler;

    StringBuilder LastFloor = new StringBuilder();
    locationPopUp popup = null;

    boolean running=true;
    boolean waiting =false;
    boolean IsMapReady=false;

    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        context = this;

        mHandler = new Handler();
        // *** IMPORTANT ***
        // MapView requires that the Bundle you pass contain _ONLY_ MapView SDK
        // objects or sub-Bundles.
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }
        mMapView = (MapView) findViewById(R.id.map);
        mMapView.onCreate(mapViewBundle);

        mMapView.getMapAsync(this);

        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "need permissions....");
            ActivityCompat.requestPermissions(this, new String[] {
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION,},
                    TAG_CODE_PERMISSION_LOCATION);
        }

        if (BTAdapter == null || !BTAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        Button popupbutton = (Button) findViewById(R.id.button);
        BatteryManager mBatteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        popup  = new locationPopUp( new PopupMenu(getApplicationContext(), popupbutton) , mBatteryManager);
        popupbutton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                popup.show();
            }
        });

        final Switch aswitch = (Switch) findViewById(R.id.switch1);
        aswitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if (isChecked) {
                    Log.i(TAG, "switch on");


                    running = true;
                    Thread thread = new Thread() {
                        @Override
                        public void run() {
                            try {
                                while (running) {
                                    while (waiting) {
                                        Log.i(TAG, "Waiting to finish download");
                                        sleep(2000);
                                    }

                                    waiting = true;
                                    Log.i(TAG, "Searching");

                                    if(IsMapReady){
                                        context.startService(mServiceIntent);
                                    }else{
                                        Log.i(TAG, "Waiting for GoogleMaps to load");
                                    }

                                    sleep(5000);
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    };
                    thread.start();
                } else {
                    Log.i(TAG, "switch off");
                    running = false;
                }
            }
        });
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        String mocLocationProvider = LocationManager.GPS_PROVIDER;
        locationManager.addTestProvider(mocLocationProvider, true, false,
                false, false, true, false, false, 0, 5);
        locationManager.setTestProviderEnabled(mocLocationProvider, true);
        locationManager.requestLocationUpdates(mocLocationProvider, 0, 0, this);

        mServiceIntent = new Intent(this, MockProviderService.class);

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
    public void onMapReady(GoogleMap map) {
        this.map = map;

        map.setMinZoomPreference(5.0f);
        map.setMaxZoomPreference(25.0f);

        Log.i(TAG, "Map is Ready");
        map.moveCamera(CameraUpdateFactory.newLatLng( new LatLng(0, 0)));
        IsMapReady = true;
        //this.startService(mServiceIntent);

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }

        mMapView.onSaveInstanceState(mapViewBundle);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //mMapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //mMapView.onStop();
    }

    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mMapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }


    //LocationLisneter functions
    @Override
    public void onLocationChanged(Location location) {
        if( location.getExtras().getString("fail").equals("yes")){
            Log.i(TAG, "provider failed");
            waiting = false;
            return;
        }

        // show the received location in the view
       Log.i(TAG, "longitude:" + location.getLongitude()
                + "\nlatitude:" + location.getLatitude()
                + "\naltitude:" + location.getAltitude()
                + "\nroom:" + location.getExtras().getString("floor")
        );

        LatLng mlatlng = new LatLng(location.getLatitude(), location.getLongitude());
        map.addMarker(new MarkerOptions().position(mlatlng).title( location.getExtras().getString("room") ));
        //map.moveCamera(CameraUpdateFactory.newLatLngZoom(IST,(float)(location.getAltitude())));

        LastFloor.replace(0,location.getExtras().getString("floor").length(), location.getExtras().getString("floor") );

        popup.update(
                location.getExtras().getString("room"),
                location.getExtras().getString("floor"),
                location.getExtras().getString("building"),
                location.getExtras().getString("street"),
                location.getExtras().getString("number"),
                location.getExtras().getString("zipcode"),
                location.getExtras().getString("city"),
                location.getExtras().getString("country")
        );
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(mlatlng)      // Sets the center of the map to Mountain View
                .zoom( (float)(location.getAltitude()) )// Sets the zoom
                .build();                   // Creates a CameraPosition from the builder
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition),3000, new GoogleMap.CancelableCallback() {
            @Override
            public void onFinish() {
                //Constant for our time. I would put this below your class declaration
                Log.i(TAG, "Finished Loading!");
                //Here is the handler for waiting
                mHandler.postDelayed(new Runnable() {

                    //This function runs after the time allotted below
                    @Override
                    public void run() {

                        //Call your screenshot function here
                        FocusBuildingFloor(LastFloor.toString());
                    }
                },DELAY_TIME_IN_MILLI);
            }

            @Override
            public void onCancel() {

            }
        });

            waiting = false;
        //FocusBuildingFloor(LastFloor);

    }

    private void FocusBuildingFloor(String floor){
        IndoorBuilding building = map.getFocusedBuilding();
        if (building != null) {
            List<IndoorLevel> levels = building.getLevels();
            if (!levels.isEmpty()) {
                for( IndoorLevel level: levels) {
                    if (level.getShortName().equals(floor) ) {
                        level.activate();
                        Log.i(TAG, "Activiating level " + level.getName());
                        return;
                    }
                }
            } else {
                Log.i(TAG,"No levels in building");
            }
        } else {
            Log.i(TAG,"No visible building");
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub
    }


    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub
    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
    }


}
