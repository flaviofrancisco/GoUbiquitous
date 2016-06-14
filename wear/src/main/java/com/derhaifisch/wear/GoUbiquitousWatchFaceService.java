package com.derhaifisch.wear;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Message;
import android.support.wearable.view.ConfirmationOverlay;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;

import java.security.PrivateKey;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Created by Flavio on 6/14/2016.
 */
public class GoUbiquitousWatchFaceService extends CanvasWatchFaceService {

    private static final String TAG = "GoUbiquitousWatchFace";

    @Override
    public Engine onCreateEngine() {
        return new GoUbiquitousWatchFaceEngine();
    }

    private class GoUbiquitousWatchFaceEngine extends CanvasWatchFaceService.Engine {

        static final int BORDER_WIDTH_PX = 5;

        final Rect mCardBounds = new Rect();
        final Paint mPaint = new Paint();

        private Typeface WATCH_TEXT_TYPEFACE = Typeface.create(Typeface.SERIF, Typeface.NORMAL);
        private  static final int MSG_UPDATE_TIME_ID = 42;
        private long mUpdateRateMs = 1000;
        private Time mDisplayTime;

        private Paint mBackgroundColorPaint;
        private Paint mTextColorPaint;

        private boolean mHasTimeZoneReceiverBeenRegistered = false;
        private boolean mIsMuteMode;
        private  boolean mIsLowBitAmbient;

        private float mXOffset;
        private float mYOffset;

        private int mBackgroundColor = Color.parseColor("black");
        private int mTextColor = Color.parseColor("red");

        final BroadcastReceiver mTimerZoneBroadCastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mDisplayTime.clear(intent.getStringExtra("time-zone"));
                mDisplayTime.setToNow();
            }
        };

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

        @Override
        public void onCreate(SurfaceHolder holder) {

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onCreate");
            }
            super.onCreate(holder);
            setWatchFaceStyle(new WatchFaceStyle.Builder(GoUbiquitousWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setPeekOpacityMode(WatchFaceStyle.PEEK_OPACITY_MODE_TRANSLUCENT)
                    .build());

            mDisplayTime = new Time();
            initBackground();
            initDisplayText();
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
            mTextColorPaint.setTextSize(32);
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onAmbientModeChanged: " + inAmbientMode);
            }
            super.onAmbientModeChanged(inAmbientMode);
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
            // Clear screen.
            canvas.drawColor(isInAmbientMode() ? Color.BLACK : Color.GREEN);

            // Draw border around card in interactive mode.
            if (!isInAmbientMode()) {
                mPaint.setColor(Color.MAGENTA);
                canvas.drawRect(mCardBounds.left - BORDER_WIDTH_PX,
                        mCardBounds.top - BORDER_WIDTH_PX,
                        mCardBounds.right + BORDER_WIDTH_PX,
                        mCardBounds.bottom + BORDER_WIDTH_PX, mPaint);
            }

            // Fill area under card.
            mPaint.setColor(isInAmbientMode() ? Color.RED : Color.GREEN);
            canvas.drawRect(mCardBounds, mPaint);
        }

        @Override
        public void onTimeTick(){
            super.onTimeTick();
            if(Log.isLoggable(TAG, Log.DEBUG)){
                Log.d(TAG, "onTimeTick: ambient = " + isInAmbientMode());
            }
        }
    }
}
