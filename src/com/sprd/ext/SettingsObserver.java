package com.sprd.ext;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;

public interface SettingsObserver {

    /**
     * Registers the content observer to call {@link #onSettingChanged(boolean)} when any of the
     * passed settings change. The value passed to onSettingChanged() is based on the key setting.
     */
    void register(String keySetting, String ... dependentSettings);
    void unregister();
    void onSettingChanged(boolean keySettingEnabled);


    abstract class Secure extends ContentObserver implements SettingsObserver {
        private ContentResolver mResolver;
        private String mKeySetting;

        public Secure(ContentResolver resolver) {
            super(new Handler());
            mResolver = resolver;
        }

        @Override
        public void register(String keySetting, String ... dependentSettings) {
            mKeySetting = keySetting;
            mResolver.registerContentObserver(
                    Settings.Secure.getUriFor(mKeySetting), false, this);
            for (String setting : dependentSettings) {
                mResolver.registerContentObserver(
                        Settings.Secure.getUriFor(setting), false, this);
            }
            onChange(true);
        }

        @Override
        public void unregister() {
            mResolver.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            onSettingChanged(Settings.Secure.getInt(mResolver, mKeySetting, 1) == 1);
        }
    }

    abstract class System extends ContentObserver implements SettingsObserver {
        private ContentResolver mResolver;
        private String mKeySetting;

        public System(ContentResolver resolver) {
            super(new Handler());
            mResolver = resolver;
        }

        @Override
        public void register(String keySetting, String ... dependentSettings) {
            mKeySetting = keySetting;
            mResolver.registerContentObserver(
                    Settings.System.getUriFor(mKeySetting), false, this);
            for (String setting : dependentSettings) {
                mResolver.registerContentObserver(
                        Settings.System.getUriFor(setting), false, this);
            }
            onChange(true);
        }

        @Override
        public void unregister() {
            mResolver.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            onSettingChanged(Settings.System.getInt(mResolver, mKeySetting, 1) == 1);
        }
    }

    abstract class Global extends ContentObserver implements SettingsObserver {
        private ContentResolver mResolver;
        private String mKeySetting;

        public Global(ContentResolver resolver) {
            super(new Handler());
            mResolver = resolver;
        }

        @Override
        public void register(String keySetting, String ... dependentSettings) {
            mKeySetting = keySetting;
            mResolver.registerContentObserver(
                    Settings.Global.getUriFor(mKeySetting), false, this);
            for (String setting : dependentSettings) {
                mResolver.registerContentObserver(
                        Settings.Global.getUriFor(setting), false, this);
            }
            onChange(true);
        }

        @Override
        public void unregister() {
            mResolver.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            onSettingChanged(Settings.Global.getInt(mResolver, mKeySetting, 1) == 1);
        }
    }
}
