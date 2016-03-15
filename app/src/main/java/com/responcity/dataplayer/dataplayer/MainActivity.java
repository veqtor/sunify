package com.responcity.dataplayer.dataplayer;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.responcity.dataplayer.dataplayer.model.ConvertedPoint;
import com.responcity.dataplayer.dataplayer.model.ConvertedSmhiData;
import com.responcity.dataplayer.dataplayer.network.POJOS.SmhiPoint;
import com.responcity.dataplayer.dataplayer.network.SmhiService;
import com.responcity.dataplayer.dataplayer.service.DataConverter;
import com.responcity.dataplayer.dataplayer.view.MultiBallView;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.service.PdPreferences;
import org.puredata.android.service.PdService;
import org.puredata.core.PdBase;
import org.puredata.core.PdReceiver;
import org.puredata.core.utils.IoUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;



public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener, View.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, MultiBallView.BallTouchListener {
    private static final String TAG = "Pd Test";
    private static final int MIN_SAMPLE_RATE = 22050;


    private PdService pdService = null;
    private MyLocationListener locationListener;
    private int patch = 0;
    private Button playBtn;
    SmhiService smhiService;
    private GoogleApiClient mGoogleApiClient;
    private MultiBallView mbv;

    private TextView WindTw;
    private TextView TempTw;
    private TextView PrecTw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        playBtn = (Button) findViewById(R.id.playStopBtn);
        playBtn.setOnClickListener(this);
        mbv = (MultiBallView) findViewById(R.id.mbv);
        mbv.setListener(this);

        WindTw = (TextView) findViewById(R.id.twWindspeed);
        TempTw = (TextView) findViewById(R.id.twTemp);
        PrecTw = (TextView) findViewById(R.id.twPrec);

        mbv.setBallPos("wind", -0.5f, -0.33f);
        sendBallPos("wind", -0.5f, -0.33f);
        mbv.setBallPos("temp", 0.3f, -0.33f);
        sendBallPos("temp", 0.3f, -0.33f);
        mbv.setBallPos("prec", -0.33f, 0.5f);
        sendBallPos("prec", -0.33f, 0.5f);
        createLocationRequest();
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        setupClient();
        setupLocationListener();
        AudioParameters.init(this);
        PdPreferences.initPreferences(getApplicationContext());
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(this);
        bindService(new Intent(this, PdService.class), pdConnection, BIND_AUTO_CREATE);
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }
    }

    private void sendBallPos(String ball, float x, float y) {
        Log.d(TAG, ball + " x: " + x + " y: " + y);
        PdBase.sendFloat(ball + "_x",x);
        PdBase.sendFloat(ball + "_y",y);

    }

    private void sendBallStateChanged(String ball, boolean state) {
        PdBase.sendFloat(ball+ "_t", state? 1.f:0.f);
    }

    LocationManager locationMangaer;

    private void setupLocationListener() {
        locationListener = new MyLocationListener();
    }

    private void setupClient() {
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://opendata-download-metfcst.smhi.se")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        smhiService = retrofit.create(SmhiService.class);
    }

    private final ServiceConnection pdConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            pdService = ((PdService.PdBinder) service).getService();
            initPd();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // this method will never be called
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanup();
    }

    private void cleanup() {
        if(curPatch > -1)
            PdBase.closePatch(curPatch);
        curPatch = -1;
        if(mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
        try {

            unbindService(pdConnection);
        } catch (IllegalArgumentException e) {
            // already unbound
            pdService = null;
        }
    }
    protected void stopLocationUpdates() {
        try {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, locationListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean pdRunning() {
        return pdService != null && pdService.isRunning();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mbv.setBallPos("wind", -0.5f, -0.33f);
        sendBallPos("wind", -0.5f, -0.33f);
        mbv.setBallPos("temp", 0.3f, -0.33f);
        sendBallPos("temp", 0.3f, -0.33f);
        mbv.setBallPos("prec", -0.33f, 0.5f);
        sendBallPos("prec", -0.33f, 0.5f);

    }
    int curPatch = -1;

    private void initPd() {
        Resources res = getResources();
        File patchFile = null;
        try {
            if(curPatch > -1)
            PdBase.closePatch(curPatch);
            PdBase.setReceiver(receiver);
            PdBase.subscribe("pos");
            InputStream in = res.openRawResource(R.raw.test);
            patchFile = IoUtils.extractResource(in, "test.pd", getCacheDir());
            PdBase.openPatch(patchFile);
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            finish();
        } finally {
            if (patchFile != null) patchFile.delete();
        }
    }

    private void startAudio() {
        String name = getResources().getString(R.string.app_name);
        try {
            int srate = Math.max(MIN_SAMPLE_RATE, AudioParameters.suggestSampleRate());
            pdService.initAudio(srate, 0, 2, -1);   // negative values will be replaced with defaults/preferences
            pdService.startAudio(new Intent(this, MainActivity.class), R.drawable.sunificon, name, "Return to " + name + ".");
            if (lastKnownLat != null && !lastKnownLat.isEmpty()) {
                callSmhiServiceForPoint(lastKnownLon, lastKnownLat);
            }
        } catch (IOException e) {
            toast(e.toString());
        }
    }

    private void stopAudio() {
        pdService.stopAudio();
    }

    private Toast toast = null;


    private void toast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (toast == null) {
                    toast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);
                }
                toast.setText(TAG + ": " + msg);
                toast.show();
            }
        });
    }

    private PdReceiver receiver = new PdReceiver() {

        private void pdPost(String msg) {
            Log.d(TAG,"Pure Data says, \"" + msg + "\"");
        }

        @Override
        public void print(String s) {
            Log.d(TAG, "fpd: " + s);
        }

        @Override
        public void receiveBang(String source) {
            Log.d(TAG,"bang");
        }

        @Override
        public void receiveFloat(String source, float x) {
            Log.d(TAG, "rfloat - " + source + " : " + x);
            switch(source) {
                case "pos":
                    receivePosition((int)x);
            }
        }

        @Override
        public void receiveList(String source, Object... args) {
            Log.d(TAG,"list: " + Arrays.toString(args));
        }

        @Override
        public void receiveMessage(String source, String symbol, Object... args) {
            Log.d(TAG, source + " : " + symbol);
        }

        @Override
        public void receiveSymbol(String source, String symbol) {
            Log.d(TAG,"symbol: " + symbol + " from " + source);
        }
    };

    private void receivePosition(int x) {
        if(latestConvertedSmhiData != null) {
            if(x < 0 || x > 23) {
                return;
            }
            ConvertedPoint curPoint = latestConvertedSmhiData.getConvertedPoints().get(x);
            TempTw.setText(curPoint.getTemp() +  " C");
            PrecTw.setText(curPoint.getPrec() + " mm/h");
            WindTw.setText(curPoint.getWind() + " m/s");
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.playStopBtn:
                if (pdRunning()) {
                    stopAudio();
                } else {
                    startAudio();
                }
        }
    }

    String lastKnownLat;
    String lastKnownLon;

    private void callSmhiServiceForPoint(String lon, String lat) {
        Log.d(TAG, "calling smhiservice for p: " + lon + " lon, " + lat + " lat.");
        smhiService.getPoint(lon, lat).enqueue(new Callback<SmhiPoint>() {
            @Override
            public void onResponse(Call<SmhiPoint> call, Response<SmhiPoint> response) {
                if (MainActivity.this != null)
                    handleResponse(response.body());
            }

            @Override
            public void onFailure(Call<SmhiPoint> call, Throwable t) {
                Log.e(TAG, "failed call!", t);
            }
        });
    }

    NumberFormat formatter = new DecimalFormat("#0.000000");

    Location mLastLocation;

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {

            String lat = String.format(Locale.ENGLISH, "%.6f", mLastLocation.getLatitude());
            String lon = String.format(Locale.ENGLISH, "%.6f", mLastLocation.getLongitude());
            Log.d(TAG, "Location speed: " + mLastLocation.getSpeed() + " m/s");
            if(lastKnownLat != null && lat.contentEquals(lastKnownLat) && lon.contentEquals(lastKnownLon)) {
                return;
            }
            else {
                lastKnownLat = lat;
                lastKnownLon = lon;
                callSmhiServiceForPoint(lastKnownLon, lastKnownLat);
            }
        }
        startLocationUpdates();
    }

    LocationRequest mLocationRequest;

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(2500);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, locationListener);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onBallMoved(String ball, float x, float y) {
        sendBallPos(ball,x,y);
    }

    @Override
    public void onBallTouchUpDown(String ball, boolean down) {
        sendBallStateChanged(ball,down);
    }


    private class MyLocationListener implements com.google.android.gms.location.LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "locationChanged!");
            long curTime = System.currentTimeMillis();
            String lat = String.format(Locale.ENGLISH, "%.6f", location.getLatitude());
            String lon = String.format(Locale.ENGLISH, "%.6f", location.getLongitude());
            if(lastKnownLat != null && lat.contentEquals(lastKnownLat) && lon.contentEquals(lastKnownLon)) {
                return;
            }
            else {
                if(lastLocationTime > 0 && mLastLocation != null) {
                    calcSpeed(mLastLocation, lastLocationTime, location, curTime);
                }
                lastKnownLat = lat;
                lastKnownLon = lon;
                callSmhiServiceForPoint(lastKnownLon, lastKnownLat);
            }
            mLastLocation = location;
            lastLocationTime = curTime;
            Log.d(TAG, "Location speed: " + mLastLocation.getSpeed() + " m/s");
        }
    }

    long lastLocationTime;
    double speed_kph = 0.d;

    private void calcSpeed(Location lastLoc, long lastTime, Location loc, long time) {
        double distance = distance_on_geoid(lastLoc.getLatitude(), lastLoc.getLongitude(), loc.getLatitude(), loc.getLongitude());
        double timeDiff = (time - lastTime) / 1000.0;
        double speed_mps = distance / timeDiff;
        speed_kph = (speed_mps * 3600.0) / 10000.0;
        Log.d(TAG, "speed: " + speed_kph + " km/h");
    }

    double distance_on_geoid(double lat1, double lon1, double lat2, double lon2) {

        // Convert degrees to radians
        lat1 = lat1 * Math.PI / 180.0;
        lon1 = lon1 * Math.PI / 180.0;

        lat2 = lat2 * Math.PI / 180.0;
        lon2 = lon2 * Math.PI / 180.0;

        // radius of earth in metres
        double r = 6378100;

        // P
        double rho1 = r * Math.cos(lat1);
        double z1 = r * Math.sin(lat1);
        double x1 = rho1 * Math.cos(lon1);
        double y1 = rho1 * Math.sin(lon1);

        // Q
        double rho2 = r * Math.cos(lat2);
        double z2 = r * Math.sin(lat2);
        double x2 = rho2 * Math.cos(lon2);
        double y2 = rho2 * Math.sin(lon2);

        // Dot product
        double dot = (x1 * x2 + y1 * y2 + z1 * z2);
        double cos_theta = dot / (r * r);

        double theta = Math.acos(cos_theta);

        // Distance in Metres
        return r * theta;
    }

    ConvertedSmhiData latestConvertedSmhiData;

    private void handleResponse(SmhiPoint body) {
        if (body != null) {
            Log.d(TAG,"Location response!!!");
            latestConvertedSmhiData = DataConverter.convertSmhiPointSeries(body);
            updateData();
        }
    }

    int timeOffset = 0;

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        mbv.setBallPos("wind", -0.5f, -0.33f);
        sendBallPos("wind", -0.5f, -0.33f);
        mbv.setBallPos("temp", 0.3f, -0.33f);
        sendBallPos("temp", 0.3f, -0.33f);
        mbv.setBallPos("prec", -0.33f, 0.5f);
        sendBallPos("prec", -0.33f, 0.5f);

    }

    private void updateData() {
        if (pdRunning()) {
            try {
                PdBase.writeArray("temp", 0, latestConvertedSmhiData.getTempsArray(), timeOffset, 24);
                PdBase.writeArray("wind", 0, latestConvertedSmhiData.getWindsArray(), timeOffset, 24);
                PdBase.writeArray("prec", 0, latestConvertedSmhiData.getPrecsArray(), timeOffset, 24);
                PdBase.writeArray("prect", 0, latestConvertedSmhiData.getPrectsArray(), timeOffset, 24);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}
