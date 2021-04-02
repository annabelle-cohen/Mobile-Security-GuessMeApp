package com.example.hw1_mobilesecurity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

import com.example.hw1_mobilesecurity.Common.Common;
import com.example.hw1_mobilesecurity.Model.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rengwuxian.materialedittext.MaterialEditText;

public class MainActivity extends AppCompatActivity implements SensorServiceAccelListenner,SensorServiceTemparttureListenner,SensorServiceLightListenner,SensorServiceProximityListenner{

    EditText edtUsername,edtPassword;
    TextView errorTxt;
    Button btnSignIn;

    boolean isSucceedToGuess = true;
    boolean stopVersionOfError = false;

    /*flags for all services we have*/
    boolean isBound = false;
    boolean isBound2 = false;
    boolean isBound3 = false;
    boolean isBound4 = false;

    /*flags for checking if the user succeed guessing*/
    boolean isOrientationGood=false;
    boolean isTempGood = false;
    boolean isLightGood = false;
    boolean isProximityGood = false;

    /*we want to count the contacts every time maybe the user notice the guess is dependent on number of contacts*/
    boolean isFirstTimeContacts = true;

    int numberOfContacts = 0;
    static int countMistakes = 0;

    /*
    * we have created list of connections for the different versions of services we have.
    * */
    private ServiceConnection mConnetion[] = new ServiceConnection[4];
    /*we have class that check the battery level*/
    private BatteryReceiver batteryReceiver = new BatteryReceiver();
    private IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

    SensorServiceAccel.SensorServiceAccelBinder mBinder;
    SensorServiceTemperature.SensorServiceTemperatureBinder mBinder2;
    SensorServiceLight.SensorServiceLightBinder mBinder3;
    SensorServiceProximity.SensorServiceProximityBinder mBinder4;

    /*constant values for the permission*/
    private static final int PERMISSION_CONTACTS_REQUEST_CODE = 123;
    private static final int MANUALLY_CONTACTS_PERMISSION_REQUEST_CODE = 124;

    /*the minimum number of contacts the user need for guessing*/
    private static final int NUMBER_OF_MINIMUM_CONTACTS=1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connect();
        getContacts();

        edtUsername=(MaterialEditText)findViewById(R.id.edtUsername);
        edtPassword=(MaterialEditText)findViewById(R.id.edtPassword);
        errorTxt =(TextView)findViewById(R.id.ErrorInGuess);
        btnSignIn=(Button)findViewById(R.id.btnSignIn2);

        //Init FireBase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference table_user= database.getReference("Users");

        edtUsername.setText(R.string.textForUserNameDefault);

        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                table_user.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        if(!isFirstTimeContacts){
                            numberOfContacts = 0;
                            getContacts();
                        }

                        //Check if user not exist in database
                        if (dataSnapshot.child(edtUsername.getText().toString()).exists() && isBound && isBound2 &&isBound3 && isBound4) {
                            Common.currentUser = dataSnapshot.child(edtUsername.getText().toString()).getValue(User.class);

                            //check if there is enough contacts
                            boolean isEnough= numberOfContacts < NUMBER_OF_MINIMUM_CONTACTS ? false : true;

                            if(!Common.currentUser.getUserName().equals(edtUsername.getText().toString()) || !Common.currentUser.getPassword().equals(edtPassword.getText().toString())||!isOrientationGood ||!isTempGood ||!isLightGood || !batteryReceiver.isBatteryOk() ||!isProximityGood||!isEnough){
                                vibrate(getApplicationContext());
                                isSucceedToGuess = false;
                                countMistakes++;
                                if(!stopVersionOfError){
                                    showError(countMistakes);
                                }else{
                                    errorTxt.setText(R.string.Error5);
                                }

                            }else{
                                errorTxt.setTextColor(Color.GREEN);
                                errorTxt.setText(R.string.CleanErr);
                                Intent Succeed = new Intent(MainActivity.this,SucceedLog.class);
                                startActivity(Succeed );
                                finish();
                            }


                        } else {
                            Toast.makeText(MainActivity.this
                                    , R.string.userNotExist, Toast.LENGTH_SHORT).show();
                        }
                    }

                    private void showError(int countMistakes) {
                        int number = countMistakes / 3;
                        switch (number){
                            case 1:
                                errorTxt.setText(R.string.Error1);
                                break;
                            case 2:
                                errorTxt.setText(R.string.Error2);
                                break;
                            case 3:
                                errorTxt.setText(R.string.Error3);
                                break;
                            case 4:
                                errorTxt.setText(R.string.Error4);
                                break;
                            case 5:
                                errorTxt.setText(R.string.Error5);
                                stopVersionOfError = true;
                                break;
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        });

    }

    private void getContacts(){
        boolean isGranted = checkForPermission();

        if (!isGranted) {
            requestPermission();
            return;
        }

        if(isFirstTimeContacts){
            isFirstTimeContacts = false;
        }

        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);

        if ((cur != null ? cur.getCount() : 0) > 0) {
            while (cur != null && cur.moveToNext()) {
                String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                if (cur.getInt(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Cursor pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id}, null);
                    while (pCur.moveToNext()) {
                        String phoneNo = pCur.getString(pCur.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER));
                        numberOfContacts++;

                    }
                    pCur.close();
                }
            }
        }
        if(cur!=null){
            cur.close();
        }
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.READ_CONTACTS},
                PERMISSION_CONTACTS_REQUEST_CODE);
    }

    private void requestPermissionWithRationaleCheck() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_CONTACTS)) {
            Log.d("pttt", "shouldShowRequestPermissionRationale = true");
            // Show user description for what we need the permission
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    PERMISSION_CONTACTS_REQUEST_CODE);
        } else {
            Log.d("pttt", "shouldShowRequestPermissionRationale = false");
            openPermissionSettingDialog();
        }
    }

    private void openPermissionSettingDialog() {
        String message = "Setting screen if user have permanently disable the permission by clicking Don't ask again checkbox.";
        AlertDialog alertDialog =
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(message)
                        .setPositiveButton(getString(android.R.string.ok),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent();
                                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                                        intent.setData(uri);
                                        startActivityForResult(intent, MANUALLY_CONTACTS_PERMISSION_REQUEST_CODE);
                                        dialog.cancel();
                                    }
                                }).show();
        alertDialog.setCanceledOnTouchOutside(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_CONTACTS_REQUEST_CODE: {

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getContacts();
                } else {
                    requestPermissionWithRationaleCheck();
                    Toast.makeText(MainActivity.this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }


    private boolean checkForPermission() {
        if ( ContextCompat.checkSelfPermission( this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    private void connect(){
        for(int i = 0 ;i<mConnetion.length;i++){
            int index = i;
            mConnetion[i] = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    switch (index){
                        case 0:
                            Log.d("Service Connection 0","bound to service");
                            mBinder = (SensorServiceAccel.SensorServiceAccelBinder)service;
                            mBinder.registerListener(MainActivity.this);
                            Log.d("Service Connection 0","registered as a listener");
                            isBound=true;
                            mBinder.startPullingSensorAccel();
                            break;
                        case 1:
                            Log.d("Service Connection 1","bound service");
                            mBinder2 = (SensorServiceTemperature.SensorServiceTemperatureBinder)service;
                            mBinder2.registerListenner(MainActivity.this);
                            Log.d("Service Connection 1","registered as a listener");
                            /*
                            here we check if the on create  change the flag if there is no temperature sensor available on the device
                            the default is true and then the program will continue without this sensor.
                             */
                            isBound2=true;
                            if(!Common.TemperatureIsNotAvailable){
                                mBinder2.startMeasureTemperature();
                            }else{
                                isTempGood= true;
                            }

                            break;
                        case 2:
                            Log.d("Service Connection 2","bound service");
                            mBinder3 = (SensorServiceLight.SensorServiceLightBinder)service;
                            mBinder3.registerListenner(MainActivity.this);
                            Log.d("Service Connection 2","registered as a listener");
                            isBound3=true;
                            mBinder3.startPullingLight();
                            break;
                        case 3:
                            Log.d("Service Connection 3","bound service");
                            mBinder4 = (SensorServiceProximity.SensorServiceProximityBinder)service;
                            mBinder4.registerListenner(MainActivity.this);
                            Log.d("Service Connection 3","registered as a listener");
                            isBound4=true;
                            mBinder4.startMeasureProximity();
                            break;
                        default:
                            Log.d("in Default","dd");
                            break;
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    switch (index){
                        case 0:
                            isBound=false;
                            break;
                        case 1:
                            isBound2=false;
                            break;
                        case 2:
                            isBound3=false;
                            break;
                        case 3:
                            isBound4 = false;
                            break;
                        default:
                            Log.d("in Default","dd");
                            break;
                    }
                }
            };

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(isBound){
            mBinder.resetInitLock();
            mBinder.startPullingSensorAccel();
        }

        if(isBound2){
            mBinder2.resetInitTemperature();
            mBinder2.startMeasureTemperature();
        }
        if(isBound3){
            mBinder3.resetInitLight();
            mBinder3.startPullingLight();
        }

        if(isBound4){
            mBinder4.resetInitProximity();
            mBinder4.startMeasureProximity();
        }

        registerReceiver(batteryReceiver,intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(isBound){
            mBinder.stopPullingSensorAccel();
        }

        if(isBound2){
            mBinder2.stopMeasureTemperature();
        }

        if(isBound3){
            mBinder3.stopPullingLight();
        }
        if(isBound4){
            mBinder4.stopMeasureProximity();
        }

        unregisterReceiver(batteryReceiver);

    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this,SensorServiceAccel.class);
       bindService(intent,mConnetion[0], Context.BIND_AUTO_CREATE);

       Intent intent2 = new Intent(this,SensorServiceTemperature.class);
       bindService(intent2,mConnetion[1],Context.BIND_AUTO_CREATE);

       Intent intent3 = new Intent(this,SensorServiceLight.class);
       bindService(intent3,mConnetion[2],Context.BIND_AUTO_CREATE);

        Intent intent4 = new Intent(this,SensorServiceProximity.class);
        bindService(intent4,mConnetion[3],Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(isBound){
            unbindService(mConnetion[0]);
            isBound=false;
        }
        if(isBound2){
            unbindService(mConnetion[1]);
            isBound2=false;
        }
        if(isBound3){
            unbindService(mConnetion[2]);
            isBound3=false;
        }
        if(isBound4){
            unbindService(mConnetion[3]);
            isBound4= false;
        }

    }

    public static void vibrate(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            //deprecated in API 26
            vibrator.vibrate(500);
        }
    }


    @Override
    public void alarmStateChanged(ALARM_STATE state) {

    }

    @Override
    public void isOrientationOk() {
        isOrientationGood=true;
    }

    @Override
    public void isOrientationNotOk() {
        isOrientationGood=false;
    }

    @Override
    public void alarmStateChangedTemp(ALARM_STATE_TEMP state) {

    }

    @Override
    public void isTemperatureOk() {
        isTempGood = true;
    }

    @Override
    public void isTemperatureNOTOk() {
        isTempGood = false;
    }

    @Override
    public void alarmStateChangedLight(ALARM_STATE_LIGHT state) {

    }

    @Override
    public void isLightOk() {
        isLightGood = true;
    }

    @Override
    public void isLightNOTOk() {
        isLightGood = false;
    }

    @Override
    public void alarmStateChangedProx(ALARM_STATE_PROXY state) {

    }

    @Override
    public void isProxy() {
        isProximityGood = true;
    }

    @Override
    public void isNOTproxy() {
        isProximityGood = false;
    }
}