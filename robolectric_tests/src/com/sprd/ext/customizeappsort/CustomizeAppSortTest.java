package com.sprd.ext.customizeappsort;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.ComponentName;

import com.android.launcher3.model.BaseModelUpdateTaskTestCase;
import com.sprd.ext.FeatureOption;
import com.sprd.ext.LauncherAppMonitor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests for {@link CustomizeAppSortController}
 */
@RunWith(RobolectricTestRunner.class)
public class CustomizeAppSortTest  extends BaseModelUpdateTaskTestCase {

    @Before
    public void initData() throws Exception {
        initializeData("/app_sort_data.txt");
    }

    @Test
    public void testAppSort() {
        FeatureOption.SPRD_ALLAPP_CUSTOMIZE_SUPPORT.setForTests(true);
        LauncherAppMonitor.INSTANCE.initializeForTesting(null);
        CustomizeAppSortController controller = LauncherAppMonitor.getInstance(targetContext)
                .getCustomizeAppSortController();

        assertNotNull(controller);

        controller.setConfigArray(mAppSortConfigs);

        controller.sortApps(allAppsList.data);

        int lastIndex = allAppsList.data.size() - 1;

        assertEquals(new ComponentName("app1", "class2"), allAppsList.data.get(0).componentName);
        assertEquals(new ComponentName("app2", "class2"), allAppsList.data.get(1).componentName);
        assertEquals(new ComponentName("app1", "class1"), allAppsList.data.get(2).componentName);
        assertEquals(new ComponentName("app2", "class1"), allAppsList.data.get(lastIndex).componentName);
    }

}
