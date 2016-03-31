package com.dubatovk.mymetronom;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.util.Log;

public class MyService extends Service {

    private static final String LOG_TAG = MyService.class.getSimpleName();

    private static final int VOLUME = 100;
    private static final int DURATION_MS = 50;
    private static final int DEFAULT_VALUE = 300;
    private static final int VIBRATION_DURATION = 50;

    public static final String ARGS_IS_VIBRATION = "is_vibration";
    public static final String ARGS_GET_VALUE = "get_value";
    public static final String ARGS_IS_TONE = "is_tone";

    private boolean isTone;
    private boolean isVibration;

    private Vibrator vibrator;
    private ToneGenerator toneG;

    private Handler handler;
    private Runnable runnable;

    private int interval;

    private final MyBinder binder = new MyBinder();
    private ServiceCallbacks serviceCallbacks;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "onCreate");

        handler = new Handler();
        toneG = new ToneGenerator(AudioManager.STREAM_ALARM, VOLUME);
        vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
    }

    public void setCallbacks(ServiceCallbacks callbacks) {
        serviceCallbacks = callbacks;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "onStartCommand");

        interval = intent.getIntExtra(ARGS_GET_VALUE, DEFAULT_VALUE);
        isVibration = intent.getBooleanExtra(ARGS_IS_VIBRATION, true);
        isTone = intent.getBooleanExtra(ARGS_IS_TONE, true);

        setInterval(interval);
        startTask();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy");

        super.onDestroy();
        handler.removeCallbacks(runnable);
    }

    void setInterval(int interval) {
        int minute = 60000;
        this.interval = minute / interval;
    }

    void setIsVibration(boolean isVibration) {
        this.isVibration = isVibration;
    }

    void setIsTone(boolean isTone) {
        this.isTone = isTone;
    }

    private void startTask() {

        runnable = new Runnable() {
            public void run() {
                handler.postDelayed(this, interval);
                if (isVibration) {
                    vibrator.vibrate(VIBRATION_DURATION);
                }
                if (isTone) {
                    toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, DURATION_MS);
                }
                serviceCallbacks.changeColorIndicator();
            }
        };
        handler.postDelayed(runnable, interval);
    }

    class MyBinder extends Binder {
        MyService getService() {
            return MyService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "onBind");
        return binder;
    }

    public boolean onUnbind(Intent intent) {
        Log.d(LOG_TAG, "onUnbind");
        return super.onUnbind(intent);
    }
}
