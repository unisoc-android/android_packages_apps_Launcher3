package com.sprd.ext.unreadnotifier;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.View;

import com.android.launcher3.AppInfo;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.R;
import com.android.launcher3.folder.FolderIcon;
import com.sprd.ext.UtilitiesExt;
import com.sprd.ext.notificationdots.NotifyDotsNumUtils;

public class DotDrawUtils {
    private static final float ROUNDRECT_ARC_X = 30.0f;
    private static final float ROUNDRECT_ARC_Y = 30.0f;
    private static final float SIZE_PERCENTAGE = 0.2f;

    private static final int MAX_UNREAD_COUNT = 99;

    private static final String BLANK_STRING = " ";

    private static Paint sBgPaint;
    private static Paint sTextPaint;

    private enum DotMode {LT_DOT, RT_DOT, LB_DOT, RB_DOT}

    static void drawDot(Canvas canvas, View v, String text) {
        drawDot(canvas, v, text, DotMode.RT_DOT);
    }

    private static void drawDot(Canvas canvas, View v, String text, DotMode mode) {
        if (canvas == null || v == null) {
            return;
        }

        // init text
        if (TextUtils.isEmpty(text)) {
            text = BLANK_STRING;
        }

        // init view rect
        Rect vRect = new Rect();
        v.getDrawingRect(vRect);

        // init icon rect
        Rect iconRect = getIconRect(v, vRect);

        // init offset for different container
        Resources res = v.getContext().getResources();
        Point offsetPoint = new Point(0, 0);
        if (v.getTag() instanceof ItemInfo) {
            ItemInfo info = (ItemInfo) v.getTag();
            if (info instanceof AppInfo) {
                offsetPoint.x = (int) res.getDimension(R.dimen.app_dot_margin_x);
                offsetPoint.y = (int) res.getDimension(R.dimen.app_dot_margin_y);
            } else {
                if (info.container == (long) LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                    offsetPoint.x = (int) res.getDimension(R.dimen.hotseat_dot_margin_x);
                    offsetPoint.y = (int) res.getDimension(R.dimen.hotseat_dot_margin_y);
                } else if (info.container == (long) LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                    offsetPoint.x = (int) res.getDimension(R.dimen.workspace_dot_margin_x);
                    offsetPoint.y = (int) res.getDimension(R.dimen.workspace_dot_margin_y);
                } else {
                    offsetPoint.x = (int) res.getDimension(R.dimen.folder_dot_margin_x);
                    offsetPoint.y = (int) res.getDimension(R.dimen.folder_dot_margin_y);
                }
            }
        }

        // init paint
        Paint textPaint = sTextPaint == null ? createTextPaint(iconRect, v.getContext()) : sTextPaint;
        Paint bgPaint = sBgPaint == null ? createBgPaint(v.getContext()) : sBgPaint;

        canvas.save();

        // init text bounds
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        Rect textBounds = new Rect();
        textBounds.left = 0;
        textBounds.top = 0;
        textBounds.right = Math.round(textPaint.measureText(text));
        textBounds.bottom = Math.round(fm.descent - fm.ascent);
        long minHeight = Math.round(Math.sqrt((textBounds.height() * textBounds.height()) << 1));
        if (textBounds.width() <= textBounds.height()) {
            textBounds.right = textBounds.bottom = (int) minHeight;
        } else {
            textBounds.right += Math.round(textPaint.measureText(BLANK_STRING));
            textBounds.bottom = (int) minHeight;
        }

        // draw dot bg
        Rect dotRect = getDotRect(vRect, iconRect, textBounds, mode, offsetPoint);
        if (dotRect.width() == dotRect.height()) {
            canvas.drawCircle(dotRect.centerX(), dotRect.centerY(), (dotRect.width() >> 1), bgPaint);
        } else {
            canvas.drawRoundRect(new RectF(dotRect), ROUNDRECT_ARC_X, ROUNDRECT_ARC_Y, bgPaint);
        }

        // draw dot text
        Point drawPoint = UtilitiesExt.getTextDrawPoint(dotRect, fm);
        canvas.drawText(text, drawPoint.x, drawPoint.y, textPaint);
        UtilitiesExt.drawDebugRect(canvas, dotRect, Color.YELLOW);

        canvas.restore();
    }

    private static Paint createBgPaint(Context context) {
        Resources res = context.getResources();
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(res.getColor(R.color.unread_dot_bg_color));
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setAntiAlias(true);
        return bgPaint;
    }

    private static Paint createTextPaint(Rect iconRect, Context context) {
        Resources res = context.getResources();
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(res.getColor(R.color.unread_dot_text_color));
        textPaint.setTextSize((iconRect.height() * SIZE_PERCENTAGE) - 1);
        Typeface font = Typeface.create("sans-serif", Typeface.BOLD);
        textPaint.setTypeface(font);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);
        return textPaint;
    }

    private static Rect getDotRect(Rect viewRect, Rect iconRect, Rect textBounds, DotMode mode,
                                   Point offset) {
        Rect dotRect = new Rect(textBounds);
        int halfWidth = dotRect.width() >> 1;
        int halfHeight = dotRect.height() >> 1;

        switch (mode) {
            case LT_DOT:
                dotRect.offsetTo(iconRect.left - halfWidth, iconRect.top - halfHeight);
                dotRect.offset(offset.x, offset.y);
                break;
            case RT_DOT:
                dotRect.offsetTo(iconRect.right - halfWidth, iconRect.top - halfHeight);
                dotRect.offset(-offset.x, offset.y);
                break;
            case LB_DOT:
                dotRect.offsetTo(iconRect.left - halfWidth, iconRect.bottom - halfHeight);
                dotRect.offset(offset.x, -offset.y);
                break;
            case RB_DOT:
                dotRect.offsetTo(iconRect.right - halfWidth, iconRect.bottom - halfHeight);
                dotRect.offset(-offset.x, -offset.y);
                break;
            default:
                break;
        }

        Point autoOffset = calcOffset(dotRect, viewRect, mode);
        dotRect.offset(autoOffset.x, autoOffset.y);

        return dotRect;
    }

    private static Point calcOffset(Rect dotRect, Rect viewRect, DotMode mode) {
        Point p = new Point();
        switch (mode) {
            case LT_DOT:
                p.x = dotRect.left < viewRect.left ? viewRect.left - dotRect.left + 1 : 0;
                p.y = dotRect.top < viewRect.top ? viewRect.top - dotRect.top + 1 : 0;
                break;
            case RT_DOT:
                p.x = dotRect.right > viewRect.right ? viewRect.right - dotRect.right - 1 : 0;
                p.y = dotRect.top < viewRect.top ? viewRect.top - dotRect.top + 1 : 0;
                break;
            case LB_DOT:
                p.x = dotRect.left < viewRect.left ? viewRect.left - dotRect.left + 1 : 0;
                p.y = dotRect.bottom > viewRect.bottom ? viewRect.bottom - dotRect.bottom - 1 : 0;
                break;
            case RB_DOT:
                p.x = dotRect.right > viewRect.right ? viewRect.right - dotRect.right - 1 : 0;
                p.y = dotRect.bottom > viewRect.bottom ? viewRect.bottom - dotRect.bottom - 1 : 0;
                break;
            default:
                break;
        }

        return p;
    }

    public static void setBgPaint(final Paint paint) {
        sBgPaint = paint;
    }

    public static void setTextPaint(final Paint paint) {
        sTextPaint = paint;
    }

    /**
     * The grand parent to allow click shadow effect.
     */
    private static Rect getIconRect(View view, final Rect rect) {
        Rect iconRect = new Rect(rect);
        int iconSize = 0;
        if (view instanceof BubbleTextView) {
            iconSize = ((BubbleTextView) view).getIconSize();
        } else if (view instanceof FolderIcon) {
            Launcher launcher = Launcher.getLauncher(view.getContext());
            DeviceProfile grid = launcher.getDeviceProfile();
            iconSize = grid.folderIconSizePx;
        } else {
            return iconRect;
        }
        Point center = new Point(view.getScrollX() + (view.getWidth() >> 1),
                view.getScrollY() + view.getPaddingTop() + (iconSize >> 1));
        iconRect.left = center.x - (iconSize >> 1);
        iconRect.top = center.y - (iconSize >> 1);
        iconRect.right = iconRect.left + iconSize;
        iconRect.bottom = iconRect.top + iconSize;
        return iconRect;
    }

    public static boolean shouldDrawDotCount(View v) {
        boolean drawDotCount = false;
        Object obj = v.getTag();
        if (obj instanceof ItemInfo) {
            ItemInfo info = (ItemInfo) obj;
            if (info.unreadNum > 0 || NotifyDotsNumUtils.showNotifyDotsNum(v.getContext())) {
                drawDotCount = true;
            }
        }
        return drawDotCount;
    }

    /**
     * Draw unread number for the given icon.
     */
    public static void drawDotAndCountIfNeed(Canvas canvas, View icon, int unreadCount) {
        if (icon.getTag() instanceof ItemInfo) {
            ItemInfo info = (ItemInfo) icon.getTag();
            if (unreadCount > 0 && UnreadHelper.isUnreadItemType(info.itemType)) {
                String unreadText;
                if (unreadCount > MAX_UNREAD_COUNT) {
                    unreadText = MAX_UNREAD_COUNT + "+";
                } else {
                    unreadText = Integer.toString(unreadCount);
                }
                drawDot(canvas, icon, unreadText);
            }
        }
    }
}
