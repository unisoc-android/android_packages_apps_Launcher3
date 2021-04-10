package com.sprd.ext.customizeappsort;

import android.content.ComponentName;
import android.content.Context;
import android.os.Process;
import android.util.Pair;
import android.util.SparseArray;

import com.android.launcher3.AppInfo;
import com.android.launcher3.R;
import com.android.launcher3.config.FeatureFlags;
import com.sprd.ext.BaseController;
import com.sprd.ext.LauncherAppMonitor;
import com.sprd.ext.LauncherAppMonitorCallback;
import com.sprd.ext.LogUtils;
import com.sprd.ext.multimode.MultiModeController;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import androidx.annotation.VisibleForTesting;

/**
 * Created by SPRD on 2017/2/20.
 */
public class CustomizeAppSortController extends BaseController {
    private static final String TAG = "CustomizeAppSortController";
    private static final String POSITION_SEPARATOR = "#";
    private static final String CLASS_PACKAGE_SEPARATOR = "/";

    private String[] mConfigArray;
    private SparseArray<Pair<String, String>> mCustomizePositions;
    private boolean mHasCustomizeAppData;
    private final LauncherAppMonitor mMonitor;

    private final LauncherAppMonitorCallback mAppMonitorCallback = new LauncherAppMonitorCallback() {

        @Override
        public void onAllAppsListUpdated(List<AppInfo> apps) {
            if (!MultiModeController.isSingleLayerMode()) {
                sortApps(apps);
            }
        }

        @Override
        public void dump(String prefix, FileDescriptor fd, PrintWriter w, boolean dumpAll) {
            w.println();
            w.println(prefix + TAG + ": size:" + mCustomizePositions.size() +
                    " mCustomizePositions:" + mCustomizePositions);
        }
    };

    public CustomizeAppSortController(final Context appContext, LauncherAppMonitor monitor) {
        super(appContext);
        mConfigArray = appContext.getResources().getStringArray(R.array.customize_app_position);
        mMonitor = monitor;
        initData();
    }

    /** Set the config array. This should only be used in tests. */
    @VisibleForTesting
    void setConfigArray(ArrayList<String> array) {
        mConfigArray = array.toArray(new String[array.size()]);
        initData();
    }

    private void initData() {
        mCustomizePositions = loadCustomizeAppPos(mConfigArray);
        mHasCustomizeAppData = mCustomizePositions.size() > 0;
        if (mHasCustomizeAppData) {
            mMonitor.registerCallback(mAppMonitorCallback);
        }
        LogUtils.d(TAG, "load config done:" + mCustomizePositions);
    }

    private SparseArray<Pair<String,String>> loadCustomizeAppPos(String[] array) {
        SparseArray<Pair<String, String>> customizePositions = new SparseArray<>();
        if ((array == null || array.length == 0) && FeatureFlags.ENABLE_DEBUG) {
            // only for developer, setting app is first, clock app is third, camera app is last.
            ArrayList<String> configApps = new ArrayList<>();
            configApps.add("com.android.settings/#0");
            configApps.add("com.android.deskclock/#2");
            configApps.add("com.android.camera2/#1024");
            array = new String[configApps.size()];
            for (int i = 0; i < array.length; i++) {
                array[i] = configApps.get(i);
            }
        }

        for (String s : array != null ? array : new String[0]) {
            String pkgName, clsName;
            int position;
            // separate class&package name, position
            String[] separteByPos = s.split(POSITION_SEPARATOR, 2);
            if (separteByPos.length < 2) {
                LogUtils.w(TAG, "customize app info must contains '#', string is : " + s);
                continue;
            }
            String pkgClsName = separteByPos[0];
            try {
                position = Integer.parseInt(separteByPos[1]);
            } catch (NumberFormatException e) {
                LogUtils.w(TAG, "position must be a number : " + s);
                continue;
            }

            // separate class name, package name
            String[] separteByClsPkg = pkgClsName.split(CLASS_PACKAGE_SEPARATOR, 2);
            if (separteByClsPkg.length < 2) {
                pkgName = pkgClsName;
                clsName = null;
            } else {
                pkgName = separteByClsPkg[0];
                clsName = separteByClsPkg[1].isEmpty() ? null : separteByClsPkg[1];
            }
            if (pkgName != null && pkgName.length() != 0) {
                customizePositions.put(position, new Pair<>(pkgName, clsName));
            }
        }
        return customizePositions;
    }

    void sortApps(final List<AppInfo> apps) {
        if (!mHasCustomizeAppData) {
            return;
        }
        ArrayList<AppInfo> sortApps = new ArrayList<>(apps);
        onSortApps(sortApps);

        // refresh mApps
        apps.clear();
        apps.addAll(sortApps);
    }

    private void onSortApps(final ArrayList<AppInfo> originalApps) {
        TreeMap<Integer, AppInfo> sortedMaps = new TreeMap<>();
        ArrayList<AppInfo> cloneAppList = new ArrayList<>();

        // find the customize component in componentNames
        Pair<String, String> pair;
        for (AppInfo app : originalApps) {
            for (int i = 0; i < mCustomizePositions.size(); i++) {
                ComponentName cn = app.componentName;
                pair = mCustomizePositions.valueAt(i);
                if (pair.first.equals(cn.getPackageName() )) {
                    if (pair.second == null || pair.second.equals(cn.getClassName())) {
                        if (app.user.equals(Process.myUserHandle())) {
                            sortedMaps.put(mCustomizePositions.keyAt(i), app);
                        } else {
                            cloneAppList.add(app);
                        }
                    }
                }

            }
        }

        // insert clone app
        if (!cloneAppList.isEmpty()) {
            LogUtils.d(TAG, "onSortApps, cloneAppList.size():" + cloneAppList.size());
            for (AppInfo app : cloneAppList) {
                insertCloneApp(app, sortedMaps);
            }
        }

        LogUtils.d(TAG, "onSortApps, sortedMaps keys:" + sortedMaps.keySet().toString());
        if (LogUtils.DEBUG_ALL) {
            LogUtils.d(TAG, "onSortApps, sortedMaps:" + maps2String(sortedMaps));
            LogUtils.d(TAG, "onSortApps, need sort apps:" + apps2String(originalApps));
        }

        // remove the found component
        for (Map.Entry<Integer, AppInfo> integerAppInfoEntry : sortedMaps.entrySet()) {
            originalApps.remove(integerAppInfoEntry.getValue());
        }

        // insert at the customize position
        for (Map.Entry<Integer, AppInfo> integerAppInfoEntry : sortedMaps.entrySet()) {
            if (integerAppInfoEntry.getKey() > originalApps.size()) {
                // append to last position
                originalApps.add(integerAppInfoEntry.getValue());
            } else {
                // insert at specific position
                originalApps.add(integerAppInfoEntry.getKey(), integerAppInfoEntry.getValue());
            }
        }
        if (LogUtils.DEBUG_ALL) {
            LogUtils.d(TAG, "onSortApps, sorted apps:" + apps2String(originalApps));
        }
    }

    private void insertCloneApp(AppInfo cloneApp, TreeMap<Integer, AppInfo> sortedAppMaps) {
        if (sortedAppMaps.containsValue(cloneApp)) {
            return;
        }

        int findKey = -1;
        for (Map.Entry<Integer, AppInfo> integerAppInfoEntry : sortedAppMaps.entrySet()) {
            AppInfo me = integerAppInfoEntry.getValue();
            int key = integerAppInfoEntry.getKey();
            if (cloneApp.componentName.equals(me.componentName)) {
                findKey = key;
                LogUtils.d(TAG, "insertCloneApp, find owner app:[" + findKey + "] " + me.componentName);
                break;
            }
        }

        if (findKey != -1) {
            int lastKey = sortedAppMaps.lastKey();
            for (int i = findKey + 1; i <= lastKey + 1; i++) {
                if (sortedAppMaps.get(i) == null) {
                    LogUtils.d(TAG, "insertCloneApp, find empty grid:[" + i + "] ");
                    sortedAppMaps.put(i, cloneApp);
                    return;
                }
            }
        }
    }

    private String apps2String(final List<AppInfo> apps) {
        TreeMap<Integer, AppInfo> maps = new TreeMap<>();
        for (int i = 0; i < apps.size(); i++) {
            maps.put(i, apps.get(i));
        }
        return maps2String(maps);
    }

    private String maps2String(TreeMap<Integer, AppInfo> maps) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, AppInfo> infoEntry : maps.entrySet()) {
            AppInfo app = infoEntry.getValue();
            sb.append("\n[ ")
                    .append(infoEntry.getKey())
                    .append(" -> ").append(app.toComponentKey().toString())
                    .append(" ]");
        }
        return sb.toString();
    }

}
