package com.sprd.ext.dynamicicon.clock;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;

import com.android.launcher3.R;
import com.sprd.ext.LogUtils;
import com.sprd.ext.SystemPropertiesUtils;
import com.sprd.ext.dynamicicon.BaseDynamicIcon;
import com.sprd.ext.dynamicicon.DynamicIconSettings;
import com.sprd.ext.dynamicicon.DynamicIconUtils;

import java.util.Calendar;

public class OriginalClockIcon extends BaseDynamicIcon {
    private static final String TAG = "OriginalClockIcon";

    // It will reduce the system performance when showing the second hand in dynamic clock icon.
    // So make IS_SHOW_SECOND to be false in general.
    private static final boolean IS_SHOW_SECOND = SystemPropertiesUtils.getBoolean(
            "ro.launcher.dyclock.second", false);
    private static final boolean DBG = IS_SHOW_SECOND ?
            LogUtils.DEBUG_DYNAMIC_ICON_ALL : LogUtils.DEBUG_DYNAMIC_ICON;

    private Runnable mSecondTick;

    public OriginalClockIcon(Context context, String pkg) {
        super(context, pkg);
        mIcon = new DreamClock(context);

        mPreDynamic = DynamicIconUtils.getAppliedValue(context, mPkg,
                context.getResources().getBoolean(R.bool.dynamic_clock_default_state));
        mCurDynamic = DynamicIconUtils.getAppliedValue(context,
                DynamicIconSettings.PREF_KEY_ORIGINAL_CLOCK,
                context.getResources().getBoolean(R.bool.dynamic_clock_default_state));

        if (IS_SHOW_SECOND) {
            mSecondTick = new Runnable() {
                @Override
                public void run() {
                    if (mRegistered) {
                        if (DBG) {
                            LogUtils.d(TAG, "start update icon in second handler");

                        }
                        updateUI();
                        mHandler.postAtTime(this, SystemClock.uptimeMillis() / 1000 * 1000 + 1000);
                    }
                }
            };
        }
        mFilter.addAction(Intent.ACTION_TIME_TICK);
        mFilter.addAction(Intent.ACTION_TIME_CHANGED);
        mFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver();
    }

    @Override
    protected void onReceiverRegistered() {
        if (IS_SHOW_SECOND) {
            mHandler.removeCallbacks(mSecondTick);
            mHandler.postAtTime(mSecondTick,
                    SystemClock.uptimeMillis() / 1000 * 1000 + 1000);
        }

        super.onReceiverRegistered();
    }

    @Override
    protected void onReceiverUnregistered() {
        if (IS_SHOW_SECOND) {
            mHandler.removeCallbacks(mSecondTick);
        }

        super.onReceiverUnregistered();
    }

    @Override
    public String getIconContentNeedShown() {
        StringBuilder str = new StringBuilder();
        str.append(DynamicIconUtils.timeOfField(Calendar.HOUR))
                .append(DynamicIconUtils.timeOfField(Calendar.MINUTE));
        return str.toString();
    }

    private class DreamClock extends DynamicDrawable {
        private Drawable mBgDrawable;
        private Drawable mCircleDrawable;
        private Drawable mHourDrawable;
        private Drawable mMinuteDrawable;
        private Drawable mSecondDrawable;

        private DreamClock(Context context) {
            super(context);

            mBgDrawable = context.getDrawable(R.drawable.ic_dial_plate);
            mCircleDrawable = context.getDrawable(R.drawable.ic_dial_circle);
            mHourDrawable = context.getDrawable(R.drawable.ic_dial_hour_hand);
            mMinuteDrawable = context.getDrawable(R.drawable.ic_dial_minute_hand);
            mSecondDrawable = context.getDrawable(R.drawable.ic_dial_minute_hand);
        }

        @Override
        public Drawable create(final int width, final int height) {
            final Canvas canvas = new Canvas();
            final Bitmap bitmap = Bitmap.createBitmap(width, width,
                    Bitmap.Config.ARGB_8888);
            canvas.setBitmap(bitmap);

            int hour = DynamicIconUtils.timeOfField(Calendar.HOUR);
            int minute = DynamicIconUtils.timeOfField(Calendar.MINUTE);
            int second = DynamicIconUtils.timeOfField(Calendar.SECOND);
            StringBuilder str = new StringBuilder();
            str.append(hour).append(minute);
            mContent = str.toString();

            float minutef = minute + second / 60.0f;
            float hourf = hour + minutef / 60.0f;

            float angleSecond = second / 60.0f * 360.0f - 90;
            float angleMinute = minutef / 60.0f * 360.0f - 90;
            float angleHour = hourf / 12.0f * 360.0f - 90;

            int centerX = width / 2;
            int centerY = height / 2;
            Rect rectDest = new Rect(0, 0, width, height);

            final Rect sOldBounds = new Rect();
            sOldBounds.set(mBgDrawable.getBounds());
            mBgDrawable.setBounds(rectDest);
            mBgDrawable.draw(canvas);
            mBgDrawable.setBounds(sOldBounds);

            sOldBounds.set(mHourDrawable.getBounds());
            mHourDrawable.setBounds(rectDest);
            canvas.save();
            canvas.rotate(angleHour, centerX, centerY);
            mHourDrawable.draw(canvas);
            canvas.restore();
            mHourDrawable.setBounds(sOldBounds);

            sOldBounds.set(mMinuteDrawable.getBounds());
            mMinuteDrawable.setBounds(rectDest);
            canvas.save();
            canvas.rotate(angleMinute, centerX, centerY);
            mMinuteDrawable.draw(canvas);
            canvas.restore();
            mMinuteDrawable.setBounds(sOldBounds);

            if (IS_SHOW_SECOND) {
                sOldBounds.set(mSecondDrawable.getBounds());
                mSecondDrawable.setBounds(rectDest);
                canvas.save();
                canvas.rotate(angleSecond, centerX, centerY);
                mSecondDrawable.draw(canvas);
                canvas.restore();
                mSecondDrawable.setBounds(sOldBounds);
            }

            sOldBounds.set(mCircleDrawable.getBounds());
            mCircleDrawable.setBounds(rectDest);
            mCircleDrawable.draw(canvas);
            mCircleDrawable.setBounds(sOldBounds);

            canvas.setBitmap(null);

            BitmapDrawable foreground = new BitmapDrawable(mRes, bitmap);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (mBackground == null) {
                    mBackground = getBackgroundDrawable(mPkg, mContext);
                }
                return new AdaptiveIconDrawable(mBackground, foreground);
            } else {
                return foreground;
            }
        }
    }
}
