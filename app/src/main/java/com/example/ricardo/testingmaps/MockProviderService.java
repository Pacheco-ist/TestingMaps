package com.example.ricardo.testingmaps;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import com.example.ricardo.testingmaps.BLE.BluetoothLeService;
import com.example.ricardo.testingmaps.BLE.LeDeviceListAdapter;
import com.example.ricardo.testingmaps.BLE.SampleGattAttributes;
import com.example.ricardo.testingmaps.BLE.serverList;
import com.example.ricardo.testingmaps.TCP.TCPClient;
import com.example.ricardo.testingmaps.ViewComponents.locationPopUp;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Ricardo on 23/10/2016.
 */
public class MockProviderService  extends IntentService {

  public static final String TAG = "GoogleMapsLog";

    Handler mHandler = new Handler();

    //BLE scanning variable
    private static final long SCAN_PERIOD = 2000;
    private boolean mScanning;
    private BluetoothAdapter BTAdapter = BluetoothAdapter.getDefaultAdapter();

    //GATT Variables
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    public final static UUID UUID_SERVER_CARAC =
            UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG);
    public final static UUID UUID_DEVICE_SERVICE =
            UUID.fromString(SampleGattAttributes.DEVICE_INFO_SERVICE);

    BluetoothLeService BLEservice;

    StringBuilder servername = new StringBuilder();
    private Gatt_discoverer mGatt_discoverer;
    boolean mConnected;

    // TCP variables
    private Socket socket;
    private int SERVERPORT;
    private StringBuilder SERVER_IP = new StringBuilder("");
    private Context ActContext;
    private String devices_found;
    private String message;

    //MAPview variable
    private Bitmap map;
    private double xcoord;
    private double ycoord;
    private volatile boolean running = true;
    private volatile boolean updating=false;
    StringBuilder MapName = new StringBuilder("");

    //custom classes
    private LeDeviceListAdapter mLeDeviceListAdapter;
    public locationPopUp popUp;

    //Info Variables
    private StringBuilder room = new StringBuilder(" ");
    private StringBuilder floor = new StringBuilder(" ");
    private StringBuilder building = new StringBuilder(" ");

    private StringBuilder street = new StringBuilder(" ");
    private StringBuilder number = new StringBuilder(" ");
    private StringBuilder zipcode = new StringBuilder(" ");
    private StringBuilder city = new StringBuilder(" ");
    private StringBuilder country = new StringBuilder(" ");

    private boolean starting=true;
    private boolean HasServer = false;


    MockProviderService(){
        super("MockProviderService");
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        Log.i(TAG, "Entered intentservice");
        if(!starting){
            mLeDeviceListAdapter.clear();
            mGatt_discoverer.resetDiscoverer();
        }else{
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            }


            mLeDeviceListAdapter = new LeDeviceListAdapter();

            mGatt_discoverer = new Gatt_discoverer();
            //BTAdapter.startLeScan(mLeScanCallback);
            starting = false;
        }
        HasServer = false;
        scanLeDevice(true);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG,"TERMINATE");

                    mScanning = false;
                    BTAdapter.stopLeScan(mLeScanCallback);


                    Log.i(TAG, "Calling GATT");
                    mGatt_discoverer.getDeviceName();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            BTAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            BTAdapter.stopLeScan(mLeScanCallback);
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    Log.i(TAG, "adding " + device.getAddress());
                    //testing tool - only my devices work
                    if( (device.getAddress().equals("B0:B4:48:BF:E0:80")) || (device.getAddress().equals("B0:B4:48:BC:45:81")) || (device.getAddress().equals("B0:B4:48:BE:8A:81") )){
                        mLeDeviceListAdapter.addDevice(device, rssi);
                        Log.i(TAG, "Added device");
                    }
                }
            };



    void SendLocation(){
        Log.i(TAG, "Send Location: " + HasServer);
        Location location = new Location(LocationManager.GPS_PROVIDER);

        if(!HasServer){
            location.setLatitude(0);
            location.setLongitude(0);
            location.setAltitude(0);
            location.setAccuracy(16F);
            location.setTime(System.currentTimeMillis());
            location.setBearing(0F);
            Bundle LocInfo = new Bundle();
            LocInfo.putString("fail", "yes" );
            location.setExtras(LocInfo);
        }else {
            // retrieve data from the current line of text
            Double latitude = xcoord;
            Double longitude = ycoord;
            Double altitude = 20.;

            location.setLatitude(latitude);
            location.setLongitude(longitude);
            location.setAltitude(altitude);
            location.setAccuracy(16F);
            location.setTime(System.currentTimeMillis());
            location.setBearing(0F);

            Bundle LocInfo = new Bundle();
            LocInfo.putString("room", room.toString());
            LocInfo.putString("floor", floor.toString());
            LocInfo.putString("building", building.toString());
            LocInfo.putString("street", street.toString());
            LocInfo.putString("number", number.toString());
            LocInfo.putString("zipcode", zipcode.toString());
            LocInfo.putString("city", city.toString());
            LocInfo.putString("country", country.toString());
            LocInfo.putString("fail", "no" );
            location.setExtras(LocInfo);
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            // Elapsed time can also be set using
            // mockLocation.setElapsedRealtimeNanos(System.nanoTime());
            // Elapsed time can be disregarded using
            // mockLocation.makeComplete();
        }

        // show debug message in log
        Log.i(TAG, location.toString());

        // provide the new location
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, location);
    }






    /*GATT FUNTIONS*/
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        Log.i(TAG, "Procurar");

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            if( UUID_DEVICE_SERVICE.equals(gattService.getUuid())) {
                Log.i(TAG, "ENCONTREI SERVIÇO");

                List<BluetoothGattCharacteristic> gattCharacteristics =
                        gattService.getCharacteristics();
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {

                    if( UUID_SERVER_CARAC.equals(gattCharacteristic.getUuid())){
                        Log.i(TAG, "ENCONTREI SERVER, PEDIR PARA LER");
                        readCharacteristic(gattCharacteristic);
                    }
                }
            }
        }
    }



    private class Gatt_discoverer{

        private BluetoothDevice device;
        private int current_device;

        private serverList mserverList = new serverList();

        private int max_rssi;

        private ArrayList<Device> cacheDevices = new ArrayList<Device>();

        private static final int cacheSize =5;

        Gatt_discoverer(){
            resetDiscoverer();
        }

        public void resetDiscoverer(){
            current_device=-1;
            max_rssi=0;
            mserverList.reset();
        }
        public void getDeviceName() {
            current_device++;
            Log.i(TAG, "Device "+ current_device + " of " + mLeDeviceListAdapter.getCount());

            if ( current_device >= mLeDeviceListAdapter.getCount()){
                if (current_device==0){
                    Log.i("ERROR", "No Devices found...");
                    SendLocation();
                    return;
                }else {
                    if( HasServer) {
                        mGatt_discoverer.launchTcp();
                        return;
                    }else{
                        SendLocation();
                        return;
                    }
                }
            }
            device = mLeDeviceListAdapter.getDevice(current_device);


            int index;
            if( (index = checkDeviceOnCache(device)) != -1){
                Log.i(TAG,"Found on cache pos "+index);
                addNS(cacheDevices.get(index).ip, cacheDevices.get(index).port);
                getDeviceName();
            }else{
                connect(device.getAddress());
            }

        }


        public void addDeviceCache( String ip, int port){
            Device argdevice = new Device(device,ip,port);
            Log.i(TAG, "adding "+ device.getAddress() + " ->>>> " + cacheDevices.contains(argdevice));
            if ( cacheDevices.contains(argdevice)){
                int index = cacheDevices.indexOf(argdevice);
                cacheDevices.remove(argdevice);
                cacheDevices.add(0,argdevice);
                Log.i(TAG, "Device already in cache: old-> "+ index +" , new -> "+ cacheDevices.indexOf(argdevice));
            }else{
                if( cacheDevices.size() >= cacheSize){
                    cacheDevices.remove(cacheSize-1);
                    Log.i(TAG, "Too big, removed "+ 2);
                }
                cacheDevices.add(0,argdevice);
                Log.i(TAG, "add device, size = "+cacheDevices.size());
            }
        }

        public int checkDeviceOnCache(BluetoothDevice device){
            Device argdevice = new Device(device,"",0);
            if(cacheDevices.contains(argdevice)){
                return cacheDevices.indexOf(argdevice);
            }
            return -1;
        }

        public void addNS(String ip, int port){
            mserverList.addServer(ip,port, mLeDeviceListAdapter.getRSSI(current_device));
        }

        public void launchTcp(){

            Log.i(TAG, "Launching TCP");

            serverList.server server = mserverList.getProminentServer();

            SERVER_IP.replace(0, server.getIpaddr().length(), server.getIpaddr());
            SERVERPORT = server.getPort();

            Log.i(TAG, "Connectiong tcp to ->" + SERVER_IP + ":" + SERVERPORT);

            devices_found ="-l ";
            for (int i = 0; i < mLeDeviceListAdapter.getCount(); i++) {
                device = mLeDeviceListAdapter.getDevice(i);
                devices_found += device.getAddress() + "\\" + mLeDeviceListAdapter.getRSSI(i);
                if( (i+1)!=mLeDeviceListAdapter.getCount()){
                    devices_found+="!";
                }
            }
            new AsyncTcp(mHandler).execute();
        }

    }


    class Device{
        BluetoothDevice device;
        String ip;
        int port;


        Device( BluetoothDevice device,String ip, int port){
            this.device=device;
            this.ip=ip;
            this.port=port;
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public String toString() {
            return device.getAddress();
        }

        @Override
        public boolean equals(Object obj) {
            if( obj == this) return true;
            if(obj==null) return false;

            if(this.getClass() != obj.getClass()) return false;

            Device other = (Device) obj;

            //Log.i(TAG, "compare My "+ this.device.getAddress() + " with " + other.device.getAddress());

            if(device.getAddress().equals(other.device.getAddress())){
                return true;
            }
            return false;
        }
    }


    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                mGatt_discoverer.getDeviceName();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                displayGattServices(getSupportedGattServices());
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            String info;
            int port;
            byte[] data = characteristic.getValue();
            String name = new String(data);
            Log.i(TAG, "DATA AQUI:" + name);
            if(name.equals("INESC")){
                Log.i(TAG, "INESC device -> 146.193.41.154:10000");
                info = "146.193.41.154";
                port = 10000;
                //mGatt_discoverer.addNS("146.193.41.154", 10000);
            }else if(name.equals("IST")){
                Log.i(TAG, "IST device -> 146.193.41.154:10001");
                info = "146.193.41.154";
                port = 10001;
                //mGatt_discoverer.addNS("146.193.41.154", 10001);
            }else{
                info= "WUT";
                port = 1234;
                Log.i(TAG, "device ??? ->" + name);
            }
            //String[] address = name.split(".");
            //mGatt_discoverer.addNS(address[0], Integer.parseInt(address[1]));
            //Log.i(TAG, name);
            disconnect();

            // FOR TESTING NO CACHE
            //mGatt_discoverer.addDeviceCache(info,port);
            mGatt_discoverer.addNS(info,port);
            HasServer = true;

            Log.i(TAG, "calling next device");
            mGatt_discoverer.getDeviceName();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
        }
    };

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
        close();
    }

    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        Log.i(TAG, "Closed this GATT");
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public boolean connect(final String address) {
        Log.i(TAG, "CONNECT GATT to "+ address);
        if (mBluetoothAdapter == null || address == null) {
            Log.i(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.i(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.i(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.i(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }




    public class AsyncTcp extends AsyncTask<String, String, TCPClient> {

        private TCPClient tcpClient;
        private Handler mHandler                         ;
        private static final String TAG = "AsyncTcp";

        /**
         * ShutdownAsyncTask constructor with handler passed as argument. The UI is updated via handler.
         * In doInBackground(...) method, the handler is passed to TCPClient object.
         * @param mHandler Handler object that is retrieved from MainActivity class and passed to TCPClient
         *                 class for sending messages and updating UI.
         */
        public AsyncTcp(Handler mHandler){
            this.mHandler = mHandler;
        }

        /**
         * Overriden method from AsyncTask class. There the TCPClient object is created.
         * @param params From MainActivity class empty string is passed.
         * @return TCPClient object for closing it in onPostExecute method.
         */
        @Override
        protected TCPClient doInBackground(String... params) {
            Log.d(TAG, "In do in background");

            try{
                tcpClient = new TCPClient(mHandler,
                        devices_found,
                        SERVER_IP.toString(),
                        new TCPClient.MessageCallback() {
                            @Override
                            public void callbackMessageReceiver(String message) {
                                publishProgress(message);
                            }
                        }, SERVERPORT,
                        MapName.toString());

            }catch (NullPointerException e){
                Log.d(TAG, "Caught null pointer exception");
                e.printStackTrace();
            }
            tcpClient.run();
            return null;
        }

        /**
         * Overriden method from AsyncTask class. Here we're checking if server answered properly.
         * @param values If "restart" message came, the client is stopped and computer should be restarted.
         *               Otherwise "wrong" message is sent and 'Error' message is shown in UI.
         */
        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            Log.w(TAG, "In progress update, values: " + values.toString());
            if(values[0].equals("error")){
                Log.i(TAG, "Error: Unable to get location from server!!");
            }else {
                xcoord = tcpClient.xcoordinate;
                ycoord = tcpClient.ycoordinate;
                room = tcpClient.room;
                floor= tcpClient.floor;
                building = tcpClient.building;

                street = tcpClient.street;
                number = tcpClient.number;
                zipcode = tcpClient.zipcode;
                city = tcpClient.city;
                country = tcpClient.country;

                Log.i(TAG, "Sucess: Sending Location!!");
                SendLocation();
            }
        }

        @Override
        protected void onPostExecute(TCPClient result){
            super.onPostExecute(result);
            Log.d(TAG, "In on post execute");

        }
    }
}
