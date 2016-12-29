package com.example.ricardo.testingmaps.TCP;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;

import com.example.ricardo.testingmaps.ViewComponents.locationPopUp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Created by ricardomartins on 25/08/16.
 */
public class TCPClient {

    private static final String TAG = "TCPClient";
    private final Handler mHandler;
    private String ipNumber, incomingMessage, command;
    private String[] info, location;
    private int portnb;
    BufferedReader in;
    PrintWriter out;
    private MessageCallback listener= null;
    private boolean mRun = false;
    private byte[] data;

    public Bitmap map;
    public String map_name;
    public String new_map_name;

    public double xcoordinate;
    public double ycoordinate;
    public String RoomName;



    public StringBuilder room = new StringBuilder(" ");
    public StringBuilder floor = new StringBuilder(" ");
    public StringBuilder building = new StringBuilder(" ");

    public StringBuilder street = new StringBuilder(" ");
    public StringBuilder number = new StringBuilder(" ");
    public StringBuilder zipcode = new StringBuilder(" ");
    public StringBuilder city = new StringBuilder(" ");
    public StringBuilder country = new StringBuilder(" ");





    /**
     * TCPClient class constructor, which is created in AsyncTasks after the button click.
     * @param mHandler Handler passed as an argument for updating the UI with sent messages
     * @param command  Command passed as an argument, e.g. "shutdown -r" for restarting computer
     * @param ipNumber String retrieved from IpGetter class that is looking for ip number.
     * @param listener Callback interface object
     */
    public TCPClient(Handler mHandler, String command, String ipNumber, MessageCallback listener , int portnb, String map_name) {
        this.listener         = listener;
        this.ipNumber         = ipNumber;
        this.command          = command ;
        this.mHandler         = mHandler;
        this.portnb           = portnb;
        this.map_name         = map_name;
    }

    /**
     * Public method for sending the message via OutputStream object.
     * @param message Message passed as an argument and sent via OutputStream object.
     */
    public void sendMessage(String message){
        if (out != null && !out.checkError()) {
            out.println(message);
            out.flush();
            Log.w(TAG, "Sent Message: " + message);
        }
    }

    /**
     * Public method for stopping the TCPClient object ( and finalizing it after that ) from AsyncTask
     */
    public void stopClient(){
        Log.d(TAG, "Client stopped!");
        mRun = false;
    }

    public void run() {

        mRun = true;

        try {
            // Creating InetAddress object from ipNumber passed via constructor from IpGetter class.
            InetAddress serverAddress = InetAddress.getByName(ipNumber);

            Log.i(TAG, "Connecting..."+ serverAddress+" "+ portnb);

            Socket socket = new Socket(serverAddress, portnb);

            try {

                // Create PrintWriter object for sending messages to server.
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                //Create BufferedReader object for receiving messages from server.
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                Log.i(TAG, "In/Out created");

                //Sending message with command specified by AsyncTask
                this.sendMessage(command);

                //Listen for the incoming messages while mRun = true
                Log.i(TAG, "VOU LET!!!! :D");
                incomingMessage = in.readLine();
                info = incomingMessage.split("\\|");
                Log.i(TAG, "LI!!!! ->>>>> " + incomingMessage);

                Log.i(TAG, "0-> "+ info[0]);
                Log.i(TAG, "1-> "+ info[1]);
                Log.i(TAG, "2-> "+ info[2]);
                Log.i(TAG, "3-> "+ info[3]);

                Log.i(TAG, "old:"+map_name + "/new:"+info[0]+"//");

                incomingMessage = in.readLine();
                location = incomingMessage.split("\\|");

                Log.i(TAG, "LI!!!! ->>>>> " + incomingMessage);

                Log.i(TAG, "4-> "+ location[0]);
                Log.i(TAG, "5-> "+ location[1]);
                Log.i(TAG, "6-> "+ location[2]);
                Log.i(TAG, "7-> "+ location[3]);
                Log.i(TAG, "8-> "+ location[4]);
                Log.i(TAG, "9-> "+ location[5]);
                Log.i(TAG, "10-> "+ location[6]);
                Log.i(TAG, "11-> "+ location[7]);


                room.replace(0, location[5].length(), location[5]);
                floor.replace(0, location[6].length(), location[6]);
                building.replace(0, location[7].length(), location[7]);

                street.replace(0, location[0].length(), location[0]);
                number.replace(0, location[1].length(), location[1]);
                zipcode.replace(0, location[2].length(), location[2]);
                city.replace(0, location[3].length(), location[3]);
                country.replace(0, location[4].length(), location[4]);


                xcoordinate = Double.parseDouble(info[2]);
                ycoordinate = Double.parseDouble(info[3]);




                if (incomingMessage == null && listener != null) {
                    Log.i(TAG,"NULL Message");
                    listener.callbackMessageReceiver("error");
                } else if ( incomingMessage == "NOK"){
                    Log.i(TAG, "Failure to Locate");
                    listener.callbackMessageReceiver("error");

                }

                /*else if( !map_name.equals(info[0]) ){
                    int index;
                    if( (index=fileStorage.checkDeviceOnCache(info[0]))==-1){

                        Log.i(TAG, "Requesting file ->" + info[0]);
                        this.sendMessage("-f " + info[0]);

                        DataInputStream in_data = new DataInputStream(socket.getInputStream());


                        String len;

                        len = in_data.readLine();

                        Log.i(TAG, "File size = " + len);

                        data = new byte[Integer.parseInt(len)];
                        in_data.readFully(data);

                        map = BitmapFactory.decodeByteArray(data, 0,
                                data.length);

                        new_map_name = info[0];
                        xcoordinate = Double.parseDouble(info[2]);
                        ycoordinate = Double.parseDouble(info[3]);

                        fileStorage.addDeviceCache(info[0],data);

                        listener.callbackMessageReceiver("map");
                    }else {
                        Log.i(TAG, "got file on storage");
                        data = fileStorage.getFIle(index);
                        map = BitmapFactory.decodeByteArray(data, 0,
                                data.length);
                        new_map_name = fileStorage.getFileName(index);
                        xcoordinate = Double.parseDouble(info[2]);
                        ycoordinate = Double.parseDouble(info[3]);
                        listener.callbackMessageReceiver("map");
                    }
                }else{
                    Log.i(TAG, "Updating coords!!");
                    xcoordinate = Double.parseDouble(info[2]);
                    ycoordinate = Double.parseDouble(info[3]);
                    new_map_name = map_name;
                    listener.callbackMessageReceiver("coords");
                }*/
                RoomName = info[1];
                this.sendMessage("ok");
                listener.callbackMessageReceiver("No change");

                Log.i(TAG, "old:"+map_name + "/new:"+info[0]+"//");
                Log.i(TAG, "Received Message: " +incomingMessage);

            } catch (Exception e) {

                Log.i(TAG, "Error", e);

            } finally {

                out.flush();
                out.close();
                in.close();
                socket.close();
                Log.i(TAG, "Socket Closed");
            }

        } catch (Exception e) {

            Log.i(TAG, "Error", e);
        }

    }

    /**
     * Callback Interface for sending received messages to 'onPublishProgress' method in AsyncTask.
     *
     */
    public interface MessageCallback {
        /**
         * Method overriden in AsyncTask 'doInBackground' method while creating the TCPClient object.
         * @param message Received message from server app.
         */
        public void callbackMessageReceiver(String message);
    }

}
