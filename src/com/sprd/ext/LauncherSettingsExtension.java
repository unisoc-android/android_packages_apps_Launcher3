package com.sprd.ext;

import android.app.ActivityManager;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.SwitchPreference;

import com.android.launcher3.R;
import com.android.launcher3.SessionCommitReceiver;
import com.android.launcher3.Utilities;
import com.sprd.ext.cyclescroll.CycleScrollController;
import com.sprd.ext.dynamicicon.DynamicIconController;
import com.sprd.ext.folder.FolderIconController;
import com.sprd.ext.gestures.GesturesController;
import com.sprd.ext.grid.DesktopGridController;
import com.sprd.ext.icon.IconLabelController;
import com.sprd.ext.multimode.MultiModeController;
import com.sprd.ext.notificationdots.NotifyDotsNumController;
import com.sprd.ext.unreadnotifier.UnreadInfoController;

/**
 * Created by SPRD on 6/15/17.
 */

public class LauncherSettingsExtension {

    public static final String PREF_ENABLE_MINUS_ONE = "pref_enable_minus_one";
    private static final String PREF_KEY_DYNAMICICON = "pref_dynamicIcon";
    private static final String PREF_KEY_UNREAD = "pref_unread";
    public static final String PREF_NOTIFICATION_DOTS_EXT = "pref_notification_dots_ext";
    public static final String PREF_ONEFINGER_PULLDOWN = "pref_pulldown_action";
    public static final String PREF_CIRCULAR_SLIDE_KEY = "pref_circular_slide_switch";
    public static final String PREF_HOME_SCREEN_STYLE_KEY = "pref_home_screen_style";
    public static final String PREF_FOLDER_ICON_MODE_KEY = "pref_folder_icon_model";
    public static final String PREF_DESKTOP_GRID_KEY = "pref_desktop_grid";
    public static final String PREF_ICON_LABEL_KEY = "pref_icon_label_line";

    private PreferenceFragment mFragment;
    private final LauncherAppMonitor mMonitor;

    public static boolean sIsUserAMonkey = false;

    public LauncherSettingsExtension(PreferenceFragment fragment) {
        mFragment = fragment;
        mMonitor = LauncherAppMonitor.getInstance(mFragment.getActivity());
    }

    public void initPreferences(Bundle savedInstanceState) {
        SwitchPreference addIconPref = (SwitchPreference) mFragment.findPreference(
                SessionCommitReceiver.ADD_ICON_PREFERENCE_KEY);
        if (MultiModeController.isSingleLayerMode()) {
            // Need Remove add icon to home preference in case Single layer mode
            mFragment.getPreferenceScreen().removePreference(addIconPref);
        } else {
            // If add icon Preference item exist. Add listener to set user action flag.
            addIconPref.setOnPreferenceChangeListener((preference, newValue) -> {
                SharedPreferences prefs = Utilities.getPrefs(preference.getContext());
                if (!prefs.contains(SessionCommitReceiver.ADD_ICON_PREFERENCE_INITIALIZED_KEY)) {
                    prefs.edit()
                            .putBoolean(SessionCommitReceiver.ADD_ICON_PREFERENCE_INITIALIZED_KEY, true)
                            .apply();
                }
                return true;
            });
        }

        mFragment.addPreferencesFromResource(R.xml.launcher_preferences_extension);

        /* add for dynamic icon */
        Preference dynamicPref = mFragment.findPreference(PREF_KEY_DYNAMICICON);
        DynamicIconController dic = mMonitor.getDynamicIconController();
        if (dic != null && dic.hasDynamicIcon()) {
            dynamicPref.setOnPreferenceClickListener(dic);
        } else {
            mFragment.getPreferenceScreen().removePreference(dynamicPref);
        }

        /* add for unread info */
        Preference unreadPreference = mFragment.findPreference(PREF_KEY_UNREAD);
        UnreadInfoController uic = mMonitor.getUnreadInfoController();
        if (uic != null) {
            unreadPreference.setOnPreferenceClickListener(uic);
        } else {
            mFragment.getPreferenceScreen().removePreference(unreadPreference);
        }

        /* add for notification dots num */
        Preference notifyDotsPrefExt = mFragment.findPreference(PREF_NOTIFICATION_DOTS_EXT);
        NotifyDotsNumController ndnc = mMonitor.getNotifiDotsNumController();
        if (null != ndnc) {
            ndnc.initPreference(notifyDotsPrefExt);
        } else {
            mFragment.getPreferenceScreen().removePreference(notifyDotsPrefExt);
        }

        /* add for single layer launcher model */
        Preference slPreference = mFragment.findPreference(PREF_HOME_SCREEN_STYLE_KEY);
        MultiModeController mmc = mMonitor.getMultiModeController();
        if (mmc != null && MultiModeController.isSupportDynamicChange()) {
            slPreference.setOnPreferenceChangeListener(mmc);
        } else {
            mFragment.getPreferenceScreen().removePreference(slPreference);
        }

        /* add for one finger pull down action */
        ListPreference pullDownGesturePref = (ListPreference) mFragment.findPreference(PREF_ONEFINGER_PULLDOWN);
        GesturesController gc = mMonitor.getGesturesController();
        if (gc != null) {
            pullDownGesturePref.setOnPreferenceChangeListener(gc);
        } else {
            mFragment.getPreferenceScreen().removePreference(pullDownGesturePref);
        }

        /* add for circle slide */
        SwitchPreference cycleScrollPref = (SwitchPreference) mFragment.findPreference(PREF_CIRCULAR_SLIDE_KEY);
        CycleScrollController csc = mMonitor.getCycleScrollController();
        if (csc != null) {
            csc.setPref(cycleScrollPref);
        } else {
            mFragment.getPreferenceScreen().removePreference(cycleScrollPref);
        }

        /* add for folder icon mode */
        ListPreference folderIconPref = (ListPreference) mFragment.findPreference(PREF_FOLDER_ICON_MODE_KEY);
        FolderIconController fic = mMonitor.getFolderIconController();
        if (fic != null) {
            fic.initPreference(folderIconPref);
        } else {
            mFragment.getPreferenceScreen().removePreference(folderIconPref);
        }

        /* Add for the desktop grid */
        ListPreference desktopGridPref = (ListPreference) mFragment.findPreference(PREF_DESKTOP_GRID_KEY);
        DesktopGridController dgc = mMonitor.getDesktopGridController();
        if (dgc != null) {
            dgc.initPreference(desktopGridPref);
        } else {
            mFragment.getPreferenceScreen().removePreference(desktopGridPref);
        }

        SwitchPreference iconLabelPref  = (SwitchPreference) mFragment.findPreference(PREF_ICON_LABEL_KEY);
        IconLabelController ilc = mMonitor.getIconLabelController();
        if (ilc != null) {
            iconLabelPref.setOnPreferenceChangeListener(ilc);
        } else {
            mFragment.getPreferenceScreen().removePreference(iconLabelPref);
        }
    }

    public void onLauncherSettingStart() {
        sIsUserAMonkey = ActivityManager.isUserAMonkey();
    }
}
