package fau.amoracen2017.archangel;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.muddzdev.styleabletoastlibrary.StyleableToast;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * A Fragment that displays a Google map with a marker (pin) to indicate a particular location.
 *
 * @author Alejandro Moracen
 */
public class MapFragment extends Fragment implements OnMapReadyCallback {
    private static final int PERMISSION_REQUEST_CODE = 1;
    //Message sent to contacts
    private String emergencySMS = "Panic Button Triggered!";
    //Camera and FlashLight
    private CameraManager objCameraManager;
    private String mCameraId;
    private boolean AlarmOn_Off;
    private boolean ActiveMode = false;
    private boolean useSoundLight = false;
    private boolean defaultMap;
    private String longitudeSTR;
    private String latitudeSTR;
    private String currentAddress;
    //Control Alarm Sound and Volume
    private AudioManager mAudioManager = null;
    private int originalVolume = 0;
    private MediaPlayer mp = null;
    //Buttons and TextView
    private Button alarmbtn;
    private Button silent;
    private Button activeSleepMode;
    private TextView showLocationTxt;
    //LocationManager locationManager;
    private Location mlocation;
    //Display Map
    private GoogleMap mMap;
    private final LatLng USA = new LatLng(39.8097343, -98.5556199);
    private Criteria criteria;
    private LocationManager mlocationManager;
    private final Looper looper = null;
    private LocationListener locationListener;
    //User and Database
    private FirebaseAuth mFireBaseAuth;
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private Contact myContacts;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_map, container, false);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapView);
        //mapFragment =   view.findFragmentById(R.id.mapView);
        mapFragment.getMapAsync(this);
        //User Instance
        mFireBaseAuth = FirebaseAuth.getInstance();
        // Read from database
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("Contacts/");
        EncUtil.generateKey(getActivity().getApplicationContext());
        // loadUserDetails();
        //Buttons
        activeSleepMode = view.findViewById(R.id.activeSleepModebtn);
        //Take Picture
        final Button takePicBtn = view.findViewById(R.id.TakePicBtn);
        //declare variables
        Button extraBTN = view.findViewById(R.id.silentBtb);
        //Make sound true or flase. Default False
        silent = view.findViewById(R.id.silentBtb);

        //Address TextView
        showLocationTxt = view.findViewById(R.id.show_location);

        //Button Animation
        final Animation myAnim = AnimationUtils.loadAnimation(getActivity(),R.anim.bounce);


        /**
         * Go to TakePicture
         */
        takePicBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePicBtn.startAnimation(myAnim);
                //Create new activity to take picture
                Intent gotoPicture = new Intent(getActivity(), TakePictureActivity.class);
                startActivity(gotoPicture);
                //StyleableToast.makeText(getActivity(),"Taking Picture",R.style.myToastPositive).show();
            }
        });

        /**
         *Check permission for location and SMS
         */
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkPermission()) {
                Log.e("Permission", "Permission already granted.");
            } else {
                requestPermission();
            }
        }
        //Check if device has Flashlight
        Boolean isFlashAvailable = getActivity().getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        if (!isFlashAvailable) {
            AlertDialog alert = new AlertDialog.Builder(getActivity()).create();
            alert.setTitle(getString(R.string.app_name));
            alert.setMessage(getString(R.string.msg_error));
            alert.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.flashAvailable), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    getActivity().finish();
                }
            });
            alert.show();
            return null;
        }
        //Get Access to Camera
        objCameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        try {
            mCameraId = objCameraManager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        /**
         * Get Location longitude and latitude
         */
        //Setting Criteria
        setCriteria();
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                mlocation = location;
                latitudeSTR = String.valueOf(location.getLatitude());
                longitudeSTR = String.valueOf(location.getLongitude());

                //Check if active mode is true
                if (!getActiveMode() && !defaultMap) {
                    //If active False->reset map
                    mMap.clear();
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(USA, 3));
                    mMap.setMinZoomPreference(3);
                    mMap.setMaxZoomPreference(5);
                    showLocationTxt.setText(R.string.address);
                    defaultMap = true;
                } else if (getActiveMode()) {
                    StyleableToast.makeText(getActivity(),"Getting Location",R.style.myToastPositive).show();
                    defaultMap = false;
                    mMap.clear();
                    mMap.resetMinMaxZoomPreference();
                    if (!latitudeSTR.isEmpty() && !longitudeSTR.isEmpty()) {
                        //Create new Location
                        LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        //Find information about the location
                        Geocoder geocoder = new Geocoder(getActivity().getApplicationContext(), Locale.getDefault());
                        try {
                            List<Address> listAddress = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                            if (listAddress != null && listAddress.size() > 0) {
                                currentAddress = "";
                                currentAddress += listAddress.get(0).getAddressLine(0);
                                showLocationTxt.setText(currentAddress);
                                showLocationTxt.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
                                //Get Time
                                Date currentTime = Calendar.getInstance().getTime();
                                //Add marker to Map
                                mMap.addMarker(new MarkerOptions().position(myLocation)
                                        .title("My Location. Time: " + currentTime.toString())
                                        .snippet("Longitude: " + longitudeSTR + " Latitude: " + latitudeSTR));

                                CameraPosition.Builder camBuilder = CameraPosition.builder();
                                camBuilder.bearing(0);
                                camBuilder.tilt(30);
                                camBuilder.target(myLocation);
                                camBuilder.zoom(18);
                                CameraPosition cp = camBuilder.build();
                                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cp));
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                System.out.println("Status Changed " + status);
            }

            @Override
            public void onProviderEnabled(String provider) {
                System.out.println("Provider Enable " + provider);
            }

            @Override
            public void onProviderDisabled(String provider) {
                System.out.println("Provider Disable " + provider);
            }
        };

        /*
         * Configure Active Mode listener
         */
        activeSleepMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (activeSleepMode.getText().toString().equalsIgnoreCase("Active Mode")) {
                    activeSleepMode.startAnimation(myAnim);
                    setActiveModes(true);
                    configureUIActiveMode();
                    //Update Google map
                    updateMap();
                } else {
                    setActiveModes(false);
                    configureUIActiveMode();
                    //Update Google map
                    if (checkPermission()) {
                        mlocationManager.requestSingleUpdate(criteria, locationListener, looper);
                    } else {
                        requestPermission();
                    }
                }
            }
        });


        silent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (silent.getText().toString().equalsIgnoreCase("Silent Off")) {
                    silent.startAnimation(myAnim);
                    silent.setText(R.string.silent_on);
                    useSoundLight = true;
                } else {
                    silent.startAnimation(myAnim);
                    silent.setText(R.string.silent_off);
                    useSoundLight = false;
                }
            }
        });

        //Alarm
        mAudioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        mp = MediaPlayer.create(getActivity(), R.raw.sample);
        setAlarmStatus(false);
        alarmbtn = view.findViewById(R.id.Alarmbtn);
        alarmbtn.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            if (alarmbtn.getText().toString().equalsIgnoreCase("S.O.S")) {
                                                alarmbtn.startAnimation(myAnim);

                                                //Update Google map
                                                if (!getActiveMode()) {
                                                    setActiveModes(true);
                                                    configureUIActiveMode();
                                                }
                                                //Update Google map
                                                updateMap();
                                                originalVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                                                setAlarmStatus(true);
                                                Thread myThread = new Thread() {
                                                    public void run() {
                                                        Looper.prepare();
                                                        while (getAlarmStatus() && useSoundLight) {
                                                            blinkFlash();
                                                            if (!mp.isPlaying() && getAlarmStatus()) {
                                                                playAlarm();
                                                            }
                                                        }
                                                        if (useSoundLight) {
                                                            StopAlarm();
                                                        }
                                                    }
                                                };
                                                myThread.start();

                                                Thread smsThread = new Thread() {
                                                    public void run() {
                                                        Looper.prepare();
                                                        while (currentAddress == null) {

                                                        }
                                                        //Edit final message
                                                        sendTextMessage(emergencySMS);
                                                    }
                                                };
                                                smsThread.start();
                                                if(useSoundLight) {
                                                    alarmbtn.setTextSize(22);
                                                    alarmbtn.setText(R.string.alarmOff);
                                                }
                                                StyleableToast.makeText(getActivity(),"Message Sent",R.style.myToastPositive).show();
                                            } else {
                                                alarmbtn.startAnimation(myAnim);
                                                if (mp != null && mp.isPlaying()) {
                                                    StopAlarm();
                                                    alarmbtn.setText(R.string.alarmOn);
                                                }else{
                                                    alarmbtn.setText(R.string.alarmOn);
                                                }
                                            }
                                        }
                                    }
        );

        return view;
    }//EOF onCreateView

    /**
     * Manipulates the map when it's available.
     * The API invokes this callback when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user receives a prompt to install
     * Play services inside the SupportMapFragment. The API invokes this method after the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //final LatLng FLORIDA = new LatLng(27.994402,-81.760254);
        //Change map's type
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        // Move the camera instantly to USA  with a zoom of 3.
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(USA, 3));
        mMap.setMinZoomPreference(3);
        mMap.setMaxZoomPreference(5);
        //Configure Map
        UiSettings uiSettings = mMap.getUiSettings();
        uiSettings.setIndoorLevelPickerEnabled(true);
        uiSettings.setMyLocationButtonEnabled(true);
        uiSettings.setMapToolbarEnabled(true);
        uiSettings.setCompassEnabled(true);
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setScrollGesturesEnabled(true);
        defaultMap = true;
    }

    /**
     * Get Location and Update Map
     */
    public void updateMap() {
        if (checkPermission()) {
            getLocation(mlocationManager, criteria, locationListener, looper);
        } else {
            requestPermission();
        }
    }

    /**
     * Set the criteria for the location
     */
    public void setCriteria() {
        criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setSpeedRequired(false);
        criteria.setCostAllowed(false);
        criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
        criteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);
        mlocationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
    }


    /**
     * Set Alarm status for running threads
     *
     * @param alarmOn_Off a boolean
     */
    public void setAlarmStatus(boolean alarmOn_Off) {
        this.AlarmOn_Off = alarmOn_Off;
    }

    /**
     * GET Alarm status
     *
     * @return true if button "Start Alarm" is presses
     */
    public boolean getAlarmStatus() {
        return AlarmOn_Off;
    }

    /**
     * Change the Active/Sleep Button settings
     */
    public void configureUIActiveMode() {
        if (getActiveMode()) {
            activeSleepMode.setText(R.string.sleep_mode);
            activeSleepMode.setTextColor(Color.WHITE);
            activeSleepMode.setBackground(getResources().getDrawable(R.drawable.button_sleep_mode));
        } else {
            activeSleepMode.setText(R.string.active_mode);
            activeSleepMode.setTextColor(Color.BLACK);
            activeSleepMode.setBackground(getResources().getDrawable(R.drawable.button_active_mode));
        }
    }

    /**
     * Set Active Mode
     *
     * @param activeMode
     */
    public void setActiveModes(boolean activeMode) {
        this.ActiveMode = activeMode;
    }

    /**
     * Get Active Mode status
     *
     * @return true if "Active Mode" is pressed
     */
    public boolean getActiveMode() {
        return ActiveMode;
    }

    /**
     * Construct Link to send with the SMS
     *
     * @return a string representing the current location
     */
    public String getStringLocation() {
        //String loc = "Longitude: " + this.longitudeSTR + "\n" + "Latitude: " + this.latitudeSTR + "\n";
        //String googleMap = "https://www.google.com/maps/search/?api=1&query=" + this.latitudeSTR + "," + this.longitudeSTR;
        return this.currentAddress;
    }

    public void getLocation(final LocationManager mlocationManager, final Criteria criteria, final LocationListener locationListener, final Looper looper) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                while (getActiveMode()) {
                    long startTime = System.currentTimeMillis();
                    //Wait time for the active mode 1 min
                    long waitTime = 1 * 60000;
                    long endTime = startTime + waitTime;
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if (getActivity() == null) {
                                setActiveModes(false);
                            } else if (checkPermission()) {
                                mlocationManager.requestSingleUpdate(criteria, locationListener, looper);
                            } else {
                                requestPermission();
                            }
                        }
                    });
                    while (endTime > System.currentTimeMillis()) {
                        if (!getActiveMode()) {
                            endTime = 0;
                        }
                    }
                }
            }
        });
    }


    /**
     * Turn on FlashLight
     */
    public void turnOnLight() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                objCameraManager.setTorchMode(mCameraId, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Turn off FlashLight
     */
    public void turnOffLight() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                objCameraManager.setTorchMode(mCameraId, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Blink Flash Light
     */
    private void blinkFlash() {
        String myString = "0101010101";
        long blinkDelay = 50; //Delay in ms
        for (int i = 0; i < myString.length(); i++) {
            if (myString.charAt(i) == '0') {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        objCameraManager.setTorchMode(mCameraId, true);
                    }
                } catch (CameraAccessException e) {
                }
            } else {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        objCameraManager.setTorchMode(mCameraId, false);
                    }
                } catch (CameraAccessException e) {
                }
            }
            try {
                Thread.sleep(blinkDelay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Play Alarm Sound
     */
    public void playAlarm() {
        //Code to turn on volume
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mp.start();
    }

    public void StopAlarm() {
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0);
        mp.pause();
        mp.seekTo(0);
        setAlarmStatus(false);
        turnOffLight();
    }


    /**
     * Check Permission for GPS Location and SMS
     *
     * @return true if permission granted
     */
    private boolean checkPermission() {
        int resultSMS = ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.SEND_SMS);
        int resultGPS = ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION);
        return resultSMS == PackageManager.PERMISSION_GRANTED && resultGPS == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request Permissions
     */
    private void requestPermission() {
        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getActivity(), "Permission accepted", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getActivity(), "Permission denied", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    /**
     * Get information from database
     */
    private void loadUserDetails() {
        DatabaseReference userReference = myRef.child(mFireBaseAuth.getUid());
        userReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                myContacts = dataSnapshot.getValue(Contact.class);
                if (myContacts != null) {
                    //System.out.println(myContacts.toString());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getActivity(), "Error reading Database", Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     * Send Messages to saved Contacts
     *
     * @param mySMS
     */
    public void sendTextMessage(String mySMS) {
        if (myContacts == null) return;
        mySMS += "\n";
        mySMS += currentAddress;
        mySMS += "\nPlease Check on me.\n";
        mySMS += "Message generated by ArchAngel.";
        String phoneNum1 = EncUtil.decrypt(getActivity().getApplicationContext(),myContacts.getPhoneNumber1());
        String phoneNum2 = EncUtil.decrypt(getActivity().getApplicationContext(),myContacts.getPhoneNumber2());
        if (!TextUtils.isEmpty(mySMS) && !TextUtils.isEmpty(phoneNum1) && !TextUtils.isEmpty(phoneNum2)) {
            if (checkPermission()) {
                //Get the default SmsManager//
                SmsManager smsManager = SmsManager.getDefault();
                //Send the SMS//
                smsManager.sendTextMessage(phoneNum1, null, mySMS, null, null);
                smsManager.sendTextMessage(phoneNum2, null, mySMS, null, null);
            } else {
                Toast.makeText(getActivity(), "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Check if user is login onStart
     */
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        if (mFireBaseAuth.getCurrentUser() == null) {
            Intent inToMain = new Intent(getActivity(), MainActivity.class);
            startActivity(inToMain);
            getActivity().finish();
        } else {
            new MyAsyncTask().execute();
        }
    }

    private class MyAsyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            // Read from database
            database = FirebaseDatabase.getInstance();
            myRef = database.getReference("Contacts/");
            DatabaseReference userReference = myRef.child(Objects.requireNonNull(mFireBaseAuth.getUid()));
            userReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    myContacts = dataSnapshot.getValue(Contact.class);
                    if (myContacts == null) {
                        Intent goToContacts = new Intent(getActivity(), SecondActivityContacts.class);
                        startActivity(goToContacts);
                        getActivity().finish();
                    } else {
                        System.out.println(myContacts.toString());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    //Toast.makeText(ThreadActivity.this, R.string.error_loading_user, Toast.LENGTH_SHORT).show();
                }
            });
            return null;
        }
    }
}
