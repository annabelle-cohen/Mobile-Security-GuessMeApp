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

interface  SensorServiceLightListenner{
    enum ALARM_STATE_LIGHT{
        ON,OFF
    }

    void alarmStateChangedLight(ALARM_STATE_LIGHT state);
    void isLightOk();
    void isLightNOTOk();
}
public class SensorServiceLight extends Service implements SensorEventListener {

    //This is a Tag for logging
    private final static String TAG= "SENSOR_SERVICE LIGHT";

    //This Threshold is for the alarm state.
    private final double THRESHOLD_MIN = 100;

    //This Threshold is for the alarm state.
    private final double THRESHOLD_MAX = 500;

    //The current alarm state that the service is in.
    private SensorServiceLightListenner.ALARM_STATE_LIGHT currentAlarmState = SensorServiceLightListenner.ALARM_STATE_LIGHT.OFF;

    //Binder given to clients.
    private final IBinder binder = new SensorServiceLightBinder();

    //int for save the light
    private double light;

    private boolean isMeasureLight = false;

    //the listenner to who we send events regarding the alarm.
    SensorServiceLightListenner mListenner;

    //sensor manager to get sensor
    SensorManager sensorManager;

    //the actual sensor we are using
    Sensor myLight;

    //this is the first sensor event that LOCKS the initial "light".
    SensorEvent firstEvent;
    public SensorServiceLight() {
    }


    public class SensorServiceLightBinder extends Binder{
        void registerListenner(SensorServiceLightListenner listenner){
            mListenner = listenner;
        }

        void startPullingLight(){
            sensorManager.registerListener(SensorServiceLight.this,myLight,SensorManager.SENSOR_DELAY_NORMAL);
        }

        void stopPullingLight(){
            sensorManager.unregisterListener(SensorServiceLight.this);
        }

        void resetInitLight(){
            firstEvent = null;
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();

        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        myLight = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        if(myLight != null){
            Log.d("Sensor output","Light available");
        }else{
            Log.d("Sensor output","Light not available");
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager = null;
        myLight=null;
        mListenner=null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(firstEvent == null){
            firstEvent = event;
            light = firstEvent.values[0];
        }else{
            if(!isMeasureLight){
                light = firstEvent.values[0];
                if(light<THRESHOLD_MIN||light>THRESHOLD_MAX){
                    currentAlarmState = SensorServiceLightListenner.ALARM_STATE_LIGHT.ON;
                }else{
                    currentAlarmState = SensorServiceLightListenner.ALARM_STATE_LIGHT.OFF;
                }

                if(currentAlarmState.equals(SensorServiceLightListenner.ALARM_STATE_LIGHT.ON)){
                    mListenner.alarmStateChangedLight(currentAlarmState);
                    mListenner.isLightNOTOk();
                }else{
                    mListenner.isLightOk();

                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public IBinder onBind(Intent intent) {
      return binder;
    }
}