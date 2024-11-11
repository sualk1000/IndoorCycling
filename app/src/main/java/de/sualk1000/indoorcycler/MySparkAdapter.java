package de.sualk1000.indoorcycler;


import android.graphics.RectF;


import java.util.ArrayList;

/*
public class MySparkAdapter extends SparkAdapter {
    private ArrayList<Float> yData;

    public MySparkAdapter(float[] yData) {
        this.yData = new ArrayList<Float>();
        for (float y:yData) {
            this.yData.add( Float.valueOf(y));

        }

    }
    public MySparkAdapter(ArrayList yData) {
        this.yData = yData;

    }

    @Override
    public int getCount() {
        return yData.size();
    }

    @Override
    public Object getItem(int index) {
        return yData.get(index).floatValue();
    }

    @Override
    public float getY(int index) {
        return yData.get(index).floatValue();
    }

    @Override
    public boolean hasBaseLine()
    {
        return true;
    }

    @Override
    public RectF getDataBounds() {
        final int count = getCount();
        final boolean hasBaseLine = hasBaseLine();

        float minY = 0f;
        float maxY = 30f;
        float minX = 0;
        float maxX = 300;
        // set values on the return object
        return createRectF(minX, minY, maxX, maxY);
    }

    RectF createRectF(float left, float top, float right, float bottom) {
        return new RectF(left, top, right, bottom);
    }

    public void remove(int i) {
        yData.remove(i);
    }

    public void add(float count) {
        yData.add(count);
    }
}

 */