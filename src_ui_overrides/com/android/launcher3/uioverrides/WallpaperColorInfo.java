/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.uioverrides;

import static android.app.WallpaperManager.FLAG_SYSTEM;

import android.content.Context;
import android.graphics.Color;
import android.util.Pair;

import com.android.launcher3.util.Themes;
import com.android.launcher3.uioverrides.dynamicui.ColorExtractionAlgorithm;
import com.android.launcher3.uioverrides.dynamicui.WallpaperColorsCompat;
import com.android.launcher3.uioverrides.dynamicui.WallpaperManagerCompat;
import com.sprd.ext.LogUtils;

import java.util.ArrayList;

import androidx.annotation.NonNull;

public class WallpaperColorInfo implements WallpaperManagerCompat.OnColorsChangedListenerCompat {
    private static final String TAG = "WallpaperColorInfo";

    private static final int MAIN_COLOR_LIGHT = 0xffdadce0;
    private static final int MAIN_COLOR_DARK = 0xff202124;
    private static final int MAIN_COLOR_REGULAR = 0xff000000;

    private static final int FALLBACK_COLOR = Color.WHITE;
    private static final Object sInstanceLock = new Object();
    private static WallpaperColorInfo sInstance;

    public static WallpaperColorInfo getInstance(Context context) {
        synchronized (sInstanceLock) {
            if (sInstance == null) {
                sInstance = new WallpaperColorInfo(context.getApplicationContext());
            }
            return sInstance;
        }
    }

    private final Context mAppContext;
    private final ArrayList<OnChangeListener> mListeners = new ArrayList<>();
    private final WallpaperManagerCompat mWallpaperManager;
    private final ColorExtractionAlgorithm mExtractionType;
    private int mMainColor;
    private int mSecondaryColor;
    private boolean mIsDark;
    private boolean mSupportsDarkText;
    private boolean mIsDarkTheme;

    private OnChangeListener[] mTempListeners;

    private WallpaperColorInfo(Context context) {
        mAppContext = context;
        mWallpaperManager = WallpaperManagerCompat.getInstance(context);
        mWallpaperManager.addOnColorsChangedListener(this);
        mExtractionType = new ColorExtractionAlgorithm();
        update(mWallpaperManager.getWallpaperColors(FLAG_SYSTEM));
    }

    public int getMainColor() {
        return mMainColor;
    }

    public int getSecondaryColor() {
        return mSecondaryColor;
    }

    public boolean isDark() {
        return mIsDark;
    }

    public boolean supportsDarkText() {
        return mSupportsDarkText;
    }

    public boolean isMainColorDark() {
        return mMainColor == MAIN_COLOR_DARK;
    }

    @Override
    public void onColorsChanged(WallpaperColorsCompat colors, int which) {
        if (LogUtils.DEBUG_EXTERNAL_MSG) {
            LogUtils.d(TAG, "onColorsChanged, which:" + which + " colors:" + colors);
        }
        if ((which & FLAG_SYSTEM) != 0) {
            update(colors);
            notifyChange();
        }
    }

    public void updateIfNeeded() {
        if (mIsDarkTheme != Themes.isDarkTheme(mAppContext, this)) {
            update(mWallpaperManager.getWallpaperColors(FLAG_SYSTEM));
        }
    }

    private void update(WallpaperColorsCompat wallpaperColors) {
        Pair<Integer, Integer> colors = mExtractionType.extractInto(wallpaperColors);
        if (colors != null) {
            mMainColor = colors.first;
            mSecondaryColor = colors.second;
        } else {
            mMainColor = FALLBACK_COLOR;
            mSecondaryColor = FALLBACK_COLOR;
        }
        mSupportsDarkText = wallpaperColors != null && (wallpaperColors.getColorHints()
                & WallpaperColorsCompat.HINT_SUPPORTS_DARK_TEXT) > 0;
        mIsDark = wallpaperColors != null && (wallpaperColors.getColorHints()
                & WallpaperColorsCompat.HINT_SUPPORTS_DARK_THEME) > 0;

        mIsDarkTheme = Themes.isDarkTheme(mAppContext, this);

        if (LogUtils.DEBUG_EXTERNAL_MSG) {
            LogUtils.d(TAG, "update done, " + this);
        }
    }

    public void addOnChangeListener(OnChangeListener listener) {
        mListeners.add(listener);
    }

    public void removeOnChangeListener(OnChangeListener listener) {
        mListeners.remove(listener);
    }

    private void notifyChange() {
        OnChangeListener[] copy =
                mTempListeners != null && mTempListeners.length == mListeners.size() ?
                        mTempListeners : new OnChangeListener[mListeners.size()];

        // Create a new array to avoid concurrent modification when the activity destroys itself.
        mTempListeners = mListeners.toArray(copy);
        for (OnChangeListener listener : mTempListeners) {
            listener.onExtractedColorsChanged(this);
        }
    }

    public interface OnChangeListener {
        void onExtractedColorsChanged(WallpaperColorInfo wallpaperColorInfo);
    }

    @NonNull
    @Override
    public String toString() {
        return "MainColor:" + Integer.toHexString(getMainColor()) +
                " SecondaryColor:" + Integer.toHexString(getSecondaryColor()) +
                " isDark:" + isDark() +
                " supportsDarkText:" + supportsDarkText() +
                " isMainColorDark:" + isMainColorDark();
    }
}