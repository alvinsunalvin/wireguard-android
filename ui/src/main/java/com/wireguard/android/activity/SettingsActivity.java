/*
 * Copyright © 2017-2019 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.activity;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.MenuItem;

import com.wireguard.android.Application;
import com.wireguard.android.R;
import com.wireguard.android.backend.WgQuickBackend;
import com.wireguard.android.util.ModuleLoader;
import com.wireguard.util.NonNullForAll;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

/**
 * Interface for changing application-global persistent settings.
 */

@NonNullForAll
public class SettingsActivity extends ThemeChangeAwareActivity {
    private final SparseArray<PermissionRequestCallback> permissionRequestCallbacks = new SparseArray<>();
    private int permissionRequestCounter;

    public void ensurePermissions(final String[] permissions, final PermissionRequestCallback cb) {
        final List<String> needPermissions = new ArrayList<>(permissions.length);
        for (final String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED)
                needPermissions.add(permission);
        }
        if (needPermissions.isEmpty()) {
            final int[] granted = new int[permissions.length];
            Arrays.fill(granted, PackageManager.PERMISSION_GRANTED);
            cb.done(permissions, granted);
            return;
        }
        final int idx = permissionRequestCounter++;
        permissionRequestCallbacks.put(idx, cb);
        ActivityCompat.requestPermissions(this,
                needPermissions.toArray(new String[needPermissions.size()]), idx);
    }

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportFragmentManager().findFragmentById(android.R.id.content) == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(android.R.id.content, new SettingsFragment())
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           final String[] permissions,
                                           final int[] grantResults) {
        final PermissionRequestCallback f = permissionRequestCallbacks.get(requestCode);
        if (f != null) {
            permissionRequestCallbacks.remove(requestCode);
            f.done(permissions, grantResults);
        }
    }

    public interface PermissionRequestCallback {
        void done(String[] permissions, int[] grantResults);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(final Bundle savedInstanceState, final String key) {
            addPreferencesFromResource(R.xml.preferences);
            final PreferenceScreen screen = getPreferenceScreen();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                screen.removePreference(getPreferenceManager().findPreference("dark_theme"));

            final Preference wgQuickOnlyPrefs[] = {
                    getPreferenceManager().findPreference("tools_installer"),
                    getPreferenceManager().findPreference("restore_on_boot"),
                    getPreferenceManager().findPreference("multiple_tunnels")
            };
            for (final Preference pref : wgQuickOnlyPrefs)
                pref.setVisible(false);
            Application.getBackendAsync().thenAccept(backend -> {
                for (final Preference pref : wgQuickOnlyPrefs) {
                    if (backend instanceof WgQuickBackend)
                        pref.setVisible(true);
                    else
                        screen.removePreference(pref);
                }
            });

            final Preference moduleInstaller = getPreferenceManager().findPreference("module_downloader");
            final Preference kernelModuleDisabler = getPreferenceManager().findPreference("kernel_module_disabler");
            moduleInstaller.setVisible(false);
            if (ModuleLoader.isModuleLoaded()) {
                screen.removePreference(moduleInstaller);
            } else {
                screen.removePreference(kernelModuleDisabler);
                Application.getAsyncWorker().runAsync(Application.getRootShell()::start).whenComplete((v, e) -> {
                    if (e == null)
                        moduleInstaller.setVisible(true);
                    else
                        screen.removePreference(moduleInstaller);
                });
            }
        }
    }
}
