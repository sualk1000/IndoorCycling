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
import android.os.Bundle;
import android.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;


/**
 * Displays a modal waiting dialog to wait for a result and optionally show progress reports.
 */
public class Dialog_ProgressWaiter extends DialogFragment
{
    TextView textView_status;
    String actionDescription;

    public Dialog_ProgressWaiter()
    {
        this.actionDescription = "";
        setCancelable(false);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View detailsView = inflater.inflate(R.layout.dialog_progresswaiter, null);
        builder.setView(detailsView);

        textView_status = (TextView)detailsView.findViewById(R.id.textView_Status);
        setStatus(actionDescription + "...");

        return builder.create();
    }

    public void setStatus(final String newStatus)
    {
        this.actionDescription = newStatus;
        if(getActivity() == null)
            return;

        getActivity().runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {

                        textView_status.setText(actionDescription );
                    }
                });
    }

}
