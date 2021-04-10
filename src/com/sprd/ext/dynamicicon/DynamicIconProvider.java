package com.sprd.ext.dynamicicon;

import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.graphics.drawable.Drawable;

import com.android.launcher3.IconProvider;
import com.sprd.ext.LauncherAppMonitor;

public class DynamicIconProvider extends IconProvider {

    private DynamicIconController mDIController;

    public DynamicIconProvider(Context context) {
        super();

        if (DynamicIconUtils.anyDynamicIconSupport()) {
            mDIController = LauncherAppMonitor.getInstance(context)
                    .getDynamicIconController();
        }
    }

    @Override
    public String getSystemStateForPackage(String systemState, String packageName) {
        return mDIController == null ?
                super.getSystemStateForPackage(systemState, packageName)
                : mDIController.getSystemStateForPackage(systemState, packageName);
    }

    @Override
    public Drawable getIcon(LauncherActivityInfo info, int iconDpi, boolean flattenDrawable) {
        Drawable icon = mDIController == null ?
                null : mDIController.getIcon(info, iconDpi, flattenDrawable);

        if (icon == null) {
            icon = super.getIcon(info, iconDpi, flattenDrawable);
        }
        return icon;
    }
}
