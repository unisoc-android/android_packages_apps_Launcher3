package com.android.launcher3.util.rule;

import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceScreen;
import androidx.test.InstrumentationRegistry;

import com.android.launcher3.settings.SettingsActivity;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.List;

import static androidx.test.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertNotNull;

/**
 * Test rule to get the current Launcher activity.
 **/
public class SettingsActivityRule implements TestRule {

    private SettingsActivity mActivity;

    @Override
    public Statement apply(Statement base, Description description) {
        return new MyStatement(base);
    }

    public SettingsActivity getActivity() {
        return mActivity;
    }


    /**
    * Starts the launcher activity in the target package.
    */
    public void startHomeSetting() {
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_APPLICATION_PREFERENCES);
        intent.setPackage(InstrumentationRegistry.getTargetContext().getPackageName());
        getInstrumentation().startActivitySync(intent);
    }

    public PreferenceScreen getPreferenceScreen() {
        List<Fragment> fragmentList = mActivity.getFragmentManager().getFragments();
        PreferenceFragment mFragment = null;
        if (fragmentList.get(0) instanceof Fragment) {
            mFragment = (PreferenceFragment) fragmentList.get(0);
        }
        assertNotNull(mFragment);

        PreferenceScreen screen = mFragment.getPreferenceScreen();
        assertNotNull(screen);
        return screen;
    }

    public Preference getPreference(String prefKey) {
        PreferenceScreen screen = getPreferenceScreen();
        return screen.findPreference(prefKey);
    }

    private class MyStatement extends Statement implements ActivityLifecycleCallbacks {

        private final Statement mBase;

        public MyStatement(Statement base) {
            mBase = base;
        }

        @Override
        public void evaluate() throws Throwable {
            Application app = (Application)
                    InstrumentationRegistry.getTargetContext().getApplicationContext();
            app.registerActivityLifecycleCallbacks(this);
            try {
                mBase.evaluate();
            } finally {
                app.unregisterActivityLifecycleCallbacks(this);
            }
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle bundle) {
            if (activity instanceof SettingsActivity) {
                mActivity = (SettingsActivity) activity;
            }
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityResumed(Activity activity) {
        }

        @Override
        public void onActivityPaused(Activity activity) {
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            if (activity == mActivity) {
                mActivity = null;
            }
        }
    }
}