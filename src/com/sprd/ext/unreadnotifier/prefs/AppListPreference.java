package com.sprd.ext.unreadnotifier.prefs;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.android.launcher3.R;
import com.sprd.ext.unreadnotifier.UnreadInfoController;

import java.util.ArrayList;
import java.util.List;

public class AppListPreference extends ListPreference {
    private static final String CHECKBOX_KEY = "_checked";

    public UnreadBaseItemInfo itemInfo;

    private Drawable[] mEntryDrawables;
    private OnPreferenceCheckBoxClickListener mCheckBoxListener;
    private CheckBox mCheckBox;

    public interface OnPreferenceCheckBoxClickListener {
        /**
         * Called when a view has been clicked.
         *
         * @param preference The view that was clicked.
         */
        void onPreferenceCheckboxClick(Preference preference);
    }

    private static class ViewHolder {
        ImageView m_iv;
        TextView m_tv;
        TextView m_tv_select;
    }

    private static class AppArrayAdapter extends ArrayAdapter<CharSequence> {
        private Drawable[] mImageDrawables;
        private int mSelectedIndex;

        AppArrayAdapter(Context context, int textViewResourceId,
                        CharSequence[] objects, Drawable[] imageDrawables, int selectedIndex) {
            super(context, textViewResourceId, objects);
            mSelectedIndex = selectedIndex;
            mImageDrawables = imageDrawables;
        }

        @SuppressLint("InflateParams")
        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null || convertView.getTag() == null) {
                LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
                convertView = inflater.inflate(R.layout.app_preference_item, null);
                viewHolder = new ViewHolder();
                viewHolder.m_iv = convertView.findViewById(R.id.app_image);
                viewHolder.m_tv = convertView.findViewById(R.id.app_label);
                viewHolder.m_tv_select = convertView.findViewById(R.id.select_label);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            viewHolder.m_tv.setText(getItem(position));
            if (position == mSelectedIndex) {
                viewHolder.m_tv_select.setVisibility(View.VISIBLE);
            }
            viewHolder.m_iv.setImageDrawable(mImageDrawables[position]);
            return convertView;
        }
    }

    public AppListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.listpref_widget_checkbox);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setListValues(ArrayList<String> listValues, String defaultPackageName) {
        // Look up all package names in PackageManager. Skip ones we can't find.
        PackageManager pm = getContext().getPackageManager();
        final int entryCount = listValues.size();
        List<CharSequence> applicationNames = new ArrayList<>(entryCount);
        List<CharSequence> validatedNames = new ArrayList<>(entryCount);
        List<Drawable> entryDrawables = new ArrayList<>(entryCount);
        int selectedIndex = -1;
        for (int i = 0; i < listValues.size(); i++) {
            try {
                String listValue = listValues.get(i);
                String packageName = listValue.substring(0, listValue.indexOf("/"));
                ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                applicationNames.add(appInfo.loadLabel(pm));
                validatedNames.add(listValues.get(i));
                entryDrawables.add(appInfo.loadIcon(pm));
                if (TextUtils.equals(appInfo.packageName, defaultPackageName)) {
                    selectedIndex = i;
                }
            } catch (PackageManager.NameNotFoundException e) {
                // Skip unknown packages.
            }
        }
        setEntries(applicationNames.toArray(new CharSequence[0]));
        setEntryValues(
                validatedNames.toArray(new CharSequence[0]));
        mEntryDrawables = entryDrawables.toArray(new Drawable[0]);

        if (selectedIndex != -1) {
            setValueIndex(selectedIndex);
        } else {
            setValue(null);
        }
    }

    private ListAdapter createListAdapter() {
        final String selectedValue = getValue();
        final boolean selectedNone = selectedValue == null;
        int selectedIndex = selectedNone ? -1 : findIndexOfValue(selectedValue);

        return new AppArrayAdapter(getContext(),
                R.layout.app_preference_item, getEntries(), mEntryDrawables, selectedIndex);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mCheckBox = view.findViewById(R.id.checkbox);
        mCheckBox.setChecked(isPreferenceChecked(getContext(), getKey(), false));

        mCheckBox.setOnClickListener(v -> {
            setPreferenceChecked(mCheckBox.isChecked());
            if (mCheckBoxListener != null) {
                mCheckBoxListener.onPreferenceCheckboxClick(AppListPreference.this);
            }

        });
    }

    public void setOnPreferenceCheckBoxClickListener(OnPreferenceCheckBoxClickListener l) {
        mCheckBoxListener = l;
    }

    public void setPreferenceChecked(boolean bool) {
        Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
        editor.putBoolean(getKey() + CHECKBOX_KEY, bool).apply();
        if (mCheckBox != null && mCheckBox.isChecked() != bool) {
            mCheckBox.setChecked(bool);
        }
    }

    static boolean isPreferenceChecked(Context context, String prefKey, boolean defValue) {
        SharedPreferences sharePref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharePref.getBoolean(prefKey + CHECKBOX_KEY, defValue);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        builder.setAdapter(createListAdapter(), this);
        super.onPrepareDialogBuilder(builder);
    }

    public void initItemListValues(UnreadInfoController uc, int type) {
        itemInfo = uc.getUnreadInfoManager().getItemByType(type);
        if (itemInfo != null) {
            String pkgName = "";
            ArrayList<String> listValues = itemInfo.loadApps(itemInfo.mContext);
            itemInfo.setInstalledList(listValues);
            itemInfo.verifyDefaultCN(listValues, itemInfo.defaultCn);
            if (!TextUtils.isEmpty(itemInfo.currentCn)) {
                ComponentName cn = ComponentName.unflattenFromString(itemInfo.currentCn);
                if (cn != null) {
                    pkgName = cn.getPackageName();
                }
            }
            setListValues(listValues, pkgName);
        }
    }
}
