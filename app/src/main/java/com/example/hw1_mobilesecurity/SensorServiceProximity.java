package com.example.hw1_mobilesecurity;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

interface SensorServiceProximityListenner{
    enum ALARM_STATE_PROXY{
        OFF,ON
    }

    void alarmStateChangedProx(ALARM_STATE_PROXY state);
    void isProxy();
    void isNOTproxy();
}
public class SensorServiceProximity extends Service implements SensorEventListener {

    //This is a Tag for logging
    private final static String TAG= "SENSOR_SERVICE PROXIMITY";

    //This Threshold is for the alarm state.
    private final double THRESHOLD_MIN = 6;

    //This Threshold is for the alarm state.
    private final double THRESHOLD_MAX = 8.5;

    //The current alarm state that the service is in.
    private SensorServiceProximityListenner.ALARM_STATE_PROXY currentAlarmState = SensorServiceProximityListenner.ALARM_STATE_PROXY.OFF;

    SensorServiceProximityListenner mListenner;
    //Binder given to clients.
    private final IBinder binder = new SensorServiceProximity.SensorServiceProximityBinder();

    //int for save the proximity
    private double proximity;

    private boolean isMeasureProximity = false;

    //sensor manager to get sensor
    SensorManager sensorManager;

    //the actual sensor we are using
    Sensor myProximity;

    //this is the first sensor event that LOCKS the initial "proximity".
    SensorEvent firstEvent;

    public SensorServiceProximity() {
    }


    public class SensorServiceProximityBinder extends Binder{
        void registerListenner(SensorServiceProximityListenner listenner){
            mListenner = listenner;
        }

        void startMeasureProximity(){
            sensorManager.registerListener(SensorServiceProximity.this,myProximity,SensorManager.SENSOR_DELAY_NORMAL);
        }

        void stopMeasureProximity(){
            sensorManager.unregisterListener(SensorServiceProximity.this);
        }

        void  resetInitProximity(){
            firstEvent = null;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        myProximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        if(myProximity != null){
            Log.d("Sensor output","Proximity available");
        }else{
            Log.d("Sensor output","Proximity not available");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager = null;
        myProximity = null;
        mListenner = null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(firstEvent == null){
            firstEvent = event;
        }
        this.proximity = firstEvent.values[0];
        if(!isMeasureProximity){
            if(proximity<THRESHOLD_MIN || proximity>THRESHOLD_MAX){
               currentAlarmState = SensorServiceProximityListenner.ALARM_STATE_PROXY.ON;
            }else{
                currentAlarmState = SensorServiceProximityListenner.ALARM_STATE_PROXY.OFF;
            }

            if(currentAlarmState.equals(SensorServiceProximityListenner.ALARM_STATE_PROXY.ON)){
                mListenner.alarmStateChangedProx(currentAlarmState);
                mListenner.isNOTproxy();
            }else{
                mListenner.isProxy();
            }
        }


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public IBinder onBind(Intent intent) {
    return  binder;
    }
}