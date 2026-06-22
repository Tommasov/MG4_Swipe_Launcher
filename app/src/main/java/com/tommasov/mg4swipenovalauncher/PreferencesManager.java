package com.tommasov.mg4swipenovalauncher;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {
    private static final String PREFS_NAME = "SwipeServicePrefs";
    private static final String KEY_PACKAGE_NAME = "packageName";
    private static final String KEY_BACK_BUTTON_VISIBLE = "backButtonVisible";
    private static final String KEY_SWIPE_AREAS_SWAPPED = "swipeAreasSwapped";
    private static final String KEY_SHOW_LOADER = "showLoader";
    // Legacy string key ("VISIBLE"/"INVISIBLE") kept only for one-time migration.
    private static final String LEGACY_KEY_BACK_BUTTON_VISIBILITY = "backButtonVisibility";

    private SharedPreferences sharedPreferences;

    public PreferencesManager(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveSelectedPackage(String packageName) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_PACKAGE_NAME, packageName);
        editor.apply();
    }

    public String getSelectedPackage() {
        return sharedPreferences.getString(KEY_PACKAGE_NAME, null);
    }

    public void setBackButtonVisible(boolean visible) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_BACK_BUTTON_VISIBLE, visible);
        editor.apply();
    }

    public boolean isBackButtonVisible() {
        // Migrate the legacy string value ("VISIBLE"/"INVISIBLE") on first read.
        if (sharedPreferences.contains(LEGACY_KEY_BACK_BUTTON_VISIBILITY)) {
            boolean visible = "VISIBLE".equals(
                    sharedPreferences.getString(LEGACY_KEY_BACK_BUTTON_VISIBILITY, "INVISIBLE"));
            sharedPreferences.edit()
                    .remove(LEGACY_KEY_BACK_BUTTON_VISIBILITY)
                    .putBoolean(KEY_BACK_BUTTON_VISIBLE, visible)
                    .apply();
            return visible;
        }
        return sharedPreferences.getBoolean(KEY_BACK_BUTTON_VISIBLE, false);
    }

    public void setSwipeAreasSwapped(boolean swapped) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_SWIPE_AREAS_SWAPPED, swapped);
        editor.apply();
    }

    public boolean isSwipeAreasSwapped() {
        return sharedPreferences.getBoolean(KEY_SWIPE_AREAS_SWAPPED, false);
    }

    public void setShowLoader(boolean show) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_SHOW_LOADER, show);
        editor.apply();
    }

    public boolean isShowLoader() {
        return sharedPreferences.getBoolean(KEY_SHOW_LOADER, true);
    }
}
