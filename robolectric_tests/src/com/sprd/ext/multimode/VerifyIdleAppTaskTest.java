package com.sprd.ext.multimode;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.os.UserHandle;
import android.util.Pair;

import com.android.launcher3.AppInfo;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.WorkspaceItemInfo;
import com.android.launcher3.model.BgDataModel;
import com.android.launcher3.util.ComponentKey;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class VerifyIdleAppTaskTest {

    public Context mTargetContext;
    public LauncherAppState appState;
    public LauncherModel model;
    private UserHandle myUser;
    BgDataModel mBgDataModel;
    private final Map<ComponentKey, Object> mAllAppsMap = new HashMap<>();

    @Before
    public void setup() {
        ShadowLog.stream = System.out;
        mTargetContext = RuntimeEnvironment.application;

        appState = mock(LauncherAppState.class);
        model = mock(LauncherModel.class);
        when(appState.getModel()).thenReturn(model);
        mBgDataModel = appState.getModel().getBgDataModel();

        myUser = Process.myUserHandle();

    }

    @Test
    public void testVerify_appInfo_item_no_exist() {
        // Clear workspaceItems first.
        mBgDataModel.workspaceItems.clear();
        // Add app to all apps.
        AppInfo appInfo = constructAppInfo("app1", "class1");
        ComponentKey cpKey = new ComponentKey(appInfo.getTargetComponent(), myUser);
        mAllAppsMap.put(cpKey, appInfo);

        VerifyIdleAppTask task = new VerifyIdleAppTask(mTargetContext, null, null, null, false);
        List<Pair<ItemInfo, Object>> result = task.verifyAllApps(mTargetContext, mAllAppsMap, false);

        // One item should be add.
        assertEquals(1, result.size());
        Pair<ItemInfo, Object> itemPair = result.get(0);
        // The item must the same appInfo.
        assertEquals(itemPair.first, appInfo);
    }

    @Test
    public void testVerify_appInfo_item_exist() {
        // Clear workspaceItems first.
        mBgDataModel.workspaceItems.clear();

        // Add app to all apps.
        AppInfo appInfo = constructAppInfo("app1", "class1");
        ComponentKey cpKey = new ComponentKey(appInfo.getTargetComponent(), myUser);
        mAllAppsMap.put(cpKey, appInfo);
        mBgDataModel.workspaceItems.add(new WorkspaceItemInfo(appInfo));

        VerifyIdleAppTask task = new VerifyIdleAppTask(mTargetContext, null, null, null, false);
        List<Pair<ItemInfo, Object>> result = task.verifyAllApps(mTargetContext, mAllAppsMap, false);

        // No item will be added.
        assertTrue(result.isEmpty());
    }

    @Test
    public void testVerify_appInfo_items_no_exist() {
        // Clear workspaceItems first.
        mBgDataModel.workspaceItems.clear();
        // Add app to all apps.
        AppInfo appInfo1 = constructAppInfo("app1", "class1");
        ComponentKey cpKey1 = new ComponentKey(appInfo1.getTargetComponent(), myUser);
        mAllAppsMap.put(cpKey1, appInfo1);
        AppInfo appInfo2 = constructAppInfo("app2", "class2");
        ComponentKey cpKey2 = new ComponentKey(appInfo2.getTargetComponent(), myUser);
        mAllAppsMap.put(cpKey2, appInfo2);

        VerifyIdleAppTask task = new VerifyIdleAppTask(mTargetContext, null, null, null, false);
        List<Pair<ItemInfo, Object>> result = task.verifyAllApps(mTargetContext, mAllAppsMap, false);

        // Two items should be add.
        assertEquals(2, result.size());
    }

    @Test
    public void testVerify_appInfo_items_part_exist() {
        // Clear workspaceItems first.
        mBgDataModel.workspaceItems.clear();
        // Add app to all apps.
        AppInfo appInfo1 = constructAppInfo("app1", "class1");
        ComponentKey cpKey1 = new ComponentKey(appInfo1.getTargetComponent(), myUser);
        mAllAppsMap.put(cpKey1, appInfo1);
        mBgDataModel.workspaceItems.add(new WorkspaceItemInfo(appInfo1));
        AppInfo appInfo2 = constructAppInfo("app2", "class2");
        ComponentKey cpKey2 = new ComponentKey(appInfo2.getTargetComponent(), myUser);
        mAllAppsMap.put(cpKey2, appInfo2);


        VerifyIdleAppTask task = new VerifyIdleAppTask(mTargetContext, null, null, null, false);
        List<Pair<ItemInfo, Object>> result = task.verifyAllApps(mTargetContext, mAllAppsMap, false);

        // One item should be add.
        assertEquals(1, result.size());
        Pair<ItemInfo, Object> itemPair = result.get(0);
        // The item must the same appInfo.
        assertEquals(itemPair.first, appInfo2);
    }

    private AppInfo constructAppInfo(String pkgName, String className) {
        ComponentName cpName = new ComponentName(pkgName, className);
        Intent intent = new Intent();
        AppInfo appInfo = new AppInfo();
        intent.setComponent(cpName);
        appInfo.componentName = cpName;
        appInfo.intent = intent;
        appInfo.user = myUser;
        return appInfo;
    }
}
