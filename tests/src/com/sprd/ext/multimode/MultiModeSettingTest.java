/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sprd.ext.multimode;

import androidx.preference.Preference;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.launcher3.util.rule.SettingsActivityRule;
import com.sprd.ext.FeatureOption;
import com.sprd.ext.LauncherAppMonitor;
import com.sprd.ext.LauncherSettingsExtension;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class MultiModeSettingTest {

    /**
     * Step 1. New a SettingsActivityRule, Don't repeat with LauncherActivityRule.
     */
    protected final SettingsActivityRule mSettingsActivityMonitor = new SettingsActivityRule();


    /**
     * Step 2. RuleChain a mOrderSensitiveRules, Don't repeat with LauncherActivityRule's.
     */
    @Rule
    public TestRule mOrderSensitiveRules = RuleChain.
            outerRule(mSettingsActivityMonitor);

    @Test
    public void testMultiModeSettingsItem() {
        // Feature close
        FeatureOption.SPRD_MULTI_MODE_SUPPORT.setForTests(false);

        // Init LauncherAppMonitor
        LauncherAppMonitor.INSTANCE.initializeForTesting(null);
        LauncherAppMonitor.getInstance(InstrumentationRegistry.getTargetContext());

        // Start SettingsActivity
        mSettingsActivityMonitor.startHomeSetting();

        // Find Feature item.
        Preference pref = mSettingsActivityMonitor.getPreference(
                LauncherSettingsExtension.PREF_HOME_SCREEN_STYLE_KEY);
        assertNull(pref);
        mSettingsActivityMonitor.getActivity().finish();

        // Feature open
        FeatureOption.SPRD_MULTI_MODE_SUPPORT.setForTests(true);
        LauncherAppMonitor.INSTANCE.initializeForTesting(null);
        LauncherAppMonitor.getInstance(InstrumentationRegistry.getTargetContext());
        mSettingsActivityMonitor.startHomeSetting();
        pref = mSettingsActivityMonitor.getPreference(
                LauncherSettingsExtension.PREF_HOME_SCREEN_STYLE_KEY);
        assertNotNull(pref);

    }
}
