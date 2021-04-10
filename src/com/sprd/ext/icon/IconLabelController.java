package com.sprd.ext.icon;

import android.content.Context;

import androidx.preference.Preference;

import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.sprd.ext.BaseController;
import com.sprd.ext.UtilitiesExt;

import static com.android.launcher3.config.BaseFlags.APPLY_CONFIG_AT_RUNTIME;
import static com.sprd.ext.LauncherSettingsExtension.PREF_ICON_LABEL_KEY;

public class IconLabelController extends BaseController implements
        Preference.OnPreferenceChangeListener {
    public final static int MIN_ICON_LABEL_LINE = 1;
    private final static int MAX_ICON_LABEL_LINE = 2;

    private boolean mEnable;

    public IconLabelController(Context context) {
        super(context);
        mEnable = Utilities.getPrefs(context).getBoolean(PREF_ICON_LABEL_KEY,
                context.getResources().getBoolean(R.bool.enable_icon_label_show_double_lines));
    }

    public int getIconLabelLine() {
        return mEnable ? MAX_ICON_LABEL_LINE : MIN_ICON_LABEL_LINE;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean enable = (boolean) newValue;
        if (mEnable != enable) {
            mEnable = enable;
            if (APPLY_CONFIG_AT_RUNTIME.get()) {
                InvariantDeviceProfile.INSTANCE.get(mContext).applyConfigChanged(mContext);
            } else {
                boolean success = Utilities.getPrefs(mContext).edit().
                        putBoolean(PREF_ICON_LABEL_KEY, mEnable).commit();
                if (success) {
                    UtilitiesExt.exitLauncher();
                }
            }
        }
        return true;
    }
}
