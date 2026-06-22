package com.tommasov.mg4swipenovalauncher;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private PackageManager packageManager;
    private AppListAdapter adapter;
    private List<ApplicationInfo> allApps;
    private PreferencesManager preferencesManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        preferencesManager = new PreferencesManager(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView explanationText = findViewById(R.id.explanation_text);
        explanationText.setText(R.string.explanation_text);

        String currentPackageName = getPackageName();
        packageManager = getPackageManager();
        List<ApplicationInfo> userApps = new ArrayList<>();

        for (ApplicationInfo appInfo : packageManager.getInstalledApplications(PackageManager.GET_META_DATA)) {
            if (!appInfo.packageName.equals(currentPackageName)) {
                userApps.add(appInfo);
            }
        }

        Collections.sort(userApps, (app1, app2) -> {
            String label1 = app1.loadLabel(packageManager).toString();
            String label2 = app2.loadLabel(packageManager).toString();
            return label1.compareToIgnoreCase(label2);
        });

        String selectedPackage = preferencesManager.getSelectedPackage();
        if (selectedPackage == null) {
            for (ApplicationInfo appInfo : userApps) {
                if (appInfo.packageName.equals("com.teslacoilsw.launcher")) {
                    selectedPackage = appInfo.packageName;
                    preferencesManager.saveSelectedPackage(selectedPackage);
                    break;
                }
            }
        }

        ListView listView = findViewById(R.id.app_list);
        adapter = new AppListAdapter(this, userApps, selectedPackage);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            ApplicationInfo selectedApp = userApps.get(position);
            preferencesManager.saveSelectedPackage(selectedApp.packageName);
            adapter.setSelectedPackage(selectedApp.packageName);
            adapter.notifyDataSetChanged();
            Toast.makeText(MainActivity.this, getString(R.string.selected_app, selectedApp.packageName), Toast.LENGTH_SHORT).show();
        });

        Button toggleSystemAppsButton = findViewById(R.id.toggle_system_apps_button);
        toggleSystemAppsButton.setOnClickListener(v -> {
            adapter.toggleSystemAppsVisibility();
            toggleSystemAppsButton.setText(adapter.isSystemAppsVisible() ? getString(R.string.hide_system_apps) : getString(R.string.show_system_apps));
        });

        Switch switchBackButton = findViewById(R.id.switch_back_button);

        // The switch label reads "Hide the back button", so checked == hidden.
        switchBackButton.setChecked(!preferencesManager.isBackButtonVisible());

        switchBackButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setBackButtonVisible(!isChecked);
            stopSwipeService();
            startSwipeService();
        });

        startSwipeService();
    }

    private void stopSwipeService() {
        Intent intent = new Intent(this, SwipeService.class);
        stopService(intent);
    }

    private void startSwipeService() {
        Intent intent = new Intent(this, SwipeService.class);
        startService(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
