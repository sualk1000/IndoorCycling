package de.sualk1000.indoorcycler;

import android.app.Activity;

public class UberManager
{
    private static UberManager instance = new UberManager();

    private Activity_IndoorCycling mainActivity = null;

    private UberManager()
    {

    }

    public static UberManager getInstance()
    {
        return instance;
    }

    public void setMainActivity( Activity_IndoorCycling mainActivity )
    {
        this.mainActivity = mainActivity;
    }

    public Activity_IndoorCycling getMainActivity()
    {
        return mainActivity;
    }

    public void cleanup()
    {
        mainActivity = null;
    }
}