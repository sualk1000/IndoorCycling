/*
This software is subject to the license described in the License.txt file
included with this software distribution. You may not use this file except in compliance
with this license.

Copyright (c) Dynastream Innovations Inc. 2013
All rights reserved.
*/

package de.sualk1000.indoorcycler.fitnessequipment;

import de.sualk1000.indoorcycler.R;
import de.sualk1000.indoorcycler.multidevicesearch.Activity_MultiDeviceSearchSampler;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceType;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.util.EnumSet;


/**
 * Dialog to allow configuring a weight scale user profile
 */
public class Dialog_ConfigSettings extends DialogFragment
{
    public static final String SETTINGS_GARMIN = "settings_garmin";
    public static final String SETTINGS_NAME = "settings_username";
    public static final String SETTINGS_PASSWORD = "settings_password";
    public static final String SETTINGS_BLE = "settings_ble";
    public static final String SETTINGS_FITNESS = "settings_fitness";

    EditText et_username;
    EditText et_password;
    Button button_ble;
    CheckBox cb_garmin;


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle("Settings Configuration");
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View detailsView = inflater.inflate(R.layout.dialog_fe_settings, null);

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(detailsView);

        // Add action buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                /*
                Intent i = new Intent(Dialog_ConfigSettings.this.getActivity(), Activity_FitnessEquipmentSampler.class);
                Bundle b = new Bundle();
                b.putString(SETTINGS_NAME, et_friendlyName.getText().toString());
                b.putShort(SETTINGS_AGE, Short.parseShort(et_age.getText().toString()));
                b.putFloat(SETTINGS_HEIGHT, Float.parseFloat(et_height.getText().toString())/100f); // Convert to m
                b.putFloat(SETTINGS_WEIGHT, Float.parseFloat(et_weight.getText().toString()));
                b.putBoolean(SETTINGS_GENDER, rb_male.isChecked());
                b.putBoolean(INCLUDE_WORKOUT,cb_workout.isChecked());
                i.putExtras(b);
                startActivity(i);
                */
                 dialog.dismiss();
            }
        });

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
        button_ble = (Button) detailsView.findViewById(R.id.buttonHeartRate);

        button_ble.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Toast.makeText(Dialog_ConfigSettings.this.getActivity(), "Scan BLE", Toast.LENGTH_LONG).show();
                Intent i = new Intent(Dialog_ConfigSettings.this.getActivity(), Activity_MultiDeviceSearchSampler.class);
                Bundle args = new Bundle();
                EnumSet<DeviceType> set = EnumSet.noneOf(DeviceType.class);
                set.add( DeviceType.FITNESS_EQUIPMENT);

                args.putSerializable(Activity_MultiDeviceSearchSampler.FILTER_KEY, set);
                i.putExtra(Activity_MultiDeviceSearchSampler.BUNDLE_KEY, args);
                // Listen for search stopped results
                startActivityForResult(i, Activity_MultiDeviceSearchSampler.RESULT_SEARCH_STOPPED);


            }

        });

        cb_garmin = (CheckBox) detailsView.findViewById(R.id.checkBox_garmin);

        return builder.create();
    }

}
