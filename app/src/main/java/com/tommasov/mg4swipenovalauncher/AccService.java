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
    // Broadcast emitted when a window of another app comes to the foreground.
    // SwipeService uses it to dismiss the "opening" loader exactly when the target
    // app actually becomes visible, instead of after a fixed timer.
    public static final String ACTION_FOREGROUND_PACKAGE = "com.tommasov.mg4swipenovalauncher.ACTION_FOREGROUND_PACKAGE";
    public static final String EXTRA_PACKAGE = "package";

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
        // Fired the moment a window becomes foreground. Forward which package owns it
        // so SwipeService can hide the loader as soon as the launched app is on screen.
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && event.getPackageName() != null) {
            Intent intent = new Intent(ACTION_FOREGROUND_PACKAGE);
            intent.setPackage(getPackageName());
            intent.putExtra(EXTRA_PACKAGE, event.getPackageName().toString());
            sendBroadcast(intent);
        }
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
