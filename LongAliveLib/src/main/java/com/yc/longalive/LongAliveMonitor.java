package com.yc.longalive;


import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.text.TextUtils;

import com.yc.appcommoninter.IEventTrack;
import com.yc.appcommoninter.ILogger;
import com.yc.appcommoninter.IMonitorToggle;

import java.util.HashMap;

/**
 * <pre>
 *     @author 杨充
 *     blog  : https://github.com/yangchong211
 *     time  : 2017/01/30
 *     desc  : 保活
 *     revise:
 * </pre>
 */
public final class LongAliveMonitor {

    private static final long INIT_DELAY_TIME_OFFSET = 5000L;
    private static final long WATCH_DOG_TIMER_INTERVAL = 15000L;
    private static final long WATCH_DOG_PERIOD_THRESHOLD = 60000L;
    private static final long WATCH_DOG_INTERVAL_UP_LIMIT = 43200000L;
    private static final long TIMESTAMP_NO_VALUE = 0L;
    private static final int PID_NO_VALUE = 0;
    private static Application sApplication;
    private static IMonitorToggle sToggle;
    private static IEventTrack sEventTrack;
    public static ILogger sLogger;
    /**
     * 获取handler对象，并且指定main主线程
     */
    private static final Handler sHandler = new Handler(Looper.getMainLooper());
    private static SharedPreferences sSP;
    private static long sLastLiveTimeStamp;
    private static int sLastPid;
    private static int sLastScreenState;
    public static int sCurrentScreenState = LongAliveConstant.LONGEVITY_SCREEN_STATE_ON;
    private static Bundle sSavedInstanceState;

    private static final Runnable sWatchDogRunnable = new Runnable() {
        @Override
        public void run() {
            if (LongAliveMonitor.sToggle.isOpen()) {
                long currentTimestamp = System.currentTimeMillis();
                int currentPid = LongAliveMonitor.getCurrentPid();
                long periodMillis = currentTimestamp - LongAliveMonitor.sLastLiveTimeStamp;
                if (periodMillis > WATCH_DOG_PERIOD_THRESHOLD && periodMillis < WATCH_DOG_INTERVAL_UP_LIMIT) {
                    if (currentPid == LongAliveMonitor.sLastPid) {
                        LongAliveMonitor.report(LongAliveConstant.LONGEVITY_MONITOR_EVENT_SLEEP,
                                LongAliveConstant.LONGEVITY_MONITOR_EVENT_SLEEP_TYPE_SLEEP,
                                periodMillis, currentTimestamp, LongAliveMonitor.sLastLiveTimeStamp);
                    } else if (LongAliveMonitor.sSavedInstanceState != null) {
                        LongAliveMonitor.report(LongAliveConstant.LONGEVITY_MONITOR_EVENT_SYSTEM_KILL,
                                "", periodMillis, currentTimestamp,
                                LongAliveMonitor.sLastLiveTimeStamp);
                    } else if (LongAliveMonitor.sLastScreenState == LongAliveConstant.LONGEVITY_SCREEN_STATE_OFF) {
                        LongAliveMonitor.report(
                                LongAliveConstant.LONGEVITY_MONITOR_EVENT_SLEEP,
                                LongAliveConstant.LONGEVITY_MONITOR_EVENT_SLEEP_TYPE_KILL,
                                periodMillis, currentTimestamp, LongAliveMonitor.sLastLiveTimeStamp);
                    } else {
                        LongAliveMonitor.sLogger.log(
                                LongAliveConstant.LONGEVITY_MONITOR_EVENT_USER_KILL + periodMillis);
                    }
                }

                if (periodMillis > WATCH_DOG_INTERVAL_UP_LIMIT) {
                    LongAliveMonitor.sLogger.log("interval exceed limit " + periodMillis);
                }

                Editor editor = LongAliveMonitor.sSP.edit();
                editor.putLong(LongAliveConstant.LONGEVITY_MONITOR_PARAM_FIELD_TS, currentTimestamp);
                editor.putInt(LongAliveConstant.LONGEVITY_MONITOR_KEY_PID, currentPid);
                editor.putInt(LongAliveConstant.LONGEVITY_MONITOR_KEY_SCREEN_STATE, LongAliveMonitor.sCurrentScreenState);
                editor.apply();
                LongAliveMonitor.sLastLiveTimeStamp = currentTimestamp;
                LongAliveMonitor.sLastPid = currentPid;
                LongAliveMonitor.sLastScreenState = LongAliveMonitor.sCurrentScreenState;
                LongAliveMonitor.sHandler.postDelayed(this, WATCH_DOG_TIMER_INTERVAL);
                LongAliveMonitor.sLogger.log("postDelayed-----sWatchDogRunnable");
            } else {
                LongAliveMonitor.sLogger.log("boolean isOpen() set false");
            }
        }
    };

    public LongAliveMonitor() {
    }

    public static void init(LongAliveMonitorConfig config) {
        sApplication = config.getApplication();
        sToggle = config.getToggle();
        sEventTrack = config.getEventTrack();
        sLogger = config.getLogger();
        LongAliveScreenReceiver.register(sApplication);
    }

    public static void onActivityCreate(Bundle savedInstanceState) {
        if (sToggle.isOpen()) {
            sLogger.log("savedInstanceState is null ?\t" + (savedInstanceState == null));
            sSavedInstanceState = savedInstanceState;
            sSP = sApplication.getSharedPreferences(LongAliveConstant.LONGEVITY_MONITOR_NAME, 0);
            sLastLiveTimeStamp = sSP.getLong(LongAliveConstant.LONGEVITY_MONITOR_PARAM_FIELD_TS, TIMESTAMP_NO_VALUE);
            sLastPid = sSP.getInt(LongAliveConstant.LONGEVITY_MONITOR_KEY_PID, PID_NO_VALUE);
            sLastScreenState = sSP.getInt(LongAliveConstant.LONGEVITY_MONITOR_KEY_SCREEN_STATE, LongAliveConstant.LONGEVITY_SCREEN_STATE_NO_VALUE);
            if (sLastLiveTimeStamp == TIMESTAMP_NO_VALUE || sLastPid == PID_NO_VALUE) {
                sLastLiveTimeStamp = System.currentTimeMillis();
                sLastPid = getCurrentPid();
            }

            restartHandler();
        }
    }

    private static void restartHandler() {
        sHandler.removeCallbacks(sWatchDogRunnable);
        sHandler.postDelayed(sWatchDogRunnable, INIT_DELAY_TIME_OFFSET);
        sLogger.log("restartHandler-----"+INIT_DELAY_TIME_OFFSET);
    }

    private static int getCurrentPid() {
        return Process.myPid();
    }

    private static void report(String eventName, String eventType, long periodMillis
            , long currentTimestamp, long lastTimestamp) {
        HashMap<String, String> params = new HashMap<>();
        params.put(LongAliveConstant.LONGEVITY_MONITOR_PARAM_FIELD_EVENT, eventName);
        params.put(LongAliveConstant.LONGEVITY_MONITOR_PARAM_FIELD_TYPE, TextUtils.isEmpty(eventType) ? "" : eventType);
        params.put(LongAliveConstant.LONGEVITY_MONITOR_PARAM_FIELD_PERIOD, String.valueOf(periodMillis));
        params.put(LongAliveConstant.LONGEVITY_MONITOR_PARAM_FIELD_TS, String.valueOf(currentTimestamp));
        params.put(LongAliveConstant.LONGEVITY_MONITOR_PARAM_FIELD_TS_LAST, String.valueOf(lastTimestamp));
        sEventTrack.onEvent(params);
        sLogger.log(params.toString());
    }
}
