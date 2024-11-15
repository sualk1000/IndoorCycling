/*
This software is subject to the license described in the License.txt file
included with this software distribution. You may not use this file except in compliance
with this license.

Copyright (c) Dynastream Innovations Inc. 2013
All rights reserved.
*/

package de.sualk1000.indoorcycler;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;


/**
 * Dialog to allow configuring a weight scale user profile
 */
public class Dialog_ConfigSettings extends DialogFragment
{
    private final static String TAG = Dialog_ConfigSettings.class.getSimpleName();


    EditText et_username;
    EditText et_password;
    Button button_heartrate_ble;
    Button button_bike_ble;
    CheckBox cb_garmin;


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle("Settings");
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View detailsView = inflater.inflate(R.layout.dialog_fe_settings, null);

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(detailsView);

        // Add action buttons
        builder.setPositiveButton("OK",null);
        /*builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                //checkSettings(dialog);
            }
        });*/

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        //Let dialog dismiss
                    }
                });

        et_username = (EditText) detailsView.findViewById(R.id.editText_Username);
        et_password = (EditText) detailsView.findViewById(R.id.editText_Password);
        button_heartrate_ble = (Button) detailsView.findViewById(R.id.buttonHeartRate);

        button_heartrate_ble.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Dialog_BLEScanner dialog = new Dialog_BLEScanner();

                dialog.dialog_ConfigSettings = Dialog_ConfigSettings.this;
                dialog.deviceType = 1;
                dialog.show(getFragmentManager(), "Scan BLE");


            }

        });
        button_bike_ble = (Button) detailsView.findViewById(R.id.buttonBike);
        button_bike_ble.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Dialog_BLEScanner dialog = new Dialog_BLEScanner();

                dialog.dialog_ConfigSettings = Dialog_ConfigSettings.this;
                dialog.deviceType = 2;
                dialog.show(getFragmentManager(), "Scan BLE");


            }

        });

        cb_garmin = (CheckBox) detailsView.findViewById(R.id.checkBox_garmin);
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(getActivity().getApplicationContext().getPackageName() + ".settings", Context.MODE_PRIVATE);

        String bleHeartbeatDeviceName = sharedPreferences.getString("heartbeat", "");
        if(bleHeartbeatDeviceName.equals("") ==false)
            button_heartrate_ble.setText(bleHeartbeatDeviceName);

        String bleBikeDeviceName = sharedPreferences.getString("bike", "");
        if(bleBikeDeviceName.equals("") ==false)
            button_bike_ble.setText(bleBikeDeviceName);

        String garmin_user = sharedPreferences.getString("garmin_user", "");

        if(garmin_user.equals("") == false)
            et_username.setText(garmin_user);
        String garmin_pwd = sharedPreferences.getString("garmin_pwd", "");
        if(garmin_pwd.equals("") == false)
            et_password.setText(garmin_pwd);

        String use_garmin = sharedPreferences.getString("use_garmin", "");
        if(use_garmin.equals("true") )
            cb_garmin.setChecked(true);
        else
            cb_garmin.setChecked(false);


        return builder.create();
    }
    @Override
    public void onResume() {
        super.onResume();

        AlertDialog dialog = (AlertDialog) getDialog();

        Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                new signInTask(Dialog_ConfigSettings.this).execute();

            }
        });
    }

    public void setHearRate(String string) {

        button_heartrate_ble.setText(string);
    }

    public void setBike(String item) {
        button_bike_ble.setText(item);
    }

    private class signInTask extends AsyncTask<Void, Void, Void> {

        private final Dialog_ConfigSettings dialogSettings;

        signInTask(Dialog_ConfigSettings dialogSettings)
        {
            this.dialogSettings = dialogSettings;
        }
        @Override
        protected Void doInBackground(Void... voids) {
            try {

            SharedPreferences sharedPreferences = dialogSettings.getActivity().getSharedPreferences(dialogSettings.getActivity().getApplicationContext().getPackageName() + ".settings", Context.MODE_PRIVATE);
            SharedPreferences.Editor sharedPreferenceEditor = sharedPreferences.edit();
            if(cb_garmin.isChecked()) {
                GarminConnect garminConnect = new GarminConnect();

                String userName = et_username.getText().toString();
                if (userName.equals(""))
                {
                    UberManager.getInstance().getMainActivity().indoorCyclingService.sendTextMessage("Username missing");
                    return null;
                }
                String pwd = et_password.getText().toString();

                String garmin_user = sharedPreferences.getString("garmin_user", "");
                String garmin_pwd = sharedPreferences.getString("garmin_pwd", "");

                if(garmin_user.equals(userName) == false || garmin_pwd.equals(pwd) == false) {
                    garminConnect.resetTokens(getActivity());
                }
                if (garminConnect.signin(userName,pwd,dialogSettings.getActivity()) == false)
                {
                    UberManager.getInstance().getMainActivity().indoorCyclingService.sendTextMessage("Error in Signin to Garmin");

                    return null;
                }


                sharedPreferenceEditor.putString("garmin_user", userName);
                sharedPreferenceEditor.putString("garmin_pwd", pwd);
                sharedPreferenceEditor.putString("use_garmin", "true");


            }else
            {
                sharedPreferenceEditor.putString("use_garmin", "false");
            }
                sharedPreferenceEditor.putString("heartbeat", button_heartrate_ble.getText().toString());
                sharedPreferenceEditor.putString("bike", button_bike_ble.getText().toString());
                sharedPreferenceEditor.commit();
            } catch (Exception e) {
                Log.e(TAG, e.toString());
                UberManager.getInstance().getMainActivity().indoorCyclingService.sendTextMessage("Failed: " + e.getMessage());

            }



            dialogSettings.getDialog().dismiss();
            return null;
        }
    }
    }
