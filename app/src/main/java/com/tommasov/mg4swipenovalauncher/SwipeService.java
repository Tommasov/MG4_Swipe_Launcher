package com.tommasov.mg4swipenovalauncher;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

public class SwipeService extends Service {
    private PreferencesManager preferencesManager;
    private static final String CHANNEL_ID = "SwipeServiceChannel";
    private static final String DEFAULT_LAUNCHER_PACKAGE = "com.teslacoilsw.launcher";
    // Height (px) of the invisible touch strips at the bottom edge.
    private static final int SWIPE_AREA_HEIGHT = 10;
    // Initial position (px) of the floating back button.
    private static final int FLOATING_BUTTON_X = 25;
    private static final int FLOATING_BUTTON_Y = 5;
    // Safety cap (ms) for the "opening" loader. The loader is normally dismissed the
    // instant the target app's window becomes foreground (see foregroundReceiver); this
    // timer only fires if that event never arrives, so the overlay can never get stuck.
    private static final long LOADER_TIMEOUT_MS = 4000;
    private WindowManager windowManager;
    private View leftSwipeArea;
    private View rightSwipeArea;
    private View floatingButton;
    private View loaderView;
    private GestureDetector leftGestureDetector;
    private GestureDetector rightGestureDetector;
    private final Handler loaderHandler = new Handler(Looper.getMainLooper());
    private Runnable loaderRemoveRunnable;
    // Package whose appearance on screen should dismiss the loader. Set when the loader
    // is shown, cleared once dismissed. null means "not currently waiting".
    private String pendingLoaderPackage;

    @SuppressLint("ForegroundServiceType")
    @Override
    public void onCreate() {
        super.onCreate();

        if (Settings.canDrawOverlays(this)) {
            preferencesManager = new PreferencesManager(this);
            IntentFilter foregroundFilter = new IntentFilter(AccService.ACTION_FOREGROUND_PACKAGE);
            ContextCompat.registerReceiver(this, foregroundReceiver, foregroundFilter,
                    ContextCompat.RECEIVER_NOT_EXPORTED);
            createNotificationChannel();
            Notification notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("MG4 Nova Launcher Swipe Service")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .build();
            startForeground(1, notification);

            swipe();
            backButton();

        } else {
            stopSelf();
        }
    }

    private static class SwipeUpGestureListener extends SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        private final Runnable action;

        SwipeUpGestureListener(Runnable action) {
            this.action = action;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1 == null) {
                return false;
            }
            float diffY = e2.getRawY() - e1.getRawY();

            if (diffY < 0 && Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                action.run();
                return true;
            }

            return false;
        }
    }

    // Dismisses the loader as soon as the launched target app reports its window as
    // foreground, so the overlay stays up exactly as long as the (variable) open takes.
    private final BroadcastReceiver foregroundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String pkg = intent.getStringExtra(AccService.EXTRA_PACKAGE);
            if (pendingLoaderPackage != null && pendingLoaderPackage.equals(pkg)) {
                hideLoader();
            }
        }
    };

    private void performBackAction() {
        Intent intent = new Intent(AccService.ACTION_BACK);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    private void openLauncher() {
        String packageName = preferencesManager.getSelectedPackage();

        if (packageName == null) {
            packageName = DEFAULT_LAUNCHER_PACKAGE;
        }

        // getLaunchIntentForPackage() returns NEW_TASK | RESET_TASK_IF_NEEDED by default.
        // RESET_TASK_IF_NEEDED resets the task when it is brought back from the background,
        // which can add extra work when re-foregrounding the (home) launcher and was a
        // suspect for the 2-3s delay on the second swipe. We instead reuse the existing
        // instance with NEW_TASK | SINGLE_TOP, so no task reset is forced.
        Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null) {
            if (preferencesManager.isShowLoader()) {
                showLoader();
                // Arm the foreground watcher: the loader is dismissed when this package
                // becomes visible. Set after showLoader(), which clears any prior state.
                pendingLoaderPackage = packageName;
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        } else {
            Toast.makeText(this, R.string.package_not_found, Toast.LENGTH_SHORT).show();
        }
    }

    // Shows a lightweight, non-interactive overlay (spinner + label) over the home
    // transition to mask the brief icon flash and make the variable re-open read as an
    // intentional "loading" rather than a glitch. It is dismissed event-driven, when the
    // target app's window becomes foreground (foregroundReceiver); LOADER_TIMEOUT_MS is
    // only a safety cap so the overlay can never get stuck if that event is missed.
    private void showLoader() {
        if (windowManager == null) {
            return;
        }
        // Drop any loader still on screen from a previous rapid swipe.
        hideLoader();

        loaderView = LayoutInflater.from(this).inflate(R.layout.layout_loader, null);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER;

        windowManager.addView(loaderView, params);

        loaderRemoveRunnable = this::hideLoader;
        loaderHandler.postDelayed(loaderRemoveRunnable, LOADER_TIMEOUT_MS);
    }

    private void hideLoader() {
        pendingLoaderPackage = null;
        if (loaderRemoveRunnable != null) {
            loaderHandler.removeCallbacks(loaderRemoveRunnable);
            loaderRemoveRunnable = null;
        }
        if (loaderView != null) {
            removeViewSafely(loaderView);
            loaderView = null;
        }
    }

    private void swipe() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        leftSwipeArea = new View(this);
        rightSwipeArea = new View(this);

        int layoutFlags;
        layoutFlags = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenWidth = size.x;

        int halfScreenWidth = screenWidth / 2;

        WindowManager.LayoutParams leftParams = new WindowManager.LayoutParams(
                halfScreenWidth,
                SWIPE_AREA_HEIGHT,
                layoutFlags,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        leftParams.gravity = Gravity.BOTTOM | Gravity.LEFT;

        WindowManager.LayoutParams rightParams = new WindowManager.LayoutParams(
                halfScreenWidth,
                SWIPE_AREA_HEIGHT,
                layoutFlags,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        rightParams.gravity = Gravity.BOTTOM | Gravity.RIGHT;

        windowManager.addView(leftSwipeArea, leftParams);
        windowManager.addView(rightSwipeArea, rightParams);

        // By default the left area goes back and the right area opens the launcher.
        // When the areas are swapped, the actions are exchanged.
        Runnable leftAction = this::performBackAction;
        Runnable rightAction = this::openLauncher;
        if (preferencesManager.isSwipeAreasSwapped()) {
            Runnable tmp = leftAction;
            leftAction = rightAction;
            rightAction = tmp;
        }

        leftGestureDetector = new GestureDetector(this, new SwipeUpGestureListener(leftAction));
        rightGestureDetector = new GestureDetector(this, new SwipeUpGestureListener(rightAction));

        leftSwipeArea.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return leftGestureDetector.onTouchEvent(event);
            }
        });

        rightSwipeArea.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return rightGestureDetector.onTouchEvent(event);
            }
        });
    }

    private void backButton() {
        if (!preferencesManager.isBackButtonVisible()) {
            return;
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        floatingButton = LayoutInflater.from(this).inflate(R.layout.layout_floating_button, null);

        int layoutFlags;
        layoutFlags = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlags,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = FLOATING_BUTTON_X;
        params.y = FLOATING_BUTTON_Y;

        windowManager.addView(floatingButton, params);

        floatingButton.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private static final int CLICK_ACTION_THRESHOLD = 10;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        floatingButton.setPressed(true);
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        int deltaX = (int) (event.getRawX() - initialTouchX);
                        int deltaY = (int) (event.getRawY() - initialTouchY);
                        params.x = initialX + deltaX;
                        params.y = initialY + deltaY;
                        windowManager.updateViewLayout(floatingButton, params);
                        return true;

                    case MotionEvent.ACTION_UP:
                        floatingButton.setPressed(false);

                        if (Math.abs(event.getRawX() - initialTouchX) <= CLICK_ACTION_THRESHOLD &&
                                Math.abs(event.getRawY() - initialTouchY) <= CLICK_ACTION_THRESHOLD) {
                            floatingButton.performClick();
                        }

                        return true;

                    case MotionEvent.ACTION_CANCEL:
                        floatingButton.setPressed(false);
                        return true;
                }
                return false;
            }
        });
        floatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performBackAction();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(foregroundReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver was never registered (overlay permission missing at onCreate).
        }
        hideLoader();
        removeViewSafely(leftSwipeArea);
        removeViewSafely(rightSwipeArea);
        removeViewSafely(floatingButton);
    }

    private void removeViewSafely(View view) {
        if (view == null || windowManager == null) {
            return;
        }
        try {
            windowManager.removeView(view);
        } catch (IllegalArgumentException e) {
            // View was never attached (e.g. permission revoked mid-lifecycle); nothing to remove.
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Swipe Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
