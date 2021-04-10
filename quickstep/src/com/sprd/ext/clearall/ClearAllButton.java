package com.sprd.ext.clearall;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Insettable;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.views.ActivityContext;
import com.sprd.ext.LogUtils;

import static com.android.launcher3.uioverrides.RecentsUiFactory.GO_LOW_RAM_RECENTS_ENABLED;


public class ClearAllButton extends FrameLayout implements Insettable {
    private static final String TAG = "ClearAllButton";
    protected final ActivityContext mActivity;
    private float mContentAlpha = 0f;
    private Button mButton;

    public static final FloatProperty<ClearAllButton> CONTENT_ALPHA =
            new FloatProperty<ClearAllButton>("ClearAllButton") {
                @Override
                public void setValue(ClearAllButton view, float v) {
                    view.setContentAlpha(v);
                }

                @Override
                public Float get(ClearAllButton view) {
                    return view.getContentAlpha();
                }
            };

    public ClearAllButton(Context context) {
        this(context, null, 0);
    }

    public ClearAllButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ClearAllButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mActivity = ActivityContext.lookupContext(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mButton = findViewById(R.id.clear_all_button);
    }

    public void setContentAlpha(float alpha) {
        mContentAlpha = alpha;
        setClickable(Float.compare(alpha, 1f) == 0);
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

        setLayoutParams(lp);
    }

    public void setClearAllOnClickListener(View.OnClickListener listener) {
        if (mButton != null) {
            mButton.setOnClickListener(listener);
        }
    }
}
