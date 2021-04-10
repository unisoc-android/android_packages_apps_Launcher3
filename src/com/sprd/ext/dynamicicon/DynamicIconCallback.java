package com.sprd.ext.dynamicicon;

import android.content.pm.LauncherActivityInfo;
import android.graphics.drawable.Drawable;

public interface DynamicIconCallback {

    String getPkgName();

    Drawable getIcon(LauncherActivityInfo info, int iconDpi, boolean flattenDrawable);
    String getSystemState();

    void registerReceiver();
    void unRegisterReceiver();

    boolean isOpened();
    void onStateChanged(boolean dynamic);
    void onStart();

    void removeDynamicIconFromDb();
}
