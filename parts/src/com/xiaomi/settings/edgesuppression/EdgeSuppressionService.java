/*
 * Copyright (C) 2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.edgesuppression;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

public class EdgeSuppressionService extends Service {

    private static final String TAG = "XiaomiPartsEdgeSuppressionService";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private EdgeSuppressionManager mEdgeSuppressionManager;

    @Override
    public void onCreate() {
        if (Build.SKU.equals("aurora")) {
            if (DEBUG) Log.d(TAG, "Creating service");
            super.onCreate();

            // Initialize EdgeSuppressionManager
            try {
                mEdgeSuppressionManager = EdgeSuppressionManager.getInstance(getApplicationContext());
                if (mEdgeSuppressionManager == null) {
                    Log.e(TAG, "Failed to initialize EdgeSuppressionManager");
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception while initializing EdgeSuppressionManager", e);
                stopSelf(); // Stop the service if initialization fails
                return;
            }

            // Enable related settings activity
            getPackageManager().setComponentEnabledSetting(
                    new ComponentName(this, "com.xiaomi.settings.edgesuppression.EdgeSuppressionSettingsActivity"),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
            );
        } else {
            if (DEBUG) Log.d(TAG, "Stopping service, not supported on this device");

            // Disable related settings activity
            getPackageManager().setComponentEnabledSetting(
                    new ComponentName(this, "com.xiaomi.settings.edgesuppression.EdgeSuppressionSettingsActivity"),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
            );

            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) Log.d(TAG, "onStartCommand");

        if (mEdgeSuppressionManager != null) {
            try {
                mEdgeSuppressionManager.handleEdgeSuppressionChange();
            } catch (Exception e) {
                Log.e(TAG, "Error handling edge suppression change", e);
            }
        } else {
            Log.e(TAG, "EdgeSuppressionManager is null, skipping edge suppression change");
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (DEBUG) Log.d(TAG, "onConfigurationChanged");

        if (mEdgeSuppressionManager != null) {
            try {
                mEdgeSuppressionManager.handleEdgeSuppressionChange();
            } catch (Exception e) {
                Log.e(TAG, "Error handling configuration change", e);
            }
        } else {
            Log.e(TAG, "EdgeSuppressionManager is null, skipping configuration change handling");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
