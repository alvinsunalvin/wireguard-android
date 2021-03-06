/*
 * Copyright © 2017-2019 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.util;

import android.content.Context;
import android.view.ContextThemeWrapper;

import com.wireguard.android.activity.SettingsActivity;
import com.wireguard.util.NonNullForAll;

import androidx.preference.Preference;

@NonNullForAll
public final class FragmentUtils {
    private FragmentUtils() {
        // Prevent instantiation
    }

    public static SettingsActivity getPrefActivity(final Preference preference) {
        final Context context = preference.getContext();
        if (context instanceof ContextThemeWrapper) {
            if (context instanceof SettingsActivity) {
                return ((SettingsActivity) context);
            }
        }
        return null;
    }
}
