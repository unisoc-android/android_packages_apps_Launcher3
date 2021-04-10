package com.sprd.ext.fuzzysearch;

import android.content.ComponentName;

import com.android.launcher3.AppInfo;
import com.android.launcher3.allapps.search.DefaultAppSearchAlgorithm;
import com.sprd.ext.FeatureOption;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link DefaultAppSearchAlgorithm}
 */
@RunWith(RobolectricTestRunner.class)
public class AppfuzzySearchAlgorithmTest {
    private static final DefaultAppSearchAlgorithm.StringMatcher MATCHER =
            DefaultAppSearchAlgorithm.StringMatcher.getInstance();

    @Before
    public void setup() {
        FeatureOption.SPRD_ALLAPP_FUZZY_SEARCH_SUPPORT.setForTests(true);
    }

    @Test
    public void testMatches() {
        assertTrue(DefaultAppSearchAlgorithm.matches(getInfo("white cow"), "cow", MATCHER, true));
        assertTrue(DefaultAppSearchAlgorithm.matches(getInfo("whiteCow"), "cow", MATCHER, true));
        assertTrue(DefaultAppSearchAlgorithm.matches(getInfo("whiteCOW"), "cow", MATCHER, true));
        assertTrue(DefaultAppSearchAlgorithm.matches(getInfo("whitecowCOW"), "cow", MATCHER, true));
        assertTrue(DefaultAppSearchAlgorithm.matches(getInfo("white2cow"), "cow", MATCHER, true));
        assertTrue(DefaultAppSearchAlgorithm.matches(getInfo("white2cow"), "2", MATCHER, true));

        assertTrue(DefaultAppSearchAlgorithm.matches(getInfo("whitecow"), "cow", MATCHER, true));
        assertTrue(DefaultAppSearchAlgorithm.matches(getInfo("whitEcow"), "cow", MATCHER, true));

        assertTrue(DefaultAppSearchAlgorithm.matches(getInfo("whitecowCow"), "cow", MATCHER, true));
        assertTrue(DefaultAppSearchAlgorithm.matches(getInfo("whitecow cow"), "cow", MATCHER, true));
        assertTrue(DefaultAppSearchAlgorithm.matches(getInfo("whitecowcow"), "cow", MATCHER, true));
        assertTrue(DefaultAppSearchAlgorithm.matches(getInfo("whit ecowcow"), "cow", MATCHER, true));
        assertTrue(DefaultAppSearchAlgorithm.matches(getInfo("whit ecowcow"), " ", MATCHER, true));

        assertTrue(DefaultAppSearchAlgorithm.matches(getInfo("cats&dogs"), "dog", MATCHER, true));
        assertTrue(DefaultAppSearchAlgorithm.matches(getInfo("cats&Dogs"), "dog", MATCHER, true));
        assertTrue(DefaultAppSearchAlgorithm.matches(getInfo("cats&Dogs"), "&", MATCHER, true));

        assertTrue(DefaultAppSearchAlgorithm.matches(getInfo("2+43"), "43", MATCHER, true));
        assertTrue(DefaultAppSearchAlgorithm.matches(getInfo("2+43"), "+", MATCHER, true));
        assertTrue(DefaultAppSearchAlgorithm.matches(getInfo("2+43"), "3", MATCHER, true));

        assertTrue(DefaultAppSearchAlgorithm.matches(getInfo("Q"), "q", MATCHER, true));
        assertTrue(DefaultAppSearchAlgorithm.matches(getInfo("  Q"), "q", MATCHER, true));

        // match lower case words
        assertTrue(DefaultAppSearchAlgorithm.matches(getInfo("elephant"), "e", MATCHER, true));

        assertTrue(DefaultAppSearchAlgorithm.matches(getInfo("电子邮件"), "电", MATCHER, true));
        assertTrue(DefaultAppSearchAlgorithm.matches(getInfo("电子邮件"), "电子", MATCHER, true));
        assertTrue(DefaultAppSearchAlgorithm.matches(getInfo("电子邮件"), "子", MATCHER, true));
        assertTrue(DefaultAppSearchAlgorithm.matches(getInfo("电子邮件"), "邮件", MATCHER, true));


        assertFalse(DefaultAppSearchAlgorithm.matches(getInfo("Bot"), "ba", MATCHER, true));
        assertFalse(DefaultAppSearchAlgorithm.matches(getInfo("bot"), "ba", MATCHER, true));
    }

    private AppInfo getInfo(String title) {
        AppInfo info = new AppInfo();
        info.title = title;
        info.componentName = new ComponentName("Test", title);
        return info;
    }
}
