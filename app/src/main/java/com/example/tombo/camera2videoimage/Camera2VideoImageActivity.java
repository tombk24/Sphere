package com.example.tombo.camera2videoimage;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;


import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class Camera2VideoImageActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, SensorEventListener {

    ///Camera Variables
    private String mCameraId;
    private HandlerThread mBackgroundHandlerThread;
    private Handler mBackgroundHandler;
    private static SparseIntArray ORIENTATIONS = new SparseIntArray();
    private android.util.Size mPreviewSize;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }
    ///Location Variables
    private final String LOG_TAG = "TESTAPP";
    private static final int REQUEST_CODE_LOCATION = 2;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationClient;
    TextView altitude;
    TextView latitude;
    TextView longitude;
    TextView accuracy;
    TextView targetLocationtxt;
    RelativeLayout spheres;

    //Variables for storing the 2nd location
    Location targetLocation;
    Button setButton;

    //Variables for the compass
    private SensorManager mSensorManager;
    TextView bearing;





    private static final int REQUEST_CAMERA_PERMISSION_RESULT = 0;
    private TextureView mTextureView; //The view on which the camera preview is put on
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            setupCamera(width, height);
            connectCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    private CameraDevice mCameraDevice;
    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }
    };

    //Altitude
    private Sensor altitudeSensor;

    private static class CompareSizeByArea implements Comparator<android.util.Size> {

        @Override
        public int compare(android.util.Size lhs, android.util.Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() /
                    (long)rhs.getWidth() * rhs.getHeight());
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2_video_image);
        mTextureView = (TextureView) findViewById(R.id.textureView);
        CheckLocationPermissions();

        altitude = (TextView)findViewById(R.id.txtAltitude);
        latitude = (TextView)findViewById(R.id.txtLatitude);
        longitude = (TextView)findViewById(R.id.txtLongitude);
        bearing = (TextView)findViewById(R.id.txtBearing);
        accuracy = (TextView)findViewById(R.id.txtAccuracy);
        setButton = (Button) findViewById(R.id.setLocation);
        targetLocationtxt = (TextView)findViewById(R.id.txtTargetLocation);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        setButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (targetLocation == null){

                } else {
                    targetLocation = null;
                }
            }
        });

        //initialise the android sensor capabilities
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        altitudeSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);



//        Sphere test = new Sphere(getApplicationContext());
//        spheres = (RelativeLayout)findViewById(R.id.Spheres);
//        spheres.addView(test.returnLayout());
    }



    @Override
    protected void onResume(){
        super.onResume();

        startBackgroundThread();

        if(mTextureView.isAvailable()){
            setupCamera(mTextureView.getWidth(), mTextureView.getHeight());
            connectCamera();

        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);

        }


        //Compass
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME, SensorManager.SENSOR_DELAY_NORMAL);

        //Altitude works
        //mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE), SensorManager.SENSOR_DELAY_NORMAL);
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION_RESULT){
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(getApplicationContext(),
                        "Application won't run without camera services", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onPause(){
        closeCamera();

        //Compass && altitude
        mSensorManager.unregisterListener(this);

        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onSensorChanged(SensorEvent event){
        float degree = Math.round(event.values[0]);
     //   float presure = event.values[0];
     //   float altitudeF = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, presure);
        bearing.setText("Bearing: " + Float.toString(degree));
     //   altitude.setText("Altitude: " + Float.toString(altitudeF));
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus){
        super.onWindowFocusChanged(hasFocus);

        View decorView = getWindow().getDecorView();

        if (hasFocus){
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    private void setupCamera(int width, int height){
        CameraManager cameraManager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try {
            for(String cameraId: cameraManager.getCameraIdList()){
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT){
                    continue;
                }
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
                int totalRotation = sensorToDeviceRotation(cameraCharacteristics, deviceOrientation);
                boolean swapRotation = totalRotation == 90 || totalRotation == 270;
                int rotatedWidth = width;
                int rotatedHeight = height;
                if (swapRotation){
                    rotatedWidth = height;
                    rotatedHeight = width;
                }
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);
                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void connectCamera(){
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                    cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
                } else {
                    if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
                        Toast.makeText(this, "Video app required access to camera", Toast.LENGTH_SHORT).show();
                    }
                    requestPermissions(new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_RESULT);
                }
            } else {
                cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startPreview(){
        SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);

        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    try {
                        cameraCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(),
                                null, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(getApplicationContext(),
                            "Unable to setup camera preview", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera(){
        if (mCameraDevice != null){
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private void startBackgroundThread(){
        mBackgroundHandlerThread = new HandlerThread("Camera2VideoImage");
        mBackgroundHandlerThread.start();
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());
    }

    private void stopBackgroundThread(){
        mBackgroundHandlerThread.quitSafely();
        try {
            mBackgroundHandlerThread.join();
            mBackgroundHandlerThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static int sensorToDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientation){
        int sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (sensorOrientation + deviceOrientation + 360) %360;
    }

    private static android.util.Size chooseOptimalSize(android.util.Size[] choices, int width, int height){
        List<android.util.Size> bigEnough = new ArrayList<android.util.Size>();

        //Finds which screens are big enough to represent the preview
        for(android.util.Size option : choices){
            if (option.getHeight() == option.getWidth() * height / width &&
                    option.getWidth() >= width && option.getHeight() >= height){
                bigEnough.add(option);
            }
        }

        if (bigEnough.size() > 0){
            return Collections.min(bigEnough, new CompareSizeByArea());
        } else {
            return choices[0];
        }
    }

    public void CheckLocationPermissions(){

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_CODE_LOCATION);
        }
    }

    @Override
    protected void onStart(){
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop(){
        mGoogleApiClient.disconnect();
        super.onStop();
    }


    @Override
    public void onConnected(Bundle bundle){
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(1000);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest,(LocationListener) this);
        }


    }

    @Override
    public void onLocationChanged(Location location){
        Log.i(LOG_TAG, location.toString());
        changeTags(location);
        if (targetLocation == null){
            targetLocation = location;
        }

    }

    @Override
    public void onConnectionSuspended(int i){
        Log.i(LOG_TAG, "GoogleAPIClient connection has been suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult){
        Log.i(LOG_TAG,"GoogleAPIClient Connect has failed");
    }

    private void changeTags(Location location){
        String altitudeText = "Altitude: " + Double.toString(location.getAltitude());
        String longitudeText = "Longitude: " + Double.toString(location.getLongitude());
        String latitudeText = "Latitude: " + Double.toString(location.getLatitude());
        String bearingText = "Bearing: " + Float.toString(location.getBearing());
        String accuracyText = "Accuracy: " + Float.toString(location.getAccuracy());

        //altitude.setText(altitudeText);
        longitude.setText(longitudeText);
        latitude.setText(latitudeText);
        //bearing.setText(bearingText);
        accuracy.setText(accuracyText);


    }

    private void createSphere(){

    }


    //Compass


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not in use
    }
}
