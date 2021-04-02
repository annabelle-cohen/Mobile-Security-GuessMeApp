package com.example.hw1_mobilesecurity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;

public class BatteryReceiver extends BroadcastReceiver {
    private boolean isBatteryOk = false;

    public boolean isBatteryOk() {
        return isBatteryOk;
    }

    public void setBatteryOk(boolean batteryOk) {
        isBatteryOk = batteryOk;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        if(action !=null && action.equals(Intent.ACTION_BATTERY_CHANGED)){

           //Percentage
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE,-1);
            int percentage = level * 100 /scale;

            if(percentage >= 90){
                setBatteryOk(false);
            }else if(90 > percentage && percentage>=65){
                setBatteryOk(true);
            }else if(65>percentage && percentage >= 40){
                setBatteryOk(false);
            }else if(40 > percentage && percentage >= 15){
                setBatteryOk(true);
            }else{
                setBatteryOk(false);
            }

        }

    }
}
