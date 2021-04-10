package com.sprd.ext.grid;

import android.content.Context;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.Hotseat;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.folder.FolderIcon;
import com.sprd.ext.BaseController;
import com.sprd.ext.LauncherAppMonitor;
import com.sprd.ext.LogUtils;
import com.sprd.ext.UtilitiesExt;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Created by unisoc on 2019/05/29
 */
public class HotseatController extends BaseController {
    private static final String TAG = "HotseatController";

    private static final int INVALID = -1;

    private boolean mDelayClearEmptyGridFlag = false;

    private CellLayout.CellInfo mBackupDragInfo = null;
    private LauncherAppMonitor mMonitor;

    public HotseatController(Context context, LauncherAppMonitor monitor) {
        super(context);
        mMonitor = monitor;
    }

    public boolean isFull(Launcher launcher) {
        if (launcher == null) {
            return false;
        }

        Hotseat hs = launcher.getHotseat();
        int gridCount = getGridCount(launcher);
        for (int i = 0; i < gridCount; i++) {
            int cx = hs.getCellXFromOrder(i);
            int cy = hs.getCellYFromOrder(i);
            if (!hs.isOccupied(cx, cy)) {
                return false;
            }
        }
        return true;
    }

    public boolean clearEmptyGrid(Launcher launcher) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            LogUtils.d(TAG, "clear empty grid only run on ui thread.");
            return false;
        }

        if (launcher == null) {
            LogUtils.d(TAG, "launcher is null, clear empty grid failed.");
            return false;
        }

        if (isFull(launcher)) {
            LogUtils.d(TAG, "there are no empty grid in hotseat, no need to clear.");
            return false;
        }

        if (mDelayClearEmptyGridFlag) {
            LogUtils.d(TAG, "mDelayClearEmptyGridFlag is true, to clear empty when it is false.");
            return false;
        }

        ArrayList<View> views = backupHotseatChildren(launcher);
        if (views.size() == 0) {
            return false;
        }

        Hotseat hs = launcher.getHotseat();
        boolean isLandscape = hs.mHasVerticalHotseat;
        int count = views.size();
        int countX = isLandscape ? 1 : Math.max(count, 1);
        int countY = isLandscape ? Math.max(count, 1) : 1;
        hs.removeAllViews();
        hs.setGridSize(countX, countY);
        for (int i = 0; i < count; i++) {
            if (!addViewToHotseat(launcher, views.get(i), i)) {
                LogUtils.e(TAG, "addViewToHotseat failed, rank:" + i);
            }
        }
        return true;
    }

    public boolean insertEmptyGrid(Launcher launcher, int index) {
        if (launcher == null) {
            LogUtils.d(TAG, "launcher is null, insert empty grid failed.");
            return false;
        }

        if (!canInsert(launcher)) {
            LogUtils.d(TAG, "achieve maximum limit, can not insert empty grid.");
            return false;
        }

        if (!isFull(launcher)) {
            LogUtils.d(TAG, "the hotseat has empty grid, can not insert empty grid.");
            return false;
        }

        ArrayList<View> views = backupHotseatChildren(launcher);
        Hotseat hs = launcher.getHotseat();
        final boolean isLandscape = hs.mHasVerticalHotseat;
        int newCount = views.size() + 1;
        int countX = isLandscape ? 1 : newCount;
        int countY = isLandscape ? newCount : 1;
        hs.removeAllViews();
        hs.setGridSize(countX, countY);
        for (int i = 0; i < views.size(); i++) {
            if (i < index) {
                addViewToHotseat(launcher, views.get(i), i);
            } else {
                addViewToHotseat(launcher, views.get(i), i + 1);
            }
        }
        return true;
    }

    private ArrayList<View> backupHotseatChildren(Launcher launcher) {
        Hotseat hs = launcher.getHotseat();
        int gridCount = getGridCount(launcher);
        ArrayList<View> views = new ArrayList<>();
        for (int i = 0; i < gridCount; i++) {
            int cx = hs.getCellXFromOrder(i);
            int cy = hs.getCellYFromOrder(i);
            View v = hs.getShortcutsAndWidgets().getChildAt(cx, cy);
            if (hs.isOccupied(cx, cy)) {
                if (v != null) {
                    if (LogUtils.DEBUG_ALL) {
                        LogUtils.d(TAG, "backup child:" + i);
                    }
                    views.add(v);
                }
            }
        }
        return views;
    }

    public boolean addViewToHotseat(Launcher launcher, View v, int rank) {
        Hotseat hs = launcher.getHotseat();
        if (v != null && rank < getGridCount(launcher)) {
            int cellX = hs.getCellXFromOrder(rank);
            int cellY = hs.getCellYFromOrder(rank);
            if (v.getTag() instanceof ItemInfo) {
                CellLayout.LayoutParams lp = new CellLayout.LayoutParams(cellX, cellY, 1, 1);
                if (hs.addViewToCellLayout(v, -1, v.getId(), lp, true)) {
                    ((ItemInfo)v.getTag()).screenId = rank;
                    if (LogUtils.DEBUG_ALL) {
                        LogUtils.d(TAG, "update " + ((ItemInfo)v.getTag()).title + " to:" + rank);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public void resetDragCell(Launcher launcher, CellLayout.CellInfo cellInfo) {
        if (cellInfo == null || cellInfo.cell == null) {
            if (cellInfo != null) {
                LogUtils.d(TAG, "cell is null, can not reset cell to original position.");
            }
            return;
        }

        if (cellInfo.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            View v = cellInfo.cell;
            Hotseat hs = launcher.getHotseat();
            int gridCount = getGridCount(launcher);
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) v.getLayoutParams();
            int index = getOrderInHotseat(launcher, lp.cellX, lp.cellY, gridCount + 1);
            if (index >= gridCount) {
                insertEmptyGrid(launcher, gridCount);
            } else {
                int cx = hs.getCellXFromOrder(index);
                int cy = hs.getCellYFromOrder(index);
                if (hs.isOccupied(cx, cy)) {
                    insertEmptyGrid(launcher, index);
                }
            }

            if (addViewToHotseat(launcher, v, index)) {
                v.setVisibility(View.VISIBLE);
                LogUtils.d(TAG, "resetDragCell to :" + index);
            }
        }
    }

    public int calculateBestIndex(Launcher launcher, float[] dragViewVisualCenterint,
                                  int findCellX, int findCellY) {
        Hotseat hs = launcher.getHotseat();
        int index = getOrderInHotseat(launcher, findCellX, findCellY);
        int[] cellCenter = new int[2];
        hs.cellToCenterPoint(findCellX, findCellY, cellCenter);
        boolean isDragInEndArea = hs.mHasVerticalHotseat ?
                dragViewVisualCenterint[1] < cellCenter[1] : dragViewVisualCenterint[0] > cellCenter[0];
        return isDragInEndArea ? index + 1 : index;
    }

    public void resetGridIfNeeded(Launcher launcher, int index) {
        if (launcher == null) {
            return;
        }

        if (!isCellOccupied(launcher)) {
            insertEmptyGrid(launcher, index);
        }
    }

    public boolean isCellOccupied(Launcher launcher) {
        if (launcher == null) {
            return false;
        }

        Hotseat hs = launcher.getHotseat();
        int gridCount = getGridCount(launcher);
        if (gridCount == 1) {
            if (!hs.isOccupied(0, 0)) {
                return true;
            }
        }
        return false;
    }

    public boolean isNeedInterceptHotseatTouch(MotionEvent event, View view) {
        boolean needIntercept = false;
        if (view != null && view.getTag() instanceof ItemInfo) {
            if (((ItemInfo) view.getTag()).container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                int iconSize = 0;
                if (view instanceof FolderIcon) {
                    iconSize = LauncherAppState.getIDP(mContext).getDeviceProfile(mContext).folderIconSizePx;
                } else if (view instanceof BubbleTextView) {
                    iconSize = ((BubbleTextView) view).getIconSize();
                }
                // we'll intercept touch event when touch in blank area in hotseat.
                needIntercept = !UtilitiesExt.getIconRect(iconSize, view)
                        .contains((int) event.getX() + view.getScrollX(),
                                (int) event.getY() + view.getScrollY());
            }
        }
        return needIntercept;
    }

    /* Get the orientation invariant order of the item in the hotseat for persistence. */
    public int getOrderInHotseat(Launcher launcher, int x, int y) {
        return getOrderInHotseat(launcher, x, y, getGridCount(launcher));
    }

    /* Get the orientation invariant order of the item in the hotseat for persistence. */
    public int getOrderInHotseat(Launcher launcher, int x, int y, int gridCount) {
        if (launcher == null) {
            LogUtils.d(TAG, "launcher is null, can not get order in hotseat.");
            return INVALID;
        }

        return launcher.getHotseat().mHasVerticalHotseat ? (gridCount - y - 1) : x;
    }

    public int getGridCount(Launcher launcher) {
        if (launcher == null) {
            LogUtils.d(TAG, "launcher is null, can not get grid count.");
            return INVALID;
        }

        Hotseat hs = launcher.getHotseat();
        return hs.mHasVerticalHotseat ? hs.getCountY() : hs.getCountX();
    }

    public boolean canInsert(Launcher launcher) {
        int gridCount = getGridCount(launcher);
        return gridCount < launcher.getDeviceProfile().inv.numHotseatIcons;
    }

    public void backupDragInfo(CellLayout.CellInfo cellInfo) {
        mBackupDragInfo = cellInfo;
    }

    public CellLayout.CellInfo getBackupDragInfo () {
        return mBackupDragInfo;
    }

    public void setDelayClearEmptyGridFlag(boolean falg) {
        mDelayClearEmptyGridFlag = falg;
    }

    @Override
    public void dumpState(String prefix, FileDescriptor fd, PrintWriter writer, boolean dumpAll) {
        writer.println();
        writer.println(prefix + TAG + ": GridCount:" + getGridCount(mMonitor.getLauncher()) +
                " isFull:" + isFull(mMonitor.getLauncher()));
    }
}
