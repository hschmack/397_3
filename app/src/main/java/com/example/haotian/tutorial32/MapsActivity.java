package com.example.haotian.tutorial32;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
//import android.location.LocationListener;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.BufferedWriter;
import java.io.File;
import android.net.Uri;
import android.os.Environment;
import android.widget.ImageView;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener {
    public static final String TAG = "MapsActivity";
    public static final int THUMBNAIL = 1;
    public static final int REQUEST_IMAGE_CAPTURE = 1;
    public static boolean mRequestingLocationUpdates;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Button picButton; //takes user to camera
    private String mCurrentPhotoPath; //path of the saved image
    private GoogleApiClient mGoogleApiClient;
    private Location mCurrentLocation;
    protected LocationRequest mLocationRequest;
    private ImageView mImageView;

    private File file;
    private File tmpPath;
    public BufferedWriter bufferedWriter;
    public StringBuilder tempLocationData;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
        mRequestingLocationUpdates = true;

        picButton = (Button) findViewById(R.id.photobutton);

        picButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });
        //set the google api client object to an initialized version of the object
        buildGoogleApiClient();

        picButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

        File dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        File myFile = new File(dcim, "photodata.csv");

        tempLocationData = new StringBuilder();

        if (!myFile.exists()){
            Log.d(TAG, "photodata.csv does not exist, creating and adding headings");
            //then create a new one
            file = new File(dcim, "photodata.csv");

            try {
                bufferedWriter = new BufferedWriter(new FileWriter(myFile));
                String[] headings = {"TimeStamp", "Latitude", "Longitude"};
                //initialize the first row of the csv, this row only contains column headings

                for (String heading : headings) {
                    tempLocationData.append(heading);
                    tempLocationData.append(',');
                }
                tempLocationData.append("\n");
                Log.d(TAG, "WRITING HEADINGS");
                bufferedWriter.write(tempLocationData.toString());
                tempLocationData.setLength(0);
                bufferedWriter.flush();
            }
            catch (java.io.IOException e){
                Log.d("IO", "CANT CREATE BUFFERED OR FILE READER");
            }
        } else {
            Log.d(TAG, "photodata.csv existed");
            try{
                bufferedWriter = new BufferedWriter(new FileWriter(myFile, true));
            } catch (IOException ex) {
                Log.d("IO", "CANT CREATE BUFFERED OR FILE READER");
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();

        super.onStop();
    }

    protected void stopLocationUpdates() {
        mRequestingLocationUpdates = false;
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }
    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.addMarker(new MarkerOptions().position(new LatLng(20, 20)).title("EECS397/600"));
    }

    /**
     * Taken from http://developer.android.com/training/camera/photobasics.html
     */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex){
                Log.d("ERROR", "Error creating the image file");
            }
            if (photoFile != null){
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK){
            //Write to CSV
            writeLocationData();
            writeToFile();

            Bundle extras = data.getExtras();
            if (extras == null){ Log.d(TAG, "YEAH NULL"); return; }
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            mImageView.setImageBitmap(imageBitmap);
        }
    }

    /**
     * Create the image file that is created on dispatchTakePictureIntent
     * @throws IOException
     */
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "MAPIMG_" + timeStamp + "_";
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "app32_imgs");
        storageDir.mkdirs(); //add new subfolder in the DCIM directory

        File image = File.createTempFile(imageFileName, ".jpg", storageDir);

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }

    public void writeLocationData(){
        Long tsLong = System.currentTimeMillis()/1000;
        String ts = tsLong.toString();
        tempLocationData.append(ts+",");
        tempLocationData.append(mCurrentLocation.getLatitude()+",");
        tempLocationData.append(mCurrentLocation.getLongitude()+",");
        tempLocationData.append("\n");
    }

    public void writeToFile(){
        try{
            bufferedWriter.write(tempLocationData.toString());
            bufferedWriter.flush();
        }catch (java.io.IOException e){
            Log.e("IO", "ERROR WRITING CSV");
        }
    }
    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    protected void createLocationRequest() {
        Log.i(TAG, "Creating Location Request");
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }
    /**
     * Request Location Updates
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to GoogleApiClient");

        if (mCurrentLocation == null) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }

        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    /**
     * Requests location updates from the FusedLocationApi.
     */
    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }
}
