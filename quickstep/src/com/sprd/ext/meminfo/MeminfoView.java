package com.sprd.ext.meminfo;

import android.content.Context;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Insettable;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.views.ActivityContext;
import com.sprd.ext.FeatureOption;
import com.sprd.ext.LogUtils;
import com.sprd.ext.clearall.ClearAllController;

import static com.android.launcher3.uioverrides.RecentsUiFactory.GO_LOW_RAM_RECENTS_ENABLED;

public class MeminfoView extends TextView implements Insettable {
    private static final String TAG = "MeminfoView";
    private static final String SEPARATOR = " | ";
    private float mContentAlpha = 0f;
    private final ActivityContext mActivity;
    private final Context mContext;

    public static final FloatProperty<MeminfoView> CONTENT_ALPHA =
            new FloatProperty<MeminfoView>("MeminfoView") {
                @Override
                public void setValue(MeminfoView view, float v) {
                    view.setContentAlpha(v);
                }

                @Override
                public Float get(MeminfoView view) {
                    return view.getContentAlpha();
                }
            };

    public MeminfoView(Context context) {
        this(context, null, 0);
    }

    public MeminfoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MeminfoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        mActivity = ActivityContext.lookupContext(context);
        MeminfoHelper.getInstance(context).updateTotalMemory();
    }

    public void setContentAlpha(float alpha) {
        mContentAlpha = alpha;
        setAlpha(alpha);
        if (getVisibility() != VISIBLE && Float.compare(alpha, 0f) > 0) {
            setVisibility(VISIBLE);
        } else if (getVisibility() != GONE && Float.compare(alpha, 0f) == 0) {
            setVisibility(GONE);
        }
    }

    public float getContentAlpha() {
        return mContentAlpha;
    }

    @Override
    public void setInsets(Rect insets) {
        if (LogUtils.DEBUG_ALL) {
            LogUtils.d(TAG, "setInsets insets: " + insets);
        }
        DeviceProfile dp = mActivity.getDeviceProfile();
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
        lp.bottomMargin = insets.bottom;
        lp.leftMargin = insets.left;
        lp.rightMargin = insets.right;
        if (mActivity instanceof Launcher && !dp.isVerticalBarLayout()
                && !GO_LOW_RAM_RECENTS_ENABLED) {
            lp.bottomMargin += dp.hotseatBarSizePx + dp.verticalDragHandleSizePx;
        } else {
            lp.bottomMargin += getContext().getResources()
                    .getDimensionPixelSize(R.dimen.recents_bottom_padding);
        }
        if (ClearAllController.IS_SUPPORT_CLEAR_ALL_ON_BOTTOM) {
            lp.bottomMargin += getContext().getResources()
                    .getDimensionPixelSize(R.dimen.recents_clearall_height);
        }
        setLayoutParams(lp);
    }

    public void setMemoryinfoText(String availStr, String totalStr) {
        if (TextUtils.isEmpty(availStr) || TextUtils.isEmpty(totalStr)) {
            return;
        }
        String text = getContext().getString(R.string.recents_memory_avail, availStr) + SEPARATOR + totalStr;
        setText(text);
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        if (VISIBLE == visibility) {
            MeminfoHelper meminfoHelper = MeminfoHelper.getInstance(mContext);
            meminfoHelper.updateAvailMemory();
            setMemoryinfoText(meminfoHelper.getAvailMemString(), meminfoHelper.getTotalMemString());
        }
        super.onVisibilityChanged(changedView, visibility);
    }
}
