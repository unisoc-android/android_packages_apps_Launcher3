package com.sprd.ext.unreadnotifier.prefs;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.android.launcher3.LauncherModel;
import com.sprd.ext.LogUtils;
import com.sprd.ext.unreadnotifier.UnreadInfoManager;

import java.util.ArrayList;

public abstract class UnreadBaseItemInfo {

    private static final int OBSERVER_HANDLER_DELAY = 1000;
    private static final Handler sUiHandler = new Handler();
    private static final Handler sWorkerHandler = new Handler(LauncherModel.getWorkerLooper());

    private final UnreadInfoManager mUnreadInfoManager;

    public int type;
    public String prefKey;
    public String permission;
    public UnreadContentObserver contentObserver;
    public ComponentName defaultCn;
    public String currentCn = "";
    public String oldCn = "";
    public ArrayList<String> installedList = new ArrayList<>();

    protected final Context mContext;
    boolean mDefaultState;
    private int mUnreadCount;
    private Uri mUri;

    UnreadBaseItemInfo(Context context, UnreadInfoManager unreadInfoManager, int type,
                       String prefKey, Uri uri, String permission, ComponentName defaultCn) {
        mContext = context;
        mUnreadInfoManager = unreadInfoManager;
        this.type = type;
        this.prefKey = prefKey;
        mUri = uri;
        this.permission = permission;
        this.defaultCn = defaultCn;
        contentObserver = new UnreadContentObserver();
    }

    public boolean checkPermission() {
        boolean isChecked;
        isChecked = mContext.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        return isChecked;
    }

    public boolean isPersistChecked() {
        return AppListPreference.isPreferenceChecked(mContext, prefKey, mDefaultState);
    }


    private Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            final int unreadNum = readUnreadCount();
            sUiHandler.post(() -> {
                if (!isPersistChecked()) {
                    return;
                }
                setUnreadCount(unreadNum);
                mUnreadInfoManager.updateUI(mContext, currentCn);
            });
        }
    };

    /**
     * Send broadcast to update the unread info.
     */
    public void updateUIFromDatabase() {
        sWorkerHandler.removeCallbacks(mUpdateRunnable);
        sWorkerHandler.postDelayed(mUpdateRunnable, OBSERVER_HANDLER_DELAY);
    }

    public ComponentName getCurrentComponentName() {
        ComponentName componentName = null;
        String value = readSavedComponentName();
        if (!TextUtils.isEmpty(value)) {
            componentName = ComponentName.unflattenFromString(value);
        }
        return componentName;
    }

    public void setUnreadCount(int num) {
        mUnreadCount = Math.max(num, 0);
    }

    public int getUnreadCount() {
        return mUnreadCount;
    }

    private String readSavedComponentName() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        return sp.getString(prefKey, "");
    }

    private void saveComponentName(String value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
        editor.putString(prefKey, value);
        editor.apply();
    }

    public void verifyDefaultCN(final ArrayList<String> installedList, final ComponentName defCn) {
        if (installedList.size() == 0) {
            return;
        }

        String savedValue = readSavedComponentName();
        int size = installedList.size();
        String cn;
        if (size == 1) {
            cn = installedList.get(0);
            currentCn = cn;
        } else {
            String defaultCnString = (defCn != null) ? defCn.flattenToString() : "";

            if (installedList.contains(savedValue)) {
                currentCn = savedValue;
            } else if (installedList.contains(defaultCnString)) {
                currentCn = defaultCnString;
            } else {
                currentCn = "";
            }
        }
        if (!TextUtils.equals(currentCn, savedValue)) {
            saveComponentName(currentCn);
        }
    }

    public void setInstalledList(ArrayList<String> list) {
        installedList = list;
    }

    public abstract int readUnreadCount();

    public abstract String getUnreadHintString();

    public abstract ArrayList<String> loadApps(Context context);

    public class UnreadContentObserver extends ContentObserver {
        private static final String TAG = "UnreadContentObserver";
        private boolean mRegistered = false;

        UnreadContentObserver() {
            super(new Handler());
        }

        public void registerContentObserver() {
            if (!mRegistered) {
                mContext.getContentResolver().registerContentObserver(mUri, true, this);
                mRegistered = true;
            }
        }

        public void unregisterContentObserver() {
            if (mRegistered) {
                mRegistered = false;
                mContext.getContentResolver().unregisterContentObserver(this);
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (LogUtils.DEBUG_UNREAD) {
                LogUtils.d(TAG, String.format("onChange: uri=%s selfChange=%b", uri.toString(), selfChange));
            }
            updateUIFromDatabase();
        }
    }
}
