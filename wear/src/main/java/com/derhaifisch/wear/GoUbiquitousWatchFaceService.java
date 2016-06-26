package com.derhaifisch.wear;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.view.ConfirmationOverlay;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.security.PrivateKey;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Created by Flavio on 6/14/2016.
 */
public class GoUbiquitousWatchFaceService extends CanvasWatchFaceService {

    private static final String TAG = "GoUbiquitousWatchFace";

    private static final String WEATHER_PATH = "/weather";
    private static final String HIGH = "HIGH";
    private static final String LOW = "LOW";
    private static final String KEY_WEATHER_ID = "weatherId";

    Bitmap mWeatherIcon;
    String mWeatherHigh;
    String mWeatherLow;

    private GoogleApiClient mGoogleApiClient;

    @Override
    public Engine onCreateEngine() {
        return new GoUbiquitousWatchFaceEngine();
    }

    private class GoUbiquitousWatchFaceEngine extends CanvasWatchFaceService.Engine implements
            DataApi.DataListener,
            GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener {

        private static final String WEATHER_PATH = "/weather";

        private static final String KEY_HIGH = "HIGH";
        private static final String KEY_LOW = "LOW";
        private static final String KEY_WEATHER_ID = "weatherId";

        Bitmap mWeatherIcon;
        Double mWeatherHigh;
        Double mWeatherLow;

        final Rect mCardBounds = new Rect();

        private Typeface WATCH_TEXT_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
        private Typeface WATCH_TEXT_DATE = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

        private  static final int MSG_UPDATE_TIME_ID = 42;
        private long mUpdateRateMs = 1000;

        private Paint mBackgroundColorPaint;
        private Paint mTextColorPaint;
        private Paint mDatePaint;

        private boolean mHasTimeZoneReceiverBeenRegistered = false;
        private boolean mIsMuteMode;
        private  boolean mIsLowBitAmbient;

        private int mBackgroundColor = Color.parseColor("#03A9F4");
        private int mTextColor = Color.parseColor("#FFFFFF");

        private Calendar mCalendar;

        private final android.os.Handler mTimeHandler = new android.os.Handler(){

            @Override
            public void handleMessage(Message msg){

             switch (msg.what){
                 case MSG_UPDATE_TIME_ID: {
                     invalidate();
                     if(isVisible() && !isInAmbientMode()){
                         long currentTimeMillis = System.currentTimeMillis();
                         long delay = mUpdateRateMs - (currentTimeMillis % mUpdateRateMs);
                         mTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME_ID, delay);
                     }
                 }
             }
            }

        };

        final BroadcastReceiver mTimerZoneBroadCastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                long now = System.currentTimeMillis();
                mCalendar.setTimeInMillis(now);
            }
        };

        GoogleApiClient mGoogleApiClient = new
                GoogleApiClient.Builder(GoUbiquitousWatchFaceService.this)
        .addConnectionCallbacks(GoUbiquitousWatchFaceEngine.this)
        .addOnConnectionFailedListener(GoUbiquitousWatchFaceEngine.this)
        .addApi(Wearable.API)
        .build();

        @Override
        public void onDataChanged(DataEventBuffer dataEventBuffer) {
            for (DataEvent dataEvent : dataEventBuffer) {
                if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                    DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
                    String path = dataEvent.getDataItem().getUri().getPath();
                    Log.d(TAG, path);
                    if (path.equals(WEATHER_PATH)) {
                        if (dataMap.containsKey(KEY_HIGH)) {
                            mWeatherHigh = dataMap.getDouble(KEY_HIGH);
                            Log.d(TAG, "High = " + mWeatherHigh);
                        } else {
                            Log.d(TAG, "What? No high?");
                        }

                        if (dataMap.containsKey(KEY_LOW)) {
                            mWeatherLow = dataMap.getDouble(KEY_LOW);
                            Log.d(TAG, "Low = " + mWeatherLow);
                        } else {
                            Log.d(TAG, "What? No low?");
                        }

                        if (dataMap.containsKey(KEY_WEATHER_ID)) {
/*                            int weatherId = dataMap.getInt(KEY_WEATHER_ID);
                            Drawable b = getResources().getDrawable(Utility.getIconResourceForWeatherCondition(weatherId));
                            Bitmap icon = ((BitmapDrawable) b).getBitmap();
                            float scaledWidth = (mTextTempHighPaint.getTextSize() / icon.getHeight()) * icon.getWidth();
                            mWeatherIcon = Bitmap.createScaledBitmap(icon, (int) scaledWidth, (int) mTextTempHighPaint.getTextSize(), true);*/

                        } else {
                            Log.d(TAG, "What? no weatherId?");
                        }

                        invalidate();
                    }
                }
            }
        }

        @Override
        public void onCreate(SurfaceHolder holder) {

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onCreate");
            }
            super.onCreate(holder);

            mGoogleApiClient = new GoogleApiClient.Builder(GoUbiquitousWatchFaceService.this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            setWatchFaceStyle(new WatchFaceStyle.Builder(GoUbiquitousWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setPeekOpacityMode(WatchFaceStyle.PEEK_OPACITY_MODE_TRANSLUCENT)
                    .build());

            mCalendar = Calendar.getInstance();

            initBackground();
            initDisplayText();
            initDisplayDate();

            if (!mGoogleApiClient.isConnected()) {
                mGoogleApiClient.connect();
            }
        }

        private  void initBackground()
        {
            mBackgroundColorPaint = new Paint();
            mBackgroundColorPaint.setColor(mBackgroundColor);
        }

        private void initDisplayText(){
            mTextColorPaint = new Paint();
            mTextColorPaint.setColor(mTextColor);
            mTextColorPaint.setTypeface(WATCH_TEXT_TYPEFACE);
            mTextColorPaint.setAntiAlias(true);
            mTextColorPaint.setTextSize(64);
        }

        private  void initDisplayDate(){
            mDatePaint = new Paint();
            mDatePaint.setColor(mTextColor);
            mDatePaint.setTypeface(WATCH_TEXT_DATE);
            mDatePaint.setAntiAlias(true);
            mDatePaint.setTextSize(32);
        }

        @Override
        public void onVisibilityChanged(boolean visible){
            super.onVisibilityChanged(visible);

            if(visible){

                if(!mHasTimeZoneReceiverBeenRegistered){
                    IntentFilter filter = new IntentFilter(Intent.ACTION_TIME_CHANGED);
                    GoUbiquitousWatchFaceService.this.registerReceiver(mTimerZoneBroadCastReceiver, filter);
                    mHasTimeZoneReceiverBeenRegistered = true;
                }

            } else {

                if(mHasTimeZoneReceiverBeenRegistered){
                    GoUbiquitousWatchFaceService.this.unregisterReceiver(mTimerZoneBroadCastReceiver);
                    mHasTimeZoneReceiverBeenRegistered = false;
                }
            }

            updateTimer();
        }

        private  void updateTimer(){
            mTimeHandler.removeMessages(MSG_UPDATE_TIME_ID);
            if(isVisible() && !isInAmbientMode())
            {
                mTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME_ID);
            }
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets){
            super.onApplyWindowInsets(insets);
        }

        @Override
        public void onPropertiesChanged(Bundle properties){
            super.onPropertiesChanged(properties);

            if(properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false)){
                mIsLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            }
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode){
            super.onAmbientModeChanged(inAmbientMode);

            if(inAmbientMode){
                mTextColorPaint.setColor(Color.parseColor("white"));
            } else {
                mTextColorPaint.setColor(Color.parseColor("red"));
            }

            if(mIsLowBitAmbient){
                mTextColorPaint.setAntiAlias(!inAmbientMode);
            }

            invalidate();
            updateTimer();
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter){
            super.onInterruptionFilterChanged(interruptionFilter);

            boolean isDeviceMuted = (interruptionFilter ==
                    WatchFaceService.INTERRUPTION_FILTER_NONE);

            if(isDeviceMuted){
             mUpdateRateMs = TimeUnit.MINUTES.toMillis(1);
            } else {
                mUpdateRateMs = 1000;
            }

            if(mIsMuteMode != isDeviceMuted){
                mIsMuteMode = isDeviceMuted;
                int alpha = (isDeviceMuted) ? 100 : 255;
                mTextColorPaint.setAlpha(alpha);
                invalidate();
                updateTimer();
            }
        }

        @Override
        public void onTimeTick(){
            super.onTimeTick();
            if(Log.isLoggable(TAG, Log.DEBUG)){
                Log.d(TAG, "onTimeTick: ambient = " + isInAmbientMode());
            }
            invalidate();
        }

        @Override
        public void onPeekCardPositionUpdate(Rect bounds) {
            super.onPeekCardPositionUpdate(bounds);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onPeekCardPositionUpdate: " + bounds);
            }
            super.onPeekCardPositionUpdate(bounds);
            if (!bounds.equals(mCardBounds)) {
                mCardBounds.set(bounds);
                invalidate();
            }
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);

            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            drawBackground(canvas, bounds);
            drawText(canvas, bounds);
            drawWeatherImage(canvas, bounds);
        }

        private void drawBackground(Canvas canvas, Rect bounds){
            canvas.drawRect(0,0, bounds.width(), bounds.height(), mBackgroundColorPaint);
        }

        private void drawText(Canvas canvas, Rect bounds){

            String timeText = getHoursString() + ":" + String.format("%02d", mCalendar.get(Calendar.MINUTE));

            if(isInAmbientMode() || mIsMuteMode){
             timeText += (mCalendar.get(Calendar.HOUR) < 12) ? "AM" : "PM";
            } else {
                timeText += ":" + String.format("%02d", Calendar.SECOND);
            }

            float timeXOffset = computeXOffset(timeText, mTextColorPaint, bounds);
            float timeYOffset = computeTimeYOffset(timeText, mTextColorPaint, bounds);

            canvas.drawText(timeText, timeXOffset , timeYOffset , mTextColorPaint);

            //Date of the day
            drawTextLine(canvas, bounds, mDatePaint, GetCurrentDate(), 40f);

            //Temperature
            drawTextLine(canvas, bounds, mDatePaint, mWeatherLow + " | " + mWeatherHigh, 100f);
        }

        private void drawTextLine(Canvas canvas, Rect bounds, Paint paint, String textContent, float top)
        {
            float timeXOffset = computeXOffset(textContent, paint, bounds);
            float timeYOffset = computeTimeYOffset(textContent, paint, bounds);
            canvas.drawText(textContent, timeXOffset , timeYOffset + top , paint);
        }

        private void drawWeatherImage(Canvas canvas, Rect bounds) {
/*
            Bitmap weatherPic = BitmapFactory.decodeResource(getResources(), R.drawable.art_clear);
            int picResize = weatherPic.getWidth() / 2;
            Bitmap weatherPicResized = Bitmap.createScaledBitmap(weatherPic, picResize, picResize, true);

            int centerX = (Math.round(bounds.exactCenterX()) - 1) - (picResize/ 2);

            canvas.drawBitmap(weatherPicResized, centerX, 40, null);
*/
        }

        private String getHoursString(){
            if(mCalendar.get(Calendar.HOUR) % 12 ==0)
                return "12";
            else if (mCalendar.get(Calendar.HOUR) <= 12)
                return String.valueOf(mCalendar.get(Calendar.HOUR));
            else
                return String.valueOf(mCalendar.get(Calendar.HOUR) -12);
        }

        private float computeXOffset(String text, Paint paint, Rect watchBounds) {
            float centerX = watchBounds.exactCenterX();
            float timeLength = paint.measureText(text);
            return centerX - (timeLength / 2.0f);
        }

        private float computeTimeYOffset(String timeText, Paint timePaint, Rect watchBounds) {
            float centerY = watchBounds.exactCenterY();
            Rect textBounds = new Rect();
            timePaint.getTextBounds(timeText, 0, timeText.length(), textBounds);
            int textHeight = textBounds.height();
            return centerY + (textHeight / 2.0f);
        }

        private String GetCurrentDate()
        {
            mCalendar = Calendar.getInstance();
            mCalendar.set(Calendar.YEAR, mCalendar.getTime().getYear());
            mCalendar.set(Calendar.DAY_OF_MONTH, mCalendar.getTime().getDay());
            mCalendar.set(Calendar.MONTH, mCalendar.getTime().getMonth());
            String format = new SimpleDateFormat("EEE, MMM d, ''yy").format(mCalendar.getTime());
            return format;
        }

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Wearable.DataApi.addListener(mGoogleApiClient, GoUbiquitousWatchFaceEngine.this);
        }

        @Override
        public void onConnectionSuspended(int i) {

        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        }

        @Override
        public void onDestroy() {
            super.onDestroy();
        }
    }
}

