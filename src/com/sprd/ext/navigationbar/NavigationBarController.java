package com.sprd.ext.navigationbar;

import android.content.ContentResolver;
import android.content.Context;
import android.os.RemoteException;
import android.provider.Settings;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.Launcher;
import com.sprd.ext.BaseController;
import com.sprd.ext.LauncherAppMonitor;
import com.sprd.ext.LauncherAppMonitorCallback;
import com.sprd.ext.LogUtils;
import com.sprd.ext.SettingsObserver;

import java.io.FileDescriptor;
import java.io.PrintWriter;

import static com.android.launcher3.AbstractFloatingView.TYPE_ACTION_POPUP;
import static com.android.launcher3.AbstractFloatingView.TYPE_WIDGET_RESIZE_FRAME;

/**
 * Created by SPRD on 2018/11/7.
 */
public class NavigationBarController extends BaseController {
    private static final String TAG = "NavigationBarController";

    private static final String NAVIGATIONBAR_CONFIG = "navigationbar_config";
    private static final String SHOW_NAVIGATIONBAR = "show_navigationbar";

    private final Context mContext;
    private final LauncherAppMonitor mMonitor;
    private final SettingsObserver mNavigationBarShowObserver;
    private boolean mIsDynamicNavigationBar;
    private boolean mIsNavigationBarShowing;

    private final LauncherAppMonitorCallback mAppMonitorCallback = new LauncherAppMonitorCallback() {
        @Override
        public void onLauncherStart() {
            if (mNavigationBarShowObserver != null && mIsDynamicNavigationBar) {
                mNavigationBarShowObserver.register(SHOW_NAVIGATIONBAR);
            }
        }

        @Override
        public void onLauncherStop() {
            if (mNavigationBarShowObserver != null) {
                mNavigationBarShowObserver.unregister();
            }
        }

        @Override
        public void dump(String prefix, FileDescriptor fd, PrintWriter w, boolean dumpAll) {
            w.println();
            w.println(prefix + TAG + ":" + "mIsDynamicNavigationBar:" + mIsDynamicNavigationBar +
                    " mIsNavigationBarShowing:" + mIsNavigationBarShowing);
        }
    };

    public NavigationBarController(Context context, LauncherAppMonitor monitor) {
        super(context);

        mContext = context;
        mMonitor = monitor;
        mMonitor.registerCallback(mAppMonitorCallback);
        ContentResolver resolver = mContext.getContentResolver();
        mIsDynamicNavigationBar = dynamicNavigationBarEnable();
        mIsNavigationBarShowing = isNavigationBarShowing();
        SettingsObserver navigationBarCfgObserver = new SettingsObserver.System(resolver) {
            @Override
            public void onSettingChanged(boolean keySettingEnabled) {
                mIsDynamicNavigationBar = dynamicNavigationBarEnable();
                LogUtils.d( TAG, "onSettingChanged, dynamic:" + mIsDynamicNavigationBar );
            }
        };
        navigationBarCfgObserver.register(NAVIGATIONBAR_CONFIG);

        mNavigationBarShowObserver = new SettingsObserver.System(resolver) {
            @Override
            public void onSettingChanged(boolean keySettingEnabled) {
                boolean show = isNavigationBarShowing();
                if (mIsNavigationBarShowing != show) {
                    mIsNavigationBarShowing = show;
                    Launcher launcher = mMonitor.getLauncher();
                    if (launcher != null) {
                        LogUtils.d(TAG, "onSettingChanged, show:" + mIsNavigationBarShowing);
                        AbstractFloatingView.closeOpenViews(launcher,
                                false, TYPE_ACTION_POPUP | TYPE_WIDGET_RESIZE_FRAME);
                    }
                }
            }
        };
    }

    private boolean isNavigationBarShowing() {
        return (Settings.System.getInt(mContext.getContentResolver(),
                SHOW_NAVIGATIONBAR, 0) & 0x1) != 0;
    }

    private boolean dynamicNavigationBarEnable() {
        return (Settings.System.getInt(mContext.getContentResolver(),
                NAVIGATIONBAR_CONFIG, 0) & 0x10) != 0;
    }

    public static boolean hasNavigationBar(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) {
            try {
                int displayID = wm.getDefaultDisplay().getDisplayId();
                return WindowManagerGlobal.getWindowManagerService().hasNavigationBar(displayID);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (NoSuchMethodError e) {
                LogUtils.w(TAG, "hasNavigationBar, NoSuchMethodError");
            }
        }
        return false;
    }

    public boolean isDynamicNavigationBarShowing() {
        return mIsNavigationBarShowing;
    }

    public boolean isDynamicNavigationBarEnable() {
        return mIsDynamicNavigationBar;
    }

    public static int getNavBarHeight(Context context) {
        return context.getResources().getDimensionPixelSize(
                com.android.internal.R.dimen.navigation_bar_height);
    }
}
