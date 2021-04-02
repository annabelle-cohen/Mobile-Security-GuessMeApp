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

import com.example.hw1_mobilesecurity.Common.Common;

interface SensorServiceTemparttureListenner{
    enum ALARM_STATE_TEMP{
        ON,OFF
    }

    void alarmStateChangedTemp(ALARM_STATE_TEMP state);
    void isTemperatureOk();
    void isTemperatureNOTOk();
}
public class SensorServiceTemperature extends Service implements SensorEventListener {

    //This is a Tag for logging
    private final static  String TAG = "SENSOR_SERVICE TEMPERATURE";

    //This Threshold is for the alarm state.
    private final double THRESHOLD_MIN = 15;
    //This Threshold is for the alarm state.
    private final double THRESHOLD_MAX = 25;


    //The current alarm state that the service is in.
    private SensorServiceTemparttureListenner.ALARM_STATE_TEMP currentAlarmState = SensorServiceTemparttureListenner.ALARM_STATE_TEMP.OFF;

    //Binder given to clients.
    private final IBinder binder = new SensorServiceTemperatureBinder();
    //save the temperature
    private float ambient_temperature;

    //the listenner to who we send events regarding the alarm.
    SensorServiceTemparttureListenner mListenner;

    //sensor manager to get sensor
    SensorManager sensorManager;

    //the actual sensor we are using
    Sensor myTemperature;

    //this is the first sensor event that LOCKS the initial "temperature".
    SensorEvent firstEvent;

    boolean isMeasureTemperature = false;


    public SensorServiceTemperature() {
    }

    public class SensorServiceTemperatureBinder extends Binder{

        void registerListenner(SensorServiceTemparttureListenner listenner){
            Log.d("Binder Temperature","registering...");
            mListenner=listenner;
        }

        void startMeasureTemperature(){
            sensorManager.registerListener(SensorServiceTemperature.this,myTemperature,SensorManager.SENSOR_DELAY_NORMAL);
        }

        void stopMeasureTemperature(){
            sensorManager.unregisterListener(SensorServiceTemperature.this);
        }

        void resetInitTemperature(){
            firstEvent = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return  binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        if(sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null){
            myTemperature = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
            /*
            we know that in some devices the temperature is not available so this is the way we fix that.
            * */
            Common.TemperatureIsNotAvailable = false;
            Log.d("Sensor output","Temperature available");
        }else{
            Common.TemperatureIsNotAvailable = true;
            Log.d("Sensor output","Temperature not available");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        sensorManager= null;
        myTemperature = null;
        mListenner=null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(firstEvent == null){
            firstEvent = event;
            ambient_temperature = firstEvent.values[0];
        }else{
            if(!isMeasureTemperature){
                double tempNew = firstEvent.values[0];
                if(tempNew < THRESHOLD_MIN || tempNew>THRESHOLD_MAX){
                    currentAlarmState = SensorServiceTemparttureListenner.ALARM_STATE_TEMP.ON;
                }else{
                    currentAlarmState= SensorServiceTemparttureListenner.ALARM_STATE_TEMP.OFF;
                }

                if(currentAlarmState.equals(SensorServiceTemparttureListenner.ALARM_STATE_TEMP.ON)){
                    mListenner.alarmStateChangedTemp(currentAlarmState);
                    mListenner.isTemperatureNOTOk();
                }else{
                    mListenner.isTemperatureOk();
                }
            }

        }



    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}