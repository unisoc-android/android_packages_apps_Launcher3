package com.sprd.ext;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;

import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.Preconditions;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Created by SPRD on 11/14/16.
 */

public class UtilitiesExt {
    private static final String TAG = "UtilitiesExt";
    private static final String IS_IN_ULTRA_SAVING_MODE = "is_in_ultra_saving_mode";
    private static Class<?> mStatusBarManagerClassType = null;
    private static Method mExpandNotificationsMethod = null;

    public static final int BASE_STACK_DEPTH = 3;

    public static final boolean IS_LOW_RAM = SystemPropertiesUtils.getBoolean("ro.config.low_ram", false);

    public static boolean isAppInstalled(Context context, String pkgName, UserHandle user) {
        return LauncherAppsCompat.getInstance(context).isPackageEnabledForProfile(pkgName, user);
    }

    public static Drawable getAppIcon(Context context, String pkgName, UserHandle userHandle) {
        LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(context);
        ApplicationInfo info = launcherApps.getApplicationInfo(pkgName,0, userHandle);
        if (info != null) {
            PackageManager pm = context.getPackageManager();
            return info.loadIcon(pm);
        }

        Resources res = Resources.getSystem();
        return res.getDrawable(android.R.mipmap.sym_def_app_icon, null);
    }

    public static CharSequence getAppLabelByPackageName(Context context, String pkgName) {
        boolean ret = false;
        ApplicationInfo info = null;
        PackageManager pm = context.getPackageManager();

        if (!TextUtils.isEmpty(pkgName)) {
            try {
                info = pm.getApplicationInfo(pkgName, 0);
                ret = info != null;
            } catch (PackageManager.NameNotFoundException e) {
                LogUtils.w(TAG, "get app label failed, pkgName:" + pkgName);
            }
        }

        return ret ? info.loadLabel(pm) : "";
    }

    public static Point getTextDrawPoint(Rect targetRect, Paint.FontMetrics fm) {
        Point p = new Point();
        int fontHeight = Math.round(fm.descent - fm.ascent);
        int paddingY = (targetRect.height() - fontHeight) >> 1;
        p.x = targetRect.centerX();
        p.y = targetRect.top + paddingY + Math.abs(Math.round(fm.ascent));
        return p;
    }

    /**
     * When set to true, apps will draw debugging information about their layouts.
     * @see android.view.View
     */
    private static boolean enableDebugLayout() {
        //see View.java:DEBUG_LAYOUT_PROPERTY
        return SystemPropertiesUtils.getBoolean("debug.layout", false);
    }

    public static void drawDebugRect(final Canvas canvas, final Rect rect, final int color) {
        if (enableDebugLayout()) {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(color);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(rect, paint);
        }
    }

    public static void DEBUG_PRINT_FUNCTIONNAME(int depth, String msg) {
            final StackTraceElement[] ste = Thread.currentThread().getStackTrace();
            String clsName = ste[1+depth].getClassName();
            String tag = clsName.substring(clsName.lastIndexOf(".") + 1);
            LogUtils.d(tag, ste[1+depth].getMethodName()
                    + (TextUtils.isEmpty(msg) ? "" : ", " + msg));
    }

    public static void DEBUG_PRINT_FUNCTIONNAME(Object obj) {
        DEBUG_PRINT_FUNCTIONNAME(BASE_STACK_DEPTH, Object2String(obj));
    }

    public static void closeCursorSilently(Cursor cursor) {
        try {
            if (cursor != null) cursor.close();
        } catch (Throwable t) {
            LogUtils.w(TAG, "fail to close", t);
        }
    }

    private static Class<?> getStatusBarManagerClass() throws ClassNotFoundException {
        if (mStatusBarManagerClassType == null) {
            mStatusBarManagerClassType = Class.forName("android.app.StatusBarManager");
        }
        return mStatusBarManagerClassType;
    }

    private static Method getExpandNotificationsMethod() throws Exception {
        if (mExpandNotificationsMethod == null) {
            Class clazz = getStatusBarManagerClass();
            mExpandNotificationsMethod = clazz.getDeclaredMethod("expandNotificationsPanel");
        }
        return mExpandNotificationsMethod;
    }

    public static void openNotifications(Context context){
        try{
            if (getExpandNotificationsMethod() != null) {
                Method method = getExpandNotificationsMethod();
                method.invoke(context.getSystemService("statusbar"));
            }
        } catch(Exception ex) {
            LogUtils.e(TAG, "expand notifications panel failed : ");
            ex.printStackTrace();
        }
    }

    public static Point parsePoint(String point) {
        String[] split = point.split(",");
        return new Point(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
    }

    public static String getPointString(int x, int y) {
        return String.format(Locale.ENGLISH, "%d,%d", x, y);
    }

    // Returns the input value x clamped to the range [min, max].
    public static int clamp(int x, int min, int max) {
        if (x > max) return max;
        if (x < min) return min;
        return x;
    }

    public static String marshall(Parcelable parcelable) {
        Parcel p = Parcel.obtain();
        parcelable.writeToParcel(p, 0);
        byte[] bytes = p.marshall();
        p.recycle();

        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    public static <T> T unMarshall(String str, Parcelable.Creator<T> creator) {
        byte[] bytes = Base64.decode(str, Base64.DEFAULT);

        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0);
        T result = creator.createFromParcel(parcel);
        parcel.recycle();

        return result;
    }

    public static boolean getSystemBooleanRes(String resName) {
        Resources res = Resources.getSystem();
        int resId = res.getIdentifier(resName, "bool", "android");

        if (resId != 0) {
            return res.getBoolean(resId);
        } else {
            LogUtils.e(TAG, "Failed to get system resource ID. Incompatible framework version?");
            return false;
        }
    }

    public static float checkFloatPropertyValueValid (float value, String propertyName, float min, float max) {
        float defaultValue = 1.0f;
        if (value > min && value < max) {
            return value;
        }
        return defaultValue;
    }

    public static Rect getIconRect(int iconSize, View view) {
        Point center = new Point(view.getScrollX() + (view.getWidth() >> 1),
                view.getScrollY() + view.getPaddingTop() + (iconSize >> 1));
        Rect iconRect = new Rect();

        iconRect.left   = center.x - (iconSize >> 1);
        iconRect.top    = center.y - (iconSize >> 1);
        iconRect.right  = iconRect.left + iconSize;
        iconRect.bottom = iconRect.top + iconSize;
        return iconRect;
    }

    public static String componentKeyToString(Context context, ComponentKey componentKey) {
        if (null == context || null == componentKey || null == componentKey.componentName) {
            return "";
        }
        String flattened = componentKey.componentName.flattenToShortString();
        if (null != componentKey.user) {
            flattened += "#" + UserManagerCompat.getInstance(context).getSerialNumberForUser(componentKey.user);
        }
        return flattened;
    }

    public static ComponentKey stringToComponentKey(Context context, String componentKeyStr) {
        ComponentName componentName;
        UserHandle user;
        int userDelimiterIndex = componentKeyStr.indexOf("#");
        if (userDelimiterIndex != -1) {
            String componentStr = componentKeyStr.substring(0, userDelimiterIndex);
            Long componentUser = Long.valueOf(componentKeyStr.substring(userDelimiterIndex + 1));
            componentName = ComponentName.unflattenFromString(componentStr);
            user = UserManagerCompat.getInstance(context)
                    .getUserForSerialNumber(componentUser.longValue());
        } else {
            // No user provided, default to the current user
            componentName = ComponentName.unflattenFromString(componentKeyStr);
            user = Process.myUserHandle();
        }
        Preconditions.assertNotNull(componentName);
        Preconditions.assertNotNull(user);
        return new ComponentKey(componentName, user);
    }

    public static boolean isInUltraSavingMode(Context context) {
        ContentResolver resolver = context.getContentResolver();
        return Settings.System.getInt(resolver, IS_IN_ULTRA_SAVING_MODE, 0) == 1;
    }

    public static String Object2String(Object obj) {
        return obj != null ? obj.toString() : "NULL";
    }

    @SuppressWarnings("CallToSystemExit")
    public static void exitLauncher() {
        System.exit(0);
    }

    public static boolean isRoboUnitTest() {
        return isASRoboUnitTest() || Build.UNKNOWN.equals(Build.DEVICE);
    }

    public static boolean isASRoboUnitTest() {
        return "robolectric".equals(Build.DEVICE);
    }
}
