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

interface SensorServiceAccelListenner{
    enum ALARM_STATE{
        ON,OFF
    }

    void alarmStateChanged(ALARM_STATE state);

    void isOrientationOk();
    void isOrientationNotOk();
}
public class SensorServiceAccel extends Service implements SensorEventListener {

    //This is a Tag for logging
    private final static String TAG = "SENSOR_SERVICE ACCELOMETER";

    //This Threshold is for the alarm state.
    private final double THRESHOLD = 1;
    //The current alarm state that the service is in.
    private SensorServiceAccelListenner.ALARM_STATE currentAlarmState = SensorServiceAccelListenner.ALARM_STATE.OFF;
    //Binder given to clients.
    private final IBinder binder = new SensorServiceAccelBinder();

    //the listenner to who we send events regarding the alarm.
    SensorServiceAccelListenner mListenner;
    //sensor manager to get sensor
    SensorManager sensorManager;
    //the actual sensor we are using
    Sensor myAccel;
    //this is the first sensor event that LOCKS the initial "orientation".
    SensorEvent firstEvent;

    private double first_X;
    private double first_Y;
    private double first_Z;

    private boolean isStopPulling=false;

    public SensorServiceAccel() {
    }


    public class SensorServiceAccelBinder extends Binder{
        void registerListener(SensorServiceAccelListenner listenner){
            Log.d("Binder Accelerometer","registering...");
           mListenner = listenner;
        }

        void startPullingSensorAccel(){
            sensorManager.registerListener(SensorServiceAccel.this,myAccel,SensorManager.SENSOR_DELAY_NORMAL);
        }

        void stopPullingSensorAccel(){
            sensorManager.unregisterListener(SensorServiceAccel.this);
        }

        void resetInitLock(){
            firstEvent = null;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager=(SensorManager)getSystemService(Context.SENSOR_SERVICE);
        myAccel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if(myAccel !=null){
            Log.d("Sensor output","Accelerometer available");
        }else{
            Log.d("Sensor output","Accelerometer NOT available");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        sensorManager = null;
        myAccel = null;
        mListenner = null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if(firstEvent == null){
            firstEvent =event;
            this.first_X=firstEvent.values[0];
            this.first_Y=firstEvent.values[1];
            this.first_Z=firstEvent.values[2];
        }else {
            if (!isStopPulling) {
                double changeX = event.values[0];
                double absDiffX = Math.abs(this.first_X - changeX);
                double changeY = event.values[1];
                double absDiffY = Math.abs(this.first_Y - changeY);
                double changeZ = event.values[2];
                double absDiffZ = Math.abs(this.first_Z - changeZ);


                if (absDiffX > THRESHOLD || absDiffY > THRESHOLD || absDiffZ > THRESHOLD) {
                    currentAlarmState = SensorServiceAccelListenner.ALARM_STATE.ON;

                } else {
                    currentAlarmState = SensorServiceAccelListenner.ALARM_STATE.OFF;
                }

                if (currentAlarmState.equals(SensorServiceAccelListenner.ALARM_STATE.ON)) {
                    mListenner.alarmStateChanged(currentAlarmState);
                    mListenner.isOrientationNotOk();
                } else {
                    mListenner.isOrientationOk();
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