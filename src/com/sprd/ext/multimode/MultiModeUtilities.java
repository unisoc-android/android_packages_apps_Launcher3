package com.sprd.ext.multimode;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Process;
import android.os.UserHandle;
import android.text.TextUtils;

import com.android.launcher3.LauncherFiles;
import com.android.launcher3.LauncherProvider.DatabaseHelper;
import com.android.launcher3.LauncherSettings.Favorites;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.shortcuts.DeepShortcutManager;
import com.android.launcher3.shortcuts.ShortcutKey;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.TraceHelper;
import com.sprd.ext.FeatureOption;
import com.sprd.ext.LauncherSettingsExtension;
import com.sprd.ext.LogUtils;
import com.sprd.ext.UtilitiesExt;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by SPRD on 09/14/18.
 */

class MultiModeUtilities {
    private static final String TAG = "MultiModeUtilities";

    static final String SINGLE = "single";
    static final String DUAL = "dual";
    static final String SL_PREFIX = "sl_";

    static boolean isSupportDynamicHomeStyle(Context context) {
        return FeatureOption.SPRD_MULTI_MODE_SUPPORT.get()
                && context.getResources().getBoolean(R.bool.show_home_screen_style_settings);
    }

    static boolean isSingleLayerMode(Context context) {
        return FeatureOption.SPRD_MULTI_MODE_SUPPORT.get()
                && SINGLE.equals(getHomeScreenStylePrefValue(context));
    }

    static boolean isDefaultMode(Context context) {
        if (!FeatureOption.SPRD_MULTI_MODE_SUPPORT.get() || context == null) {
            return true;
        }

        if (!isSupportDynamicHomeStyle(context)) {
            return true;
        }

        String defaultMode = context.getResources().getString(R.string.default_home_screen_style);
        return defaultMode.equals(getHomeScreenStylePrefValue(context));
    }

    static String getHomeScreenStylePrefValue(Context context) {
        if (context == null) {
            return DUAL;
        }
        Resources res = context.getResources();
        return Utilities.getPrefs(context)
                .getString(LauncherSettingsExtension.PREF_HOME_SCREEN_STYLE_KEY,
                        res.getString(R.string.default_home_screen_style));
    }

    @SuppressLint("ApplySharedPref")
    static void syncSaveNewModel(Context context, String newModel) {
        String key = LauncherSettingsExtension.PREF_HOME_SCREEN_STYLE_KEY;
        Utilities.getPrefs(context).edit().putString(key, newModel).commit();
    }

    static void updateSingleModeAppIconDb(Context context, SQLiteDatabase db) {
        String slDbName = LauncherFiles.getLauncherDb(true);
        String dbName = new File(db.getPath()).getName();

        if (!dbName.equals(slDbName)) {
            LogUtils.d(TAG, "updateSingleModeAppIconDb:"
                    + dbName + " is not " + slDbName + ", no need update.");
            return;
        }
        LogUtils.d(TAG, "updateSingleModeAppIconDb : " + dbName);

        UserManagerCompat userManager = UserManagerCompat.getInstance(context);
        LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(context);
        PackageManager packageManager = context.getPackageManager();

        // Get all launchable activities that match #ACTION_MAIN and #CATEGORY_LAUNCHER
        ArrayList<ComponentKey> allApps = new ArrayList<>();
        for (UserHandle user : userManager.getUserProfiles()) {
            boolean userUnlocked = userManager.isUserUnlocked(user);
            // We can only query for app icon when the user is unlocked.
            if (userUnlocked) {
                List<LauncherActivityInfo> apps = launcherApps.getActivityList(null, user);
                for (LauncherActivityInfo info : apps) {
                    allApps.add(new ComponentKey(info.getComponentName(), user));
                }
            }
        }

        // Check app icon in db.
        try (Cursor c = db.query(Favorites.TABLE_NAME,
                new String[]{Favorites._ID, Favorites.INTENT, Favorites.PROFILE_ID},
                "itemType=" + Favorites.ITEM_TYPE_APPLICATION, null,
                null, null, null)) {
            final int indexId = c.getColumnIndexOrThrow(Favorites._ID);
            final int indexIntent = c.getColumnIndexOrThrow(Favorites.INTENT);
            final int indexUser = c.getColumnIndexOrThrow(Favorites.PROFILE_ID);

            while (c.moveToNext()) {
                int id = c.getInt(indexId);
                UserHandle user = userManager.getUserForSerialNumber(c.getInt(indexUser));
                Intent intent = Intent.parseUri(c.getString(indexIntent), 0);

                ComponentName cn = intent.getComponent();
                if (cn == null) continue;


                if (launcherApps.isActivityEnabledForProfile(cn, user)
                        && !allApps.contains(new ComponentKey(cn, user))) {
                    try {
                        // Try to replace it to targetActivity.
                        ActivityInfo activityInfo = packageManager.getActivityInfo(cn, 0);
                        // Target Activity is in all apps.
                        if (activityInfo.targetActivity != null
                                && allApps.contains(new ComponentKey(
                                new ComponentName(cn.getPackageName(), activityInfo.targetActivity)
                                , user))) {
                            intent.setClassName(cn.getPackageName(), activityInfo.targetActivity);
                            LogUtils.d(TAG, "Replace " + cn + "to " + intent.getComponent());

                            ContentValues newValues = new ContentValues();
                            newValues.put(Favorites.INTENT, intent.toUri(0));

                            db.update(Favorites.TABLE_NAME, newValues,
                                    Favorites._ID + "=" + id, null);
                        } else {
                            // Deleted it.
                            db.delete(Favorites.TABLE_NAME, Favorites._ID + "=" + id, null);
                            LogUtils.w(TAG, "Deleted app icon, targetActivity not valid cn:" + cn);
                        }
                    } catch (PackageManager.NameNotFoundException nnfe) {
                        // Deleted it.
                        db.delete(Favorites.TABLE_NAME, Favorites._ID + "=" + id, null);
                        LogUtils.w(TAG, "Deleted app icon not valid cn:" + cn);
                    }
                }
            }
        } catch (URISyntaxException | SQLiteException e) {
            LogUtils.e(TAG, "query app icon frome Db error!", e);
        }
        // Don't close db in case it is a parameter in.
    }

    static void restoreShortcuts(Context context, HashSet<ShortcutKey> preModeShortcutKeys) {
        String sectionName = "restoreShortcuts";
        TraceHelper.beginSection(sectionName);

        preModeShortcutKeys.clear();
        preModeShortcutKeys.addAll(
                queryDeepShortcutsFromDb(context, LauncherFiles.getLauncherNonCurModeDb(context)));
        TraceHelper.partitionSection(sectionName, "query pre mode db");

        HashSet<ShortcutKey> curModeShortcutKeys =
                queryDeepShortcutsFromDb(context, LauncherFiles.getLauncherDb(context));
        TraceHelper.partitionSection(sectionName, "query cur mode db");

        DeepShortcutManager dsMgr = DeepShortcutManager.getInstance(context);
        UserManagerCompat userManager = UserManagerCompat.getInstance(context);

        // Load all unlocked user's shortcuts.
        HashMap<ShortcutKey, ShortcutInfo> shortcutKeyToPinnedShortcuts = new HashMap<>();
        for (UserHandle user : userManager.getUserProfiles()) {
            if (userManager.isUserUnlocked(user)) {
                List<ShortcutInfo> pinnedShortcuts =
                        dsMgr.queryForPinnedShortcuts(null, user);
                if (dsMgr.wasLastCallSuccess()) {
                    for (ShortcutInfo shortcut : pinnedShortcuts) {
                        shortcutKeyToPinnedShortcuts.put(ShortcutKey.fromInfo(shortcut),
                                shortcut);
                    }
                }
            }
        }

        // unPinned pre mode Shortcut
        for (ShortcutKey key : preModeShortcutKeys) {
            ShortcutInfo shortcutInfo = shortcutKeyToPinnedShortcuts.get(key);
            if (!curModeShortcutKeys.contains(key) && shortcutInfo != null
                    && (shortcutInfo.isDynamic() || shortcutInfo.isDeclaredInManifest())) {
                if (LogUtils.DEBUG_ALL) {
                    LogUtils.d(TAG, "unPinnedShortcuts, key:" + key + " info:" + shortcutInfo);
                }
                // Only unpin the dynamic or mainfest shortcuts
                dsMgr.unpinShortcut(key);
                if (LogUtils.DEBUG_ALL) {
                    LogUtils.d(TAG, "unpin shortcut ret:" + dsMgr.wasLastCallSuccess());
                }
            }
        }

        // Pinned cur mode Shortcut
        for (ShortcutKey key : curModeShortcutKeys) {
            ShortcutInfo shortcutInfo = shortcutKeyToPinnedShortcuts.get(key);
            if (LogUtils.DEBUG_ALL) {
                LogUtils.d(TAG, "restorePinnedShortcuts, shortcut:" + key + " info:" + shortcutInfo);
            }
            if (shortcutInfo == null) {
                // Only pin the shortcuts that is unpinned
                dsMgr.pinShortcut(key);
                if (LogUtils.DEBUG_ALL) {
                    LogUtils.d(TAG, "pin shortcut ret:" + dsMgr.wasLastCallSuccess());
                }
            }
        }
        TraceHelper.endSection(sectionName, "restore end");
    }

    static HashSet<ShortcutKey> queryDeepShortcutsFromDb(Context context, String dbName) {
        HashSet<ShortcutKey> shortcutKeys = new HashSet<>();
        DatabaseHelper dbHelper = new DatabaseHelper(context, null, dbName);
        try (Cursor c = dbHelper.getWritableDatabase()
                .query(Favorites.TABLE_NAME,
                        new String[]{Favorites.INTENT, Favorites.PROFILE_ID},
                        "itemType=" + Favorites.ITEM_TYPE_DEEP_SHORTCUT, null,
                        null, null, null)) {
            UserManagerCompat userManager = UserManagerCompat.getInstance(context);
            while (c.moveToNext()) {
                UserHandle user = UtilitiesExt.isRoboUnitTest() ? Process.myUserHandle()
                        : userManager.getUserForSerialNumber(c.getInt(1));
                Intent intent = Intent.parseUri(c.getString(0), 0);
                shortcutKeys.add(ShortcutKey.fromIntent(intent, user));
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "queryDeepShortcutsFromeDb error!", e);
        } finally {
            dbHelper.close();
        }

        return shortcutKeys;
    }

    // We add SL_PREFIX only single layer mode & support dynamic change.
    static String getKeyByMode(String originalKey, boolean isSingleMode, int dbVersion) {
        if (TextUtils.isEmpty(originalKey)) {
            throw new RuntimeException("getKeyByMode() the original key is empty!");
        }

        String outKey = originalKey;
        if (MultiModeController.isSupportDynamicChange()) {
            switch (dbVersion) {
                case 27:
                    // Android P db version is 27.
                    outKey = isSingleMode ? SINGLE + "_" + originalKey : DUAL + "_" + originalKey;
                    break;
                default:
                    outKey = isSingleMode ? SL_PREFIX + originalKey : originalKey;
                    break;

            }
        }
        return outKey;
    }
}
