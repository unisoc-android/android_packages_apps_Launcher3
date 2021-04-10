package com.sprd.ext.grid;

import static com.android.launcher3.InvariantDeviceProfile.KEY_IDP_GRID_NAME;
import static com.android.launcher3.config.FeatureFlags.APPLY_CONFIG_AT_RUNTIME;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.Utilities;
import com.android.launcher3.model.GridSizeMigrationTask;
import com.sprd.ext.BaseController;
import com.sprd.ext.LauncherAppMonitor;
import com.sprd.ext.LauncherAppMonitorCallback;
import com.sprd.ext.LauncherSettingsExtension;
import com.sprd.ext.LogUtils;
import com.sprd.ext.multimode.MultiModeController;

import java.util.List;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

public class DesktopGridController extends BaseController
        implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "DesktopGridController";

    private final DesktopGridModel mModel;
    private final LauncherAppMonitor mMonitor;

    private final LauncherAppMonitorCallback mAppMonitorCallback = new LauncherAppMonitorCallback() {
        @Override
        public void onDbUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            GridSizeMigrationTask.onDbUpgrade(mContext, oldVersion, newVersion);
            mMonitor.unregisterCallback(this);
        }
    };

    public DesktopGridController(Context context, LauncherAppMonitor monitor) {
        super(context);
        mModel = new DesktopGridModel(context);
        mMonitor = monitor;
        mMonitor.registerCallback(mAppMonitorCallback);
    }

    public void initPreference(ListPreference listPref) {
        if (null == listPref) {
            return;
        }

        final List<String> entries = getGridOptionEntries();
        final List<String> values = getGridOptionNames();
        if (!FeatureFlags.showFlagTogglerUi(mContext)
                && (entries.size() <= 1 || values.size() <= 1)) {
            LogUtils.w(TAG, "Remove desktop grid preference due to: entries.size() = "
                    + entries.size() + ", values.size() = " + values.size());
            listPref.getParent().removePreference(listPref);
            return;
        }

        listPref.setEntries(entries.toArray(new CharSequence[0]));
        listPref.setEntryValues(values.toArray(new CharSequence[0]));

        String gridNameDef = getDefaultGridName();
        String gridName = Utilities.getPrefs(mContext).getString(
                MultiModeController.getKeyByMode(mContext, KEY_IDP_GRID_NAME), gridNameDef);
        if (values.contains(gridName)) {
            listPref.setValue(gridName);
        } else {
            LogUtils.d(TAG, "The value in SharedPreference is invalid, so make "
                    + values.get(0) + " as default!");
            listPref.setValue(values.get(0));
        }
        listPref.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (LauncherSettingsExtension.sIsUserAMonkey) {
            return false;
        }

        if (APPLY_CONFIG_AT_RUNTIME.get()) {
            InvariantDeviceProfile.INSTANCE.get(mContext).setCurrentGrid(mContext, (String) newValue);
        } else {
            String gridKey = MultiModeController.getKeyByMode(mContext, KEY_IDP_GRID_NAME);
            boolean success = Utilities.getPrefs(mContext).edit()
                    .putString(gridKey, (String) newValue).commit();
            if (LogUtils.DEBUG) {
                LogUtils.d(TAG, "onPreferenceChange: newGridName = " + newValue
                        + ", success = " + success);
            }

            // Kill the process to apply the grid change.
            android.os.Process.killProcess(android.os.Process.myPid());
        }
        return true;
    }

    public String getDefaultGridName() {
        return mModel.getDefaultGridName();
    }

    public boolean isGridNameValid(String gridName) {
        return mModel.getGridOptionNames().contains(gridName);
    }

    List<String> getGridOptionNames() {
        return mModel.getGridOptionNames();
    }

    List<String> getGridOptionEntries() {
        return mModel.getGridOptionEntries();
    }
}
