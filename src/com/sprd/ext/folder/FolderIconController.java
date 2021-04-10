package com.sprd.ext.folder;

import static com.sprd.ext.LauncherSettingsExtension.PREF_FOLDER_ICON_MODE_KEY;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.view.View;

import com.android.launcher3.CellLayout;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutAndWidgetContainer;
import com.android.launcher3.Utilities;
import com.android.launcher3.Workspace;
import com.android.launcher3.folder.ClippedFolderIconLayoutRule;
import com.android.launcher3.folder.Folder;
import com.android.launcher3.folder.FolderIcon;
import com.sprd.ext.BaseController;
import com.sprd.ext.LauncherAppMonitor;
import com.sprd.ext.LauncherAppMonitorCallback;
import com.sprd.ext.LauncherSettingsExtension;
import com.sprd.ext.LogUtils;
import com.sprd.ext.multimode.MultiModeController;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

/**
 * Created on 2019/5/20.
 */
public class FolderIconController extends BaseController implements Preference.OnPreferenceChangeListener{
    private static final String TAG = "FolderIconController";

    // the "Annular" and "Grid" from the array folder_icon_model_values
    static final String AUTO = "auto";
    static final String ANNULAR = "annular";
    static final String GRID = "grid";

    private String mModel = ANNULAR;
    private String mOldModel;

    private int mNumFolderRows;
    private int mNumFolderColumns;
    private int mGridNumFolderRows;
    private int mGridNumFolderColumns;

    private LauncherAppMonitor mMonitor;
    private GridFolderIconLayoutRule mGridLayoutRule;

    private boolean mShowFolderIconModelSetting;

    private final ArrayList<FolderIconModelListener> mListeners = new ArrayList<>();

    public interface FolderIconModelListener {
        void onFolderIconModelChanged();
    }

    private LauncherAppMonitorCallback mAppMonitorCallback = new LauncherAppMonitorCallback() {
        @Override
        public void onLauncherPreCreate(Launcher launcher) {
            mOldModel = mModel = getFolderIconModelFromPref();
        }

        @Override
        public void onLauncherOrientationChanged() {
            updateFolderRowAndColumns(LauncherAppState.getIDP(mContext));
        }

        @Override
        public void onHomeIntent() {
            if (mMonitor.getLauncher() != null) {
                Folder folder = Folder.getOpen(mMonitor.getLauncher());
                if (folder instanceof GridFolder) {
                    ((GridFolder) folder).setNeedResetState(false);
                }
            }
        }

        @Override
        public void onLauncherResumed() {
            if (!mOldModel.equals(mModel)) {
                mOldModel = mModel;

                if (LogUtils.DEBUG) {
                    LogUtils.d(TAG, "Folder icon model change to " + mModel);
                }
                updateFolderRowAndColumns(LauncherAppState.getIDP(mContext));
                onFolderIconModelChanged();
            }
        }

        @Override
        public void onLauncherDestroy() {
            mListeners.clear();
        }

        @Override
        public void dump(String prefix, FileDescriptor fd, PrintWriter w, boolean dumpAll) {
            w.println();
            w.println(prefix + TAG + ": mModel:" + mModel);
            if (dumpAll) {
                int rows = GRID.equals(mModel) ? mGridNumFolderRows : mNumFolderRows;
                int cols = GRID.equals(mModel) ? mGridNumFolderColumns : mNumFolderColumns;
                w.println(prefix + TAG + ": [rows x cols]:" + rows + "x" + cols +
                        " mShowFolderIconModelSetting:" + mShowFolderIconModelSetting);
            }
        }
    };

    public FolderIconController(Context context, LauncherAppMonitor monitor) {
        super(context);
        mMonitor = monitor;
        mMonitor.registerCallback(mAppMonitorCallback);

        Resources resources = context.getResources();
        mShowFolderIconModelSetting = resources.getBoolean(R.bool.show_folder_icon_model_settings);
        mGridLayoutRule = new GridFolderIconLayoutRule(context);
    }

    String getFolderIconModelFromPref() {
        String defaultModel = mContext.getResources().getString(R.string.default_folder_icon_model);
        String model = Utilities.getPrefs(mContext).getString(PREF_FOLDER_ICON_MODE_KEY, defaultModel);
        return verifyFolderIconModel(model);
    }

    String getFolderIconModel() {
        return mModel;
    }

    void setFolderIconModel(String folderIconMode) {
        mModel = verifyFolderIconModel(folderIconMode);
    }

    private String verifyFolderIconModel(String model) {
        String result;
        switch (model) {
            case ANNULAR:
            case GRID:
                result = model;
                break;
            case AUTO:
                result = MultiModeController.isSingleLayerMode() ? GRID : ANNULAR;
                break;
            default:
                LogUtils.w(TAG, model + "is a wrong mode, will use annular");
                result = ANNULAR;
                break;
        }
        return result;
    }

    public void addListener(FolderIconModelListener listener) {
        mListeners.add(listener);
    }

    public void removeListener(FolderIconModelListener listener) {
        mListeners.remove(listener);
    }

    private void onFolderIconModelChanged() {
        for (int i = 0; i < mListeners.size(); i++) {
            mListeners.get(i).onFolderIconModelChanged();
        }
    }

    public boolean isNativeFolderIcon() {
        return ANNULAR.equals(mModel);
    }

    public boolean isGridFolderIcon() {
        return GRID.equals(mModel);
    }

    public ClippedFolderIconLayoutRule getGridLayoutRule() {
        return mGridLayoutRule;
    }

    private void updateFolderIconsListener(Launcher launcher, boolean add) {
        if (launcher == null) {
            return;
        }

        Workspace workspace = launcher.getWorkspace();
        if (workspace == null) {
            return;
        }

        for (CellLayout cellLayout : workspace.getWorkspaceAndHotseatCellLayouts()) {
            final ShortcutAndWidgetContainer shortcutsAndWidgets = cellLayout.getShortcutsAndWidgets();
            for (int j = shortcutsAndWidgets.getChildCount() - 1; j >= 0; --j) {
                final View child = shortcutsAndWidgets.getChildAt(j);
                if (child instanceof FolderIcon) {
                    if (add) {
                        this.addListener((FolderIcon) child);
                    } else {
                        this.removeListener((FolderIcon) child);
                    }
                 }
            }
        }
    }

    public void updateFolderIconIfModelChanged(Launcher launcher, FolderIcon folderIcon) {
        folderIcon.getFolderInfo().removeListener(folderIcon.getFolder());

        folderIcon.setLayoutRule(isGridFolderIcon() ?
                mGridLayoutRule : new ClippedFolderIconLayoutRule());
        folderIcon.getPreviewItemManager().computePreviewDrawingParamsIfNeeded();

        Folder folder = isGridFolderIcon() ? GridFolder.fromXml(launcher) : Folder.fromXml(launcher);
        folder.setDragController(launcher.getDragController());
        folder.setFolderIcon(folderIcon);
        folder.bind(folderIcon.getFolderInfo());

        folderIcon.setFolder(folder);
        folderIcon.invalidate();
    }

    public void backupOriginalFolderRowAndColumns(int rows, int columns) {
        mNumFolderRows = rows;
        mNumFolderColumns = columns;
    }

    public void updateFolderRowAndColumns(InvariantDeviceProfile inv) {
        if (isGridFolderIcon()) {
            final Resources res = mContext.getResources();
            mGridNumFolderRows = res.getInteger(R.integer.grid_folder_page_rows);
            mGridNumFolderColumns = res.getInteger(R.integer.grid_folder_page_columns);

            if(mGridNumFolderRows != inv.numFolderRows
                    || mGridNumFolderColumns != inv.numFolderColumns) {
                inv.numFolderRows = mGridNumFolderRows;
                inv.numFolderColumns = mGridNumFolderColumns;
            }
        } else {
            if (mNumFolderRows != inv.numFolderRows
                    || mNumFolderColumns != inv.numFolderColumns ) {
                inv.numFolderRows = mNumFolderRows;
                inv.numFolderColumns = mNumFolderColumns;
            }
        }
    }

    public void initPreference(ListPreference preference) {
        if (preference == null) {
            return;
        }

        if (isSupportDynamicChange()) {
            if (mListeners.isEmpty()) {
                updateFolderIconsListener(mMonitor.getLauncher(), true);
            }
        } else {
            PreferenceGroup parent = preference.getParent();
            if (parent != null) {
                parent.removePreference(preference);
            }
            if (!mListeners.isEmpty()) {
                updateFolderIconsListener(mMonitor.getLauncher(), false);
            }
            return;
        }

        final Resources res = mContext.getResources();
        final CharSequence[] entries = res.getTextArray(R.array.folder_icon_model_entries);
        final CharSequence[] values = res.getTextArray(R.array.folder_icon_model_values);
        final CharSequence defaultValue = res.getText(R.string.default_folder_icon_model);

        final boolean isSupportDynamicChange = MultiModeController.isSupportDynamicChange();
        final int entryCount = entries.length + (isSupportDynamicChange ? 1 : 0);
        final List<CharSequence> modelEntries = new ArrayList<>(entryCount);
        final List<CharSequence> modelValues = new ArrayList<>(entryCount);

        if (isSupportDynamicChange) {
            // add auto items
            modelEntries.add(res.getText(R.string.auto_model));
            modelValues.add(AUTO);
        }

        for (int i = 0; i < entries.length; i++) {
            modelEntries.add(entries[i]);
            modelValues.add(values[i]);
        }

        preference.setEntries(modelEntries.toArray(new CharSequence[0]));
        preference.setEntryValues(modelValues.toArray(new CharSequence[0]));

        // init default value
        String value = preference.getValue();
        String model = TextUtils.isEmpty(value) ? defaultValue.toString() : value;
        if (!modelValues.contains(model)) {
            if (modelValues.contains(defaultValue.toString())) {
                model = defaultValue.toString();
            } else {
                model = verifyFolderIconModel(model);
            }
        }
        preference.setValue(model);

        preference.setOnPreferenceChangeListener(this);
    }

    public boolean isSupportDynamicChange() {
        return mShowFolderIconModelSetting && Utilities.isDevelopersOptionsEnabled(mContext);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (LauncherSettingsExtension.sIsUserAMonkey) {
            return false;
        }

        setFolderIconModel((String)newValue);
        return true;
    }
}
