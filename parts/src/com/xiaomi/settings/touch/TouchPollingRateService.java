/*
 * Copyright (C) 2023 Paranoid Android
 * Copyright (C) 2025 Neoteric OS
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.touch;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

public class TouchPollingRateService extends Service {

    private static final String TAG = "XiaomiPartsTouchPollingRateService";
    private static final boolean DEBUG = true;

    private static final String SECURE_KEY_POLLING = "touch_polling_enabled";

    private boolean mIsPollingEnabled;

    private ContentResolver mContentResolver;
    private ScreenStateReceiver mScreenStateReceiver;
    private SettingsObserver mSettingsObserver;
    private final Handler mHandler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) Log.d(TAG, "Creating service");
        mContentResolver = getContentResolver();
        mScreenStateReceiver = new ScreenStateReceiver();
        mSettingsObserver = new SettingsObserver(mHandler);
        mSettingsObserver.update();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {
        if (DEBUG) Log.d(TAG, "Starting service");
        mScreenStateReceiver.register();
        mSettingsObserver.register();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.d(TAG, "Destroying service");
        unregisterReceiver(mScreenStateReceiver);
        mContentResolver.unregisterContentObserver(mSettingsObserver);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final class ScreenStateReceiver extends BroadcastReceiver {
        public void register() {
            if (DEBUG) Log.d(TAG, "ScreenStateReceiver: register");
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_USER_PRESENT);
            registerReceiver(this, filter);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Intent.ACTION_SCREEN_ON:
                    if (DEBUG) Log.d(TAG, "Received ACTION_SCREEN_ON");
                    mSettingsObserver.update();
                    break;
                case Intent.ACTION_USER_PRESENT:
                    if (DEBUG) Log.d(TAG, "Received ACTION_USER_PRESENT");
                    mSettingsObserver.update();
                    break;
            }
        }
    }

    private final class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        public void register() {
            if (DEBUG) Log.d(TAG, "SettingsObserver: register");
            mContentResolver.registerContentObserver(
                    Settings.Secure.getUriFor(SECURE_KEY_POLLING), false, this);
        }

        void update() {
            mIsPollingEnabled = Settings.Secure.getInt(mContentResolver, SECURE_KEY_POLLING, 0) != 0;
            if (DEBUG) Log.d(TAG, "SettingsObserver: SECURE_KEY_POLLING: " + mIsPollingEnabled);
            TfWrapper.setTouchFeature(
                    new TfWrapper.TfParams(/*TOUCH_GAME_MODE*/ 0, mIsPollingEnabled ? 1 : 0));
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (DEBUG) Log.d(TAG, "SettingsObserver: onChange: " + uri.toString());
            if (uri.equals(Settings.Secure.getUriFor(SECURE_KEY_POLLING))) {
                update();
            }
        }
    }
}
