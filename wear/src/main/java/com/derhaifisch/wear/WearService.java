package com.derhaifisch.wear;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by Flavio on 6/26/2016.
 */
public class WearService extends WearableListenerService {

    private static final String TAG = "WearService";

    private static final String KEY_UUID = "uuid";

    private static final String WEATHER_PATH = "/weather";
    private static final String HIGH = "HIGH";
    private static final String LOW = "LOW";
    private static final String KEY_WEATHER_ID = "weatherId";

    Bitmap mWeatherIcon;
    String mWeatherHigh;
    String mWeatherLow;

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        for (DataEvent dataEvent : dataEventBuffer) {
            if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
                String path = dataEvent.getDataItem().getUri().getPath();

                Log.d(TAG, path);
                if (path.equals(WEATHER_PATH)) {
                    if (dataMap.containsKey(HIGH)) {
                        mWeatherHigh = dataMap.getString(HIGH);
                        Log.d(TAG, "High = " + mWeatherHigh);
                    } else {
                        Log.d(TAG, "What? No high?");
                    }

                    if (dataMap.containsKey(LOW)) {
                        mWeatherLow = dataMap.getString(LOW);
                        Log.d(TAG, "Low = " + mWeatherLow);
                    } else {
                        Log.d(TAG, "What? No low?");
                    }

                    if (dataMap.containsKey(KEY_WEATHER_ID)) {
/*                        int weatherId = dataMap.getInt(KEY_WEATHER_ID);
                        Drawable b = getResources().getDrawable(Utility.getIconResourceForWeatherCondition(weatherId));
                        Bitmap icon = ((BitmapDrawable) b).getBitmap();
                        float scaledWidth = (mTextTempHighPaint.getTextSize() / icon.getHeight()) * icon.getWidth();
                        mWeatherIcon = Bitmap.createScaledBitmap(icon, (int) scaledWidth, (int) mTextTempHighPaint.getTextSize(), true);*/

                    } else {
                        Log.d(TAG, "What? no weatherId?");
                    }
                }
            }
        }
    }
}
