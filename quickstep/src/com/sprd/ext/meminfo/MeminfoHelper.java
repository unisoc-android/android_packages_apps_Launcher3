package com.sprd.ext.meminfo;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.format.Formatter;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;

import com.android.launcher3.R;
import com.android.launcher3.util.MainThreadInitializedObject;
import com.sprd.ext.FeatureOption;
import com.sprd.ext.LogUtils;
import com.sprd.ext.SystemPropertiesUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MeminfoHelper {
    private static final String TAG = "Meminfo";
    private static final long INVALID_MEM_SIZE = -1L;
    private static final long PROCESS_REMOVETASKS_DELAY_MS = 200L;
    private static final String CONFIG_RAM_SIZE = "ro.deviceinfo.ram";
    private static final String RAM_SIZE = "ro.boot.ddrsize";
    private static final String DEFAULT_CONFIG = "unconfig";

    private static final MainThreadInitializedObject<MeminfoHelper> INSTANCE =
            new MainThreadInitializedObject<>(MeminfoHelper::new);

    private long mTotalMem = INVALID_MEM_SIZE;
    private long mAvailMem = INVALID_MEM_SIZE;

    private static final int SI_KUNIT = 1000;
    private static final int KUINT = 1024;
    private Context mContext;

    private MeminfoHelper(Context context) {
        mContext = context;
    }

    public static MeminfoHelper getInstance(final Context context) {
        return INSTANCE.get(context.getApplicationContext());
    }

    public void showReleaseMemoryToast(boolean isRemoveView) {
        if (FeatureOption.SPRD_SHOW_CLEAR_MEM_SUPPORT.get()) {
            long availSizeOriginal = mAvailMem != INVALID_MEM_SIZE
                    ? mAvailMem : getSystemAvailMemory();
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                mAvailMem = getSystemAvailMemory();
                long releaseMem = isRemoveView ? mAvailMem - availSizeOriginal : 0;
                if (releaseMem <= 0) {
                    Toast.makeText(mContext,
                            mContext.getString(R.string.recents_nothing_to_clear), Toast.LENGTH_SHORT)
                            .show();
                } else {
                    String release = Formatter.formatShortFileSize(mContext, releaseMem);
                    String avail = Formatter.formatShortFileSize(mContext, mAvailMem);
                    Toast.makeText(mContext,
                            mContext.getString(R.string.recents_clean_finished_toast,
                                    release.toUpperCase(), avail.toUpperCase()), Toast.LENGTH_SHORT)
                            .show();
                }
            }, PROCESS_REMOVETASKS_DELAY_MS);
        }
    }

    void updateTotalMemory() {
        mTotalMem = getSystemTotalMemory();
    }

    void updateAvailMemory() {
        mAvailMem = getSystemAvailMemory();
    }

    String getTotalMemString() {
        if (mTotalMem == INVALID_MEM_SIZE) {
            updateTotalMemory();
        }
        return Formatter.formatShortFileSize(mContext, mTotalMem);
    }

    String getAvailMemString() {
        if (mAvailMem == INVALID_MEM_SIZE) {
            updateAvailMemory();
        }
        return Formatter.formatShortFileSize(mContext, mAvailMem);
    }

    long getSystemAvailMemory() {
        ActivityManager activityManager =
                (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        if (activityManager != null) {
            activityManager.getMemoryInfo(memoryInfo);
        }
        if (LogUtils.DEBUG_ALL) {
            LogUtils.d(TAG, "getSystemAvailMemory:  " + memoryInfo.availMem);
        }
        return memoryInfo.availMem;
    }

    long getSystemTotalMemory() {
        String ramConfig = getConfig(CONFIG_RAM_SIZE);
        if (!DEFAULT_CONFIG.equals(ramConfig)) {
            long configTotalRam = Long.parseLong(ramConfig);
            LogUtils.d(TAG, "config ram to be: " + configTotalRam);
            return configTotalRam;
        }

        ramConfig = getConfig(RAM_SIZE);
        if (DEFAULT_CONFIG.equals(ramConfig)) {
            LogUtils.d(TAG, "property value is:" + ramConfig);
            return INVALID_MEM_SIZE;
        }
        String regEx = "[^0-9]";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(ramConfig);
        ramConfig = m.replaceAll("").trim();
        long ramSize = Long.parseLong(ramConfig);
        return covertUnitsToSI(ramSize);
    }

    /**
     * SI_KUNIT = 1000bytes; KUINT = 1024bytes
     * 512MB = 512 * 1000 * 1000
     * 2048MB = 2048/1024 * 1000 * 1000 * 1000
     * 2000MB = 2000 * 1000 * 1000
     */
    long covertUnitsToSI(long size) {
        if (size > SI_KUNIT && size % KUINT == 0) {
            return size / KUINT * SI_KUNIT * SI_KUNIT * SI_KUNIT;
        }
        return size * SI_KUNIT * SI_KUNIT;
    }

    String getConfig(String key) {
        return SystemPropertiesUtils.get(key, DEFAULT_CONFIG);
    }

    @VisibleForTesting
    public void setAvailMemSize(long size) {
        mAvailMem = size;
    }

    @VisibleForTesting
    public void setTotalMemSize(long size) {
        mTotalMem = size;
    }

    @VisibleForTesting
    public void initializeForTesting(MeminfoHelper helper) {
        INSTANCE.initializeForTesting(helper);
    }
}
