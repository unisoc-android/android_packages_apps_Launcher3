package com.sprd.ext.multimode;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Process;

import com.android.launcher3.LauncherSettings;
import com.android.launcher3.shortcuts.ShortcutKey;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.HashSet;

import static android.provider.BaseColumns._ID;
import static com.android.launcher3.LauncherSettings.Favorites.CONTAINER;
import static com.android.launcher3.LauncherSettings.Favorites.CONTAINER_DESKTOP;
import static com.android.launcher3.LauncherSettings.Favorites.INTENT;
import static com.android.launcher3.LauncherSettings.Favorites.ITEM_TYPE;
import static com.android.launcher3.LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT;
import static com.android.launcher3.LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT;
import static com.android.launcher3.LauncherSettings.Favorites.PROFILE_ID;
import static com.android.launcher3.LauncherSettings.Favorites.TITLE;
import static com.android.launcher3.shortcuts.ShortcutKey.EXTRA_SHORTCUT_ID;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class DbQueryTest extends BaseMultiModeStyleTestCase {

    private void initDeepShorcutDb(int id, int itemType, String pkgName, String className) {
        Intent intent = new Intent();
        intent.setPackage(pkgName);
        intent.setClassName(pkgName, className);
        intent.setComponent(new ComponentName(pkgName, className));
        intent.putExtra(EXTRA_SHORTCUT_ID, className);
        ContentValues values = new ContentValues();
        values.put(_ID, id);
        values.put(TITLE, "my-deepshortcut");
        values.put(ITEM_TYPE, itemType);
        values.put(CONTAINER, CONTAINER_DESKTOP);
        values.put(PROFILE_ID, 0);
        values.put(INTENT, intent.toUri(0));
        mProvider.getDataBaseHelper().getWritableDatabase()
                .insert(LauncherSettings.Favorites.TABLE_NAME, null, values);
    }

    @Test
    public void queryDeepShortcut() {
        HashSet<ShortcutKey> shortcutKeys;
        initDeepShorcutDb(0, ITEM_TYPE_SHORTCUT, "app1", "class2");
        shortcutKeys = MultiModeUtilities.queryDeepShortcutsFromDb(mTargetContext,
                mProvider.getDataBaseHelper().getDatabaseName());
        assertTrue(shortcutKeys.isEmpty());

        initDeepShorcutDb(1, ITEM_TYPE_DEEP_SHORTCUT, "app1", "class1");
        shortcutKeys = MultiModeUtilities.queryDeepShortcutsFromDb(mTargetContext,
                mProvider.getDataBaseHelper().getDatabaseName());
        assertFalse(shortcutKeys.isEmpty());
        assertTrue(shortcutKeys.contains(new ShortcutKey(new ComponentName("app1", "class1"),
                Process.myUserHandle())));
    }
}
