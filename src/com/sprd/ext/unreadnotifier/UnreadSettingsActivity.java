package com.sprd.ext.unreadnotifier;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.text.TextUtils;
import android.widget.Toast;

import com.android.launcher3.R;
import com.sprd.ext.LauncherAppMonitor;
import com.sprd.ext.LogUtils;
import com.sprd.ext.unreadnotifier.prefs.AppListPreference;
import com.sprd.ext.unreadnotifier.prefs.UnreadBaseItemInfo;

import java.util.Arrays;

public class UnreadSettingsActivity extends Activity {
    private static final String TAG = "UnreadSettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new UnreadSettingsFragment())
                .commit();
    }

    public static class UnreadSettingsFragment extends PreferenceFragment
            implements Preference.OnPreferenceChangeListener,
            AppListPreference.OnPreferenceCheckBoxClickListener {
        public static final String PREF_KEY_MISS_CALL = "pref_missed_call_count";
        public static final String PREF_KEY_UNREAD_SMS = "pref_unread_sms_count";
        public static final String PREF_KEY_UNREAD_EMAIL = "pref_unread_email_count";
        public static final String PREF_KEY_UNREAD_CALENDAR = "pref_unread_calendar_count";

        private Context mContext;
        private UnreadInfoController mUnreadInfoController;
        private PackageManager mPm;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mContext = getActivity();
            mPm = mContext.getPackageManager();

            mUnreadInfoController = LauncherAppMonitor.getInstance(mContext).getUnreadInfoController();
            mUnreadInfoController.getUnreadInfoManager().createItemIfNeeded();
            addPreferencesFromResource(R.xml.unread_settings_preferences);

            init();
        }

        private void updateUnreadItemInfos(UnreadBaseItemInfo item) {
            if (!item.checkPermission()) {
                return;
            }
            boolean isChecked = item.isPersistChecked();
            ComponentName cn = item.getCurrentComponentName();
            String value = null;
            if (cn != null) {
                value = cn.flattenToShortString();
            }
            if (isChecked) {
                item.contentObserver.registerContentObserver();
                item.updateUIFromDatabase();
            } else {
                item.contentObserver.unregisterContentObserver();
                item.setUnreadCount(0);
                mUnreadInfoController.getUnreadInfoManager().updateUI(mContext, value);
            }
        }

        private void updateComponentUnreadInfos(UnreadBaseItemInfo item) {
            if (!item.checkPermission()) {
                return;
            }
            String oldValue = item.oldCn;
            String currentValue = item.currentCn;

            if (item.isPersistChecked()) {
                UnreadInfoManager unreadInfoManager = mUnreadInfoController.getUnreadInfoManager();
                // clear the unread info on the old icon, and update the current icon
                if (!TextUtils.isEmpty(oldValue)) {
                    unreadInfoManager.updateUI(mContext, oldValue);
                }
                unreadInfoManager.updateUI(mContext, currentValue);
            }

            // update the old value
            item.oldCn = currentValue;
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (preference instanceof AppListPreference) {
                UnreadBaseItemInfo item = ((AppListPreference) preference).itemInfo;
                item.currentCn = (String) newValue;
                ((AppListPreference) preference).setValue((String) newValue);
                preference.setSummary(((AppListPreference) preference).getEntry());
                updateComponentUnreadInfos(item);
                return false;
            }
            return true;
        }

        @Override
        public void onPreferenceCheckboxClick(Preference preference) {
            String key = preference.getKey();
            if (LogUtils.DEBUG_UNREAD) {
                LogUtils.d(TAG, "onPreferenceCheckboxClick, key is: " + key);
            }
            UnreadBaseItemInfo item = mUnreadInfoController.getUnreadInfoManager().getItemByKey(key);
            if (item != null) {
                if (item.isPersistChecked() && !item.checkPermission()) {
                    requestPermissions(new String[]{item.permission}, item.type);
                }
                updateUnreadItemInfos(item);
            }
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (LogUtils.DEBUG_UNREAD) {
                LogUtils.d(TAG, "onRequestPermissionsResult, requestCode: " + requestCode + ", permissions: "
                        + Arrays.toString(permissions) + ", grantResults:" + Arrays.toString(grantResults));
            }

            UnreadInfoController uc = LauncherAppMonitor.getInstance(mContext).getUnreadInfoController();
            if (null != uc) {
                UnreadBaseItemInfo item = uc.getUnreadInfoManager().getItemByType(requestCode);
                if (item != null) {
                    if (grantResults.length == 1) {
                        if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                            Toast.makeText(mContext, item.getUnreadHintString(), Toast.LENGTH_LONG).show();
                        } else {
                            item.contentObserver.registerContentObserver();
                            item.updateUIFromDatabase();
                        }
                    } else {
                        LogUtils.e(TAG, "grantResult length error.");
                    }
                }
            }
        }

        private void init() {
            AppListPreference defaultPhonePref = (AppListPreference) findPreference(PREF_KEY_MISS_CALL);
            initPref(defaultPhonePref, UnreadInfoManager.TYPE_CALL_LOG, R.string.pref_missed_call_count_summary);

            AppListPreference defaultSmsPref = (AppListPreference) findPreference(PREF_KEY_UNREAD_SMS);
            initPref(defaultSmsPref, UnreadInfoManager.TYPE_SMS, R.string.pref_unread_sms_count_summary);

            AppListPreference emailPref = (AppListPreference) findPreference(PREF_KEY_UNREAD_EMAIL);
            initPref(emailPref, UnreadInfoManager.TYPE_EMAIL, R.string.pref_unread_email_count_summary);

            AppListPreference calendarPref = (AppListPreference) findPreference(PREF_KEY_UNREAD_CALENDAR);
            initPref(calendarPref, UnreadInfoManager.TYPE_CALENDAR, R.string.pref_unread_calendar_count_summary);
        }

        private boolean hasValidSelectItem(AppListPreference pref) {
            return pref != null && pref.itemInfo != null && pref.itemInfo.installedList != null
                    && !pref.itemInfo.installedList.isEmpty();
        }

        private void initPref(AppListPreference pref, int type, int defaultSummaryID) {
            if (pref != null) {
                pref.initItemListValues(mUnreadInfoController, type);
                if (hasValidSelectItem(pref)) {
                    pref.setOnPreferenceCheckBoxClickListener(this);
                    pref.setPreferenceChecked(pref.itemInfo.isPersistChecked());
                    loadPrefsSetting(pref, defaultSummaryID);
                    pref.setOnPreferenceChangeListener(this);
                } else {
                    getPreferenceScreen().removePreference(pref);
                    if (LogUtils.DEBUG_UNREAD) {
                        LogUtils.d(TAG, "preference: " + pref.getTitle() + " is null, remove it.");
                    }
                }
            }
        }

        private void loadPrefsSetting(AppListPreference preference, int defaultSummaryID) {
            if (preference == null) {
                return;
            }

            boolean ret = false;
            ApplicationInfo info = null;
            try {
                UnreadBaseItemInfo item = preference.itemInfo;
                String pkgName = ComponentName.unflattenFromString(item.currentCn).getPackageName();
                item.oldCn = item.currentCn;
                info = mPm.getApplicationInfo(pkgName, 0);
                ret = true;
            } catch (Exception e) {
                LogUtils.e(TAG, "loadPrefsSetting failed, e:" + e);
            }

            preference.setSummary(ret ? info.loadLabel(mPm) : getString(defaultSummaryID));
        }
    }
}
