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
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Dialog to allow configuring a weight scale user profile
 */
public class Dialog_BLEScanner extends DialogFragment
{
    private final static String TAG = Dialog_BLEScanner.class.getSimpleName();
    public Dialog_ConfigSettings dialog_ConfigSettings;
    public int deviceType;
    BLEScanner bleScanner = null;
    private ListView simpleList;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle("BLE Scanner");
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View detailsView = inflater.inflate(R.layout.dialog_ble_scanner, null);
        detailsView.setMinimumHeight(600);

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(detailsView);

        // Add action buttons
        //builder.setPositiveButton("OK",null);

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        bleScanner.stopScan(false);
                        //Let dialog dismiss
                    }
                });

        simpleList = (ListView) detailsView.findViewById(R.id.bleItemsList);
        bleScanner = BLEScanner.getInstance();

        //list.add(values[i]);
        final StableArrayAdapter adapter = new StableArrayAdapter(getActivity(),
                android.R.layout.simple_list_item_1, bleScanner.bleItemList);
        simpleList.setAdapter(adapter);


        simpleList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {
                String item = (String) parent.getItemAtPosition(position);

                if(deviceType == 1)
                    dialog_ConfigSettings.setHearRate(item);
                else
                    dialog_ConfigSettings.setBike(item);

                bleScanner.stopScan(false);
                AlertDialog dialog = (AlertDialog) getDialog();
                dialog.dismiss();


            }

        });

        bleScanner.bleItemAdapter = adapter;
        bleScanner.startScan();
        return builder.create();
    }

    /*
    @Override
    public void onResume() {
        super.onResume();

        AlertDialog dialog = (AlertDialog) getDialog();

        Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                bleScanner.stopScan();
                if(selected_item == null)
                {
                    UberManager.getInstance().getMainActivity().indoorCyclingService.sendTextMessage("Please select");
                    return;
                }

                dialog_ConfigSettings.setHearRate(selected_item);
                dialog.dismiss();

            }
        });
        if(bleScanner.isDiscovering() == false)
            bleScanner.startScan();
    }*/
    class StableArrayAdapter extends ArrayAdapter<String> {

        HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

        public StableArrayAdapter(Context context, int textViewResourceId,
                                  List<String> objects) {
            super(context, textViewResourceId, objects);
            for (int i = 0; i < objects.size(); ++i) {
                mIdMap.put(objects.get(i), i);
            }
        }

        @Override
        public long getItemId(int position) {
            String item = getItem(position);
            if(mIdMap.get(item) == null)
            {
                return -1;
            }
            return mIdMap.get(item);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

    }

}
