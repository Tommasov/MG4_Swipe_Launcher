package com.tommasov.mg4swipenovalauncher;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.accessibility.AccessibilityEvent;

import androidx.core.content.ContextCompat;

public class AccService extends AccessibilityService {

    public static final String ACTION_BACK = "com.tommasov.mg4swipenovalauncher.ACTION_BACK";

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_BACK);
        ContextCompat.registerReceiver(this, backReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    private final BroadcastReceiver backReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            performBackAction();
        }
    };

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    public void performBackAction() {
        performGlobalAction(GLOBAL_ACTION_BACK);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(backReceiver);
        super.onDestroy();
    }
}
