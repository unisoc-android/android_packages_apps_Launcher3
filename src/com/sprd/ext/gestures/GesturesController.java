package com.sprd.ext.gestures;

import android.content.Context;

import androidx.preference.Preference;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.sprd.ext.BaseController;
import com.sprd.ext.LauncherAppMonitor;
import com.sprd.ext.LauncherAppMonitorCallback;
import com.sprd.ext.LauncherSettingsExtension;
import com.sprd.ext.LogUtils;
import com.sprd.ext.UtilitiesExt;
import com.sprd.ext.gestures.LauncherRootViewGestures.Gesture;

import java.io.FileDescriptor;
import java.io.PrintWriter;

public class GesturesController extends BaseController
        implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "GesturesController";

    /**
     * Pull down actions.
     */
    public static final String NONE = "none";
    public static final String NOTIFICATION = "notification";
    public static final String SEARCH = "search";

    private final LauncherAppMonitor mMonitor;
    private LauncherRootViewGestures mGesture;

    private String mPullDownAction;
    private LauncherRootViewGestures.OnGestureListener mPullDownListener;

    private final LauncherAppMonitorCallback mAppMonitorCallback = new LauncherAppMonitorCallback() {
        @Override
        public void onLauncherCreated() {
            mGesture = new LauncherRootViewGestures(mMonitor.getLauncher());
            mPullDownAction = Utilities.getPrefs(mContext)
                    .getString(LauncherSettingsExtension.PREF_ONEFINGER_PULLDOWN,
                            mContext.getResources().getString(R.string.default_pull_down_value));
            mPullDownListener = (launcher, gesture) -> {
                if (gesture == Gesture.ONE_FINGER_SLIDE_DOWN) {
                    return doAction(launcher, mPullDownAction);
                }
                return false;
            };
        }

        @Override
        public void onLauncherStart() {
            if (mGesture != null) {
                mGesture.registerOnGestureListener(mPullDownListener);
            }
        }

        @Override
        public void onLauncherStop() {
            if (mGesture != null) {
                mGesture.unregisterOnGestureListener(mPullDownListener);
            }
        }

        @Override
        public void onLauncherDestroy() {
            mPullDownListener = null;
            mGesture = null;
        }
    };

    public GesturesController(Context context, LauncherAppMonitor monitor) {
        super(context);
        mMonitor = monitor;
        mMonitor.registerCallback(mAppMonitorCallback);
    }

    public LauncherRootViewGestures getGesture() {
        return mGesture;
    }

    private boolean doAction(Launcher launcher, String action) {
        switch (action) {
            case SEARCH:
                if (launcher != null) {
                    launcher.onSearchRequested();
                }
                return true;
            case NOTIFICATION:
                UtilitiesExt.openNotifications(mContext);
                return true;
            case NONE:
                //do nothing
                return true;
            default:
                LogUtils.w(TAG, "error action : " + action);
                break;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        switch (key) {
            case LauncherSettingsExtension.PREF_ONEFINGER_PULLDOWN: {
                mPullDownAction = (String) newValue;
                return true;
            }
            default:
                break;
        }
        return false;
    }

    @Override
    public void dumpState(String prefix, FileDescriptor fd, PrintWriter writer, boolean dumpAll) {
        writer.println();
        writer.println(prefix + TAG + ": mPullDownAction:" + mPullDownAction);
    }
}
