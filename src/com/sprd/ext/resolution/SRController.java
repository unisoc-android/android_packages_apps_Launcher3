package com.sprd.ext.resolution;

import android.content.ContentResolver;
import android.content.Context;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.WindowManagerGlobal;

import com.android.launcher3.AppWidgetResizeFrame;
import com.android.launcher3.util.ConfigMonitor;
import com.sprd.ext.BaseController;
import com.sprd.ext.LauncherAppMonitor;
import com.sprd.ext.LauncherAppMonitorCallback;
import com.sprd.ext.LogUtils;
import com.sprd.ext.SettingsObserver;

import java.util.ArrayList;
import java.util.List;

public class SRController extends BaseController {
    private static final String TAG = "SRController";

    private static final String ACTION_SR_CHANGING = "sprd.action.super_resolution_state";
    private static final String KEY_GRID_NAME_IN_STORAGE = "grid_name_in_storage";

    private static final int RESOLUTION_SWITCH_STATE_OFF = 0;
    private static final int RESOLUTION_SWITCH_STATE_ON = 1;
    private static final int SUPPORT_RESOLUTION_SWITCH_THRESHOLD = 2;

    private float mOldDensity = 0f;
    private float mNewDensity = 0f;

    private boolean mResolutionChanging = false;

    private ConfigMonitor mConfigMonitor;

    private String mStoredGridName;

    private LauncherAppMonitorCallback mAppMonitorCallback = new LauncherAppMonitorCallback() {
        @Override
        public void onBindingWorkspaceFinish() {
            mOldDensity = mNewDensity;
        }
    };

    public SRController(Context context, LauncherAppMonitor monitor) {
        super(context);
        monitor.registerCallback(mAppMonitorCallback);

        ContentResolver resolver = mContext.getContentResolver();
        SettingsObserver resolutionSwitchObserver = new SettingsObserver.System(resolver) {
            @Override
            public void onSettingChanged(boolean changed) {
                int isChanging = Settings.System.getInt(
                        mContext.getContentResolver(), ACTION_SR_CHANGING, RESOLUTION_SWITCH_STATE_OFF);
                LogUtils.d(TAG, "Resolution isChanging = " + isChanging);
                if (isChanging == RESOLUTION_SWITCH_STATE_ON) {
                    mResolutionChanging = true;
                    mOldDensity = mContext.getResources().getDisplayMetrics().density;
                } else {
                    if (mResolutionChanging) {
                        mResolutionChanging = false;
                        mNewDensity = mContext.getResources().getDisplayMetrics().density;
                        LogUtils.d(TAG, "Switch resolution, mOldDensity = " + mOldDensity
                                + " newDensity = " + mNewDensity);
                        if (mNewDensity != mOldDensity) {
                            AppWidgetResizeFrame.resetWidgetCellSize();

                            if (mConfigMonitor != null) {
                                mConfigMonitor.notifyChange();
                            }
                        }
                    }
                }
            }
        };
        resolutionSwitchObserver.register(ACTION_SR_CHANGING);
    }

    public void setConfigMonitor(ConfigMonitor configMonitor) {
        mConfigMonitor = configMonitor;
    }

    public boolean isResolutionSwitching() {
        return mResolutionChanging;
    }

    public boolean isDensityChanged() {
        return mNewDensity != mOldDensity;
    }

    public String getGridNameFromStorage(Context context) {
        if (TextUtils.isEmpty(mStoredGridName)) {
            mStoredGridName =
                    Settings.Global.getString(context.getContentResolver(), KEY_GRID_NAME_IN_STORAGE);
        }
        return mStoredGridName;
    }

    public void saveGridNameIntoStorage(Context context, String gridName) {
        if (TextUtils.isEmpty(getGridNameFromStorage(context))) {
            if (LogUtils.DEBUG) {
                LogUtils.d(TAG, "saveGridNameIntoStorage: gridName = " + gridName);
            }
            Settings.Global.putString(context.getContentResolver(), KEY_GRID_NAME_IN_STORAGE, gridName);
        }
    }

    public static boolean isSupportResolutionSwitch() {
        List<String[]> resolutionMode = new ArrayList<>();
        try {
            resolutionMode = WindowManagerGlobal.getWindowManagerService().getResolutions();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NoSuchMethodError e) {
            LogUtils.w(TAG, "getResolutions, NoSuchMethodError");
        }

        return resolutionMode.size() >= SUPPORT_RESOLUTION_SWITCH_THRESHOLD;
    }
}
