package com.sprd.ext.grid;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.text.TextUtils;
import android.util.Xml;

import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.R;
import com.sprd.ext.LogUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class DesktopGridModel {
    private static final String TAG = "DesktopGridModel";

    private static final String CROSS_PREFIX = "x";

    private Context mContext;
    private String mDefaultGridName;

    private List<String> mGridOptionNames = new ArrayList<>();
    private List<String> mGridOptionEntries = new ArrayList<>();

    DesktopGridModel(final Context context) {
        mContext = context;
        mDefaultGridName = context.getString(R.string.default_desktop_grid_name);

        List<String> ignoredGridOptions = Arrays.asList(
                context.getResources().getStringArray(R.array.grid_options_ignored));
        List<InvariantDeviceProfile.GridOption> gridOptions = parseAllGridOptions();
        updateGridOptionNames(gridOptions, ignoredGridOptions);
    }

    private void updateGridOptionNames(
            List<InvariantDeviceProfile.GridOption> gridOptions, List<String> ignored) {
        if (gridOptions == null) {
            return;
        }

        for (InvariantDeviceProfile.GridOption gridOption : gridOptions) {
            String gridOptionName = gridOption.name;
            if (ignored == null || !ignored.contains(gridOptionName)) {
                // Filter the ignored grid option.
                mGridOptionNames.add(gridOptionName);
                StringBuilder builder = new StringBuilder();
                builder.append(gridOption.numColumns).append(CROSS_PREFIX).append(gridOption.numRows);
                mGridOptionEntries.add(builder.toString());
            }
        }

        if (LogUtils.DEBUG) {
            LogUtils.d(TAG, "Grid Options: " + mGridOptionEntries);
        }
    }

    private List<InvariantDeviceProfile.GridOption> parseAllGridOptions() {
        List<InvariantDeviceProfile.GridOption> gridOptions = new ArrayList<>();

        try (XmlResourceParser parser = mContext.getResources().getXml(R.xml.device_profiles)) {
            final int depth = parser.getDepth();
            int type;
            while (((type = parser.next()) != XmlPullParser.END_TAG ||
                    parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
                if ((type == XmlPullParser.START_TAG)
                        && InvariantDeviceProfile.GridOption.TAG_NAME.equals(parser.getName())) {
                    gridOptions.add(new InvariantDeviceProfile.GridOption(mContext, Xml.asAttributeSet(parser)));
                }
            }
        } catch (IOException | XmlPullParserException e) {
            LogUtils.e(TAG, "Error parsing device profile", e);
            return Collections.emptyList();
        }
        return gridOptions;
    }

    List<String> getGridOptionNames() {
        return mGridOptionNames;
    }

    List<String> getGridOptionEntries() {
        return mGridOptionEntries;
    }

    String getDefaultGridName() {
        return mDefaultGridName;
    }
}
