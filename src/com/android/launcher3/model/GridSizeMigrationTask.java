package com.android.launcher3.model;

import static com.android.launcher3.LauncherProvider.SCHEMA_VERSION;
import static com.android.launcher3.LauncherSettings.Settings.EXTRA_VALUE;
import static com.android.launcher3.Utilities.getPointString;
import static com.android.launcher3.Utilities.parsePoint;

import android.annotation.SuppressLint;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;
import android.util.SparseArray;

import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherAppWidgetProviderInfo;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.LauncherSettings.Favorites;
import com.android.launcher3.LauncherSettings.Settings;
import com.android.launcher3.Utilities;
import com.android.launcher3.Workspace;
import com.android.launcher3.compat.AppWidgetManagerCompat;
import com.android.launcher3.compat.PackageInstallerCompat;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.provider.LauncherDbUtils;
import com.android.launcher3.provider.LauncherDbUtils.SQLiteTransaction;
import com.android.launcher3.util.GridOccupancy;
import com.android.launcher3.util.IntArray;
import com.android.launcher3.util.IntSparseArrayMap;
import com.sprd.ext.FeatureOption;
import com.sprd.ext.LogUtils;
import com.sprd.ext.UtilitiesExt;
import com.sprd.ext.multimode.MultiModeController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

import androidx.annotation.VisibleForTesting;

/**
 * This class takes care of shrinking the workspace (by maximum of one row and one column), as a
 * result of restoring from a larger device or device density change.
 */
public class GridSizeMigrationTask {

    private static final String TAG = "GridSizeMigrationTask";
    private static final boolean DEBUG = true;

    private static final String KEY_MIGRATION_SRC_WORKSPACE_SIZE = "migration_src_workspace_size";
    private static final String KEY_MIGRATION_SRC_HOTSEAT_COUNT = "migration_src_hotseat_count";
    private static final String KEY_MIGRATION_SRC_DENSITY_DPI = "migration_src_density_dpi";

    // These are carefully selected weights for various item types (Math.random?), to allow for
    // the least absurd migration experience.
    private static final float WT_SHORTCUT = 1;
    private static final float WT_APPLICATION = 0.8f;
    private static final float WT_WIDGET_MIN = 2;
    private static final float WT_WIDGET_FACTOR = 0.6f;
    private static final float WT_FOLDER_FACTOR = 0.5f;

    protected final SQLiteDatabase mDb;
    protected final Context mContext;

    protected final IntArray mEntryToRemove = new IntArray();
    protected final ArrayList<DbEntry> mCarryOver = new ArrayList<>();

    private final SparseArray<ContentValues> mUpdateOperations = new SparseArray<>();
    private final HashSet<String> mValidPackages;

    private final int mSrcX, mSrcY;
    private int mTrgX, mTrgY;
    private final boolean mShouldRemoveX, mShouldRemoveY;

    private final int mSrcHotseatSize;
    private final int mDestHotseatSize;

    private final HashMap<DbEntry, Point> mSpanUpdatedWidgets = new HashMap<>();
    private final ArrayList<DbEntry> mRepositionEntries = new ArrayList<>();

    @SuppressWarnings("ComparatorNotSerializable")
    private static class PositionComparator implements Comparator<ItemInfo> {
        public int compare(ItemInfo left, ItemInfo right) {
            if (left.cellY == right.cellY) {
                return left.cellX - right.cellX;
            } else {
                return left.cellY - right.cellY;
            }
        }
    }

    protected GridSizeMigrationTask(Context context, SQLiteDatabase db,
            HashSet<String> validPackages, Point sourceSize, Point targetSize) {
        mContext = context;
        mDb = db;
        mValidPackages = validPackages;

        mSrcX = sourceSize.x;
        mSrcY = sourceSize.y;

        mTrgX = targetSize.x;
        mTrgY = targetSize.y;

        mShouldRemoveX = mTrgX < mSrcX;
        mShouldRemoveY = mTrgY < mSrcY;

        // Non-used variables
        mSrcHotseatSize = mDestHotseatSize = -1;
    }

    protected GridSizeMigrationTask(Context context, SQLiteDatabase db,
            HashSet<String> validPackages, int srcHotseatSize, int destHotseatSize) {
        mContext = context;
        mDb = db;
        mValidPackages = validPackages;

        mSrcHotseatSize = srcHotseatSize;

        mDestHotseatSize = destHotseatSize;

        // Non-used variables
        mSrcX = mSrcY = mTrgX = mTrgY = -1;
        mShouldRemoveX = mShouldRemoveY = false;
    }

    public void setTargetSize(Point targetSize) {
        mTrgX = targetSize.x;
        mTrgY = targetSize.y;
    }

    protected boolean migrateItemsFormHotseatToWorkspace() throws Exception {
        if (mCarryOver.isEmpty()) {
            return false;
        }

        mUpdateOperations.clear();
        mEntryToRemove.clear();

        IntArray allScreens = getWorkspaceScreenIds(mDb);
        if (allScreens.isEmpty()) {
            throw new Exception("Unable to get workspace screens");
        }

        int index = 0;
        int screenCount = allScreens.size();
        while (!mCarryOver.isEmpty()) {
            int screenId = index < screenCount ? allScreens.get(index) :
                    LauncherSettings.Settings.call(mContext.getContentResolver(),
                    LauncherSettings.Settings.METHOD_NEW_SCREEN_ID)
                    .getInt(EXTRA_VALUE);
            migrateItemsToWorkspaceScreen(screenId);
            index++;
        }

        return applyOperations();
    }

    private void migrateItemsToWorkspaceScreen(int screenId) {
        int startY = (FeatureFlags.QSB_ON_FIRST_SCREEN && screenId == Workspace.FIRST_SCREEN_ID)
                ? 1 : 0;

        // init the grid occupancy
        GridOccupancy occupied = new GridOccupancy(mTrgX, mTrgY);
        occupied.markCells(0, 0, mTrgX, startY, true);
        ArrayList<DbEntry> items = loadWorkspaceEntries(screenId);
        for (DbEntry item : items) {
            occupied.markCells(item, true);
        }

        for (int y = startY; y < mTrgY; y++) {
            int x = 0;
            for (; x < mTrgX; x++) {
                if (!occupied.cells[x][y]) {
                    DbEntry entry = mCarryOver.remove(0);
                    entry.cellX = x;
                    entry.cellY = y;
                    entry.updateContainerAndScreenId(Favorites.CONTAINER_DESKTOP, screenId);
                    update(entry);
                    if (DEBUG) {
                        LogUtils.d(TAG, "Migrate to workspace: [" + entry.screenId + ", "
                                + entry.cellX + ", " + entry.cellY + "].");
                    }

                    if (mCarryOver.isEmpty()) {
                        break;
                    }
                }
            }
            if (x < mTrgX) break;
        }
    }

    /**
     * Applied all the pending DB operations
     *
     * @return true if any DB operation was commited.
     */
    private boolean applyOperations() throws Exception {
        // Update items
        int updateCount = mUpdateOperations.size();
        for (int i = 0; i < updateCount; i++) {
            mDb.update(Favorites.TABLE_NAME, mUpdateOperations.valueAt(i),
                    "_id=" + mUpdateOperations.keyAt(i), null);
        }

        if (!mEntryToRemove.isEmpty()) {
            if (DEBUG) {
                Log.d(TAG, "Removing items: " + mEntryToRemove.toConcatString());
            }
            mDb.delete(Favorites.TABLE_NAME, Utilities.createDbSelectionQuery(
                    Favorites._ID, mEntryToRemove), null);
        }

        return updateCount > 0 || !mEntryToRemove.isEmpty();
    }

    /**
     * To migrate hotseat, we load all the entries in order (LTR or RTL) and arrange them
     * in the order in the new hotseat while keeping an empty space for all-apps. If the number of
     * entries is more than what can fit in the new hotseat, we drop the entries with least weight.
     * For weight calculation {@see #WT_SHORTCUT}, {@see #WT_APPLICATION}
     * & {@see #WT_FOLDER_FACTOR}.
     *
     * @return true if any DB change was made
     */
    protected boolean migrateHotseat() throws Exception {
        ArrayList<DbEntry> items = loadHotseatEntries();
        while (items.size() > mDestHotseatSize) {
            // Pick the center item by default.
            DbEntry toRemove = items.get(items.size() / 2);

            // Find the item with least weight.
            for (DbEntry entry : items) {
                if (entry.weight < toRemove.weight) {
                    toRemove = entry;
                }
            }

            if (FeatureOption.SPRD_DESKTOP_GRID_SUPPORT.get()) {
                mCarryOver.add(toRemove);
            } else {
                mEntryToRemove.add(toRemove.id);
            }
            items.remove(toRemove);
        }

        // Update screen IDS
        int newScreenId = 0;
        for (DbEntry entry : items) {
            if (entry.screenId != newScreenId) {
                entry.screenId = newScreenId;

                // These values does not affect the item position, but we should set them
                // to something other than -1.
                entry.cellX = newScreenId;
                entry.cellY = 0;

                update(entry);
            }

            newScreenId++;
        }

        return applyOperations();
    }

    @VisibleForTesting
    static IntArray getWorkspaceScreenIds(SQLiteDatabase db) {
        IntArray allScreens = LauncherDbUtils.queryIntArray(db, Favorites.TABLE_NAME, Favorites.SCREEN,
                Favorites.CONTAINER + " = " + Favorites.CONTAINER_DESKTOP,
                Favorites.SCREEN, Favorites.SCREEN);

        // Make sure that the default screen(id: Workspace.FIRST_SCREEN_ID) is always contained.
        if (!allScreens.contains(Workspace.FIRST_SCREEN_ID)) {
            allScreens.add(0, Workspace.FIRST_SCREEN_ID);
        }
        return allScreens;
    }

    /**
     * @return true if any DB change was made
     */
    protected boolean migrateWorkspace() throws Exception {
        IntArray allScreens = getWorkspaceScreenIds(mDb);
        if (allScreens.isEmpty()) {
            throw new Exception("Unable to get workspace screens");
        }

        for (int i = 0; i < allScreens.size(); i++) {
            int screenId = allScreens.get(i);
            if (DEBUG) {
                Log.d(TAG, "Migrating " + screenId);
            }
            migrateScreen(screenId);
        }

        if (!mRepositionEntries.isEmpty()) {
            mCarryOver.addAll(mRepositionEntries);
            mRepositionEntries.clear();
        }
        if (!mCarryOver.isEmpty()) {
            IntSparseArrayMap<DbEntry> itemMap = new IntSparseArrayMap<>();
            for (DbEntry e : mCarryOver) {
                itemMap.put(e.id, e);
            }

            do {
                // Some items are still remaining. Try adding a few new screens.

                // At every iteration, make sure that at least one item is removed from
                // {@link #mCarryOver}, to prevent an infinite loop. If no item could be removed,
                // break the loop and abort migration by throwing an exception.
                OptimalPlacementSolution placement = new OptimalPlacementSolution(
                        new GridOccupancy(mTrgX, mTrgY), deepCopy(mCarryOver), 0, true);
                placement.find();
                if (placement.finalPlacedItems.size() > 0) {
                    int newScreenId = LauncherSettings.Settings.call(
                            mContext.getContentResolver(),
                            LauncherSettings.Settings.METHOD_NEW_SCREEN_ID)
                            .getInt(EXTRA_VALUE);
                    for (DbEntry item : placement.finalPlacedItems) {
                        if (!mCarryOver.remove(itemMap.get(item.id))) {
                            throw new Exception("Unable to find matching items");
                        }
                        item.screenId = newScreenId;
                        update(item);
                    }
                } else {
                    throw new Exception("None of the items can be placed on an empty screen");
                }

            } while (!mCarryOver.isEmpty());
        }
        return applyOperations();
    }

    /**
     * Migrate a particular screen id.
     * Strategy:
     *   1) For all possible combinations of row and column, pick the one which causes the least
     *      data loss: {@link #tryRemove(int, int, int, ArrayList, float[])}
     *   2) Maintain a list of all lost items before this screen, and add any new item lost from
     *      this screen to that list as well.
     *   3) If all those items from the above list can be placed on this screen, place them
     *      (otherwise they are placed on a new screen).
     */
    protected void migrateScreen(int screenId) {
        // If we are migrating the first screen, do not touch the first row.
        int startY = (FeatureFlags.QSB_ON_FIRST_SCREEN && screenId == Workspace.FIRST_SCREEN_ID)
                ? 1 : 0;

        ArrayList<DbEntry> items = loadWorkspaceEntries(screenId);
        if (LogUtils.DEBUG_LOADER) {
            LogUtils.d(TAG, "migrateScreen: mShouldRemoveX = " + mShouldRemoveX + ", mShouldRemoveY = "
                    + mShouldRemoveY + ", mSpanUpdatedWidgets.size() = " + mSpanUpdatedWidgets.size()
                    + ", mRepositionEntries.size() = " + mRepositionEntries.size());
        }
        if (mSpanUpdatedWidgets.size() > 0 || mRepositionEntries.size() > 0) {
            // Some widgets spans had changed, So update them.
            updateWidgetSpans(startY, screenId, items);
        }
        if (!mShouldRemoveX && !mShouldRemoveY) {
            // The source size is smaller than the target one. Just return.
            return;
        }

        int removedCol = Integer.MAX_VALUE;
        int removedRow = Integer.MAX_VALUE;

        // removeWt represents the cost function for loss of items during migration, and moveWt
        // represents the cost function for repositioning the items. moveWt is only considered if
        // removeWt is same for two different configurations.
        // Start with Float.MAX_VALUE (assuming full data) and pick the configuration with least
        // cost.
        float removeWt = Float.MAX_VALUE;
        float moveWt = Float.MAX_VALUE;
        float[] outLoss = new float[2];
        ArrayList<DbEntry> finalItems = null;

        // Try removing all possible combinations
        for (int x = 0; x < mSrcX; x++) {
            // Try removing the rows first from bottom. This keeps the workspace
            // nicely aligned with hotseat.
            for (int y = mSrcY - 1; y >= startY; y--) {
                // Use a deep copy when trying out a particular combination as it can change
                // the underlying object.
                ArrayList<DbEntry> itemsOnScreen = tryRemove(x, y, startY, deepCopy(items),
                        outLoss);

                if ((outLoss[0] < removeWt) || ((outLoss[0] == removeWt) && (outLoss[1]
                        < moveWt))) {
                    removeWt = outLoss[0];
                    moveWt = outLoss[1];
                    removedCol = mShouldRemoveX ? x : removedCol;
                    removedRow = mShouldRemoveY ? y : removedRow;
                    finalItems = itemsOnScreen;
                }

                // No need to loop over all rows, if a row removal is not needed.
                if (!mShouldRemoveY) {
                    break;
                }
            }

            if (!mShouldRemoveX) {
                break;
            }
        }

        if (DEBUG) {
            Log.d(TAG, String.format("Removing row %d, column %d on screen %d",
                    removedRow, removedCol, screenId));
        }

        IntSparseArrayMap<DbEntry> itemMap = new IntSparseArrayMap<>();
        for (DbEntry e : deepCopy(items)) {
            itemMap.put(e.id, e);
        }

        for (DbEntry item : finalItems) {
            DbEntry org = itemMap.get(item.id);
            itemMap.remove(item.id);

            // Check if update is required
            if (!item.columnsSame(org)) {
                update(item);
            }
        }

        // The remaining items in {@link #itemMap} are those which didn't get placed.
        for (DbEntry item : itemMap) {
            mCarryOver.add(item);
        }

        if (!mCarryOver.isEmpty() && removeWt == 0) {
            // No new items were removed in this step. Try placing all the items on this screen.
            GridOccupancy occupied = new GridOccupancy(mTrgX, mTrgY);
            occupied.markCells(0, 0, mTrgX, startY, true);
            for (DbEntry item : finalItems) {
                occupied.markCells(item, true);
            }

            OptimalPlacementSolution placement = new OptimalPlacementSolution(occupied,
                    deepCopy(mCarryOver), startY, true);
            placement.find();
            if (placement.lowestWeightLoss == 0) {
                // All items got placed

                for (DbEntry item : placement.finalPlacedItems) {
                    item.screenId = screenId;
                    update(item);
                }

                mCarryOver.clear();
            }
        }
    }

    private void updateWidgetSpans(int startY, int screenId, ArrayList<DbEntry> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        int countX = mShouldRemoveX ? mSrcX : mTrgX;
        int countY = mShouldRemoveY ? mSrcY : mTrgY;
        GridOccupancy occupied = new GridOccupancy(countX, countY);
        occupied.markCells(0, 0, countX, startY, true);
        ArrayList<DbEntry> widgets = new ArrayList<>(mSpanUpdatedWidgets.keySet());
        PositionComparator comparator = new PositionComparator();
        Collections.sort(widgets, comparator);
        for (DbEntry item : items) {
            if (!widgets.contains(item)) {
                occupied.markCells(item, true);
            }
        }

        for (DbEntry entry : widgets) {
            Point spans = mSpanUpdatedWidgets.get(entry);
            if (mRepositionEntries.contains(entry)) {
                entry.spanX = spans.x;
                entry.spanY = spans.y;
                continue;
            }

            if (spans.x <= entry.spanX && spans.y <= entry.spanY) {
                entry.spanX = spans.x;
                entry.spanY = spans.y;
                occupied.markCells(entry, true);
                update(entry);
                if (DEBUG) {
                    LogUtils.d(TAG, "The item (" + entry.cellX + ", " + entry.cellY + ") spans become smaller.");
                }
                continue;
            }

            int col = countX - 1;
            int row = countY - 1;
            if (entry.cellX > col || entry.cellY > row
                    || (entry.cellX <= col && (spans.x + entry.cellX) > col)
                    || (entry.cellY <= row && (spans.y + entry.cellY) > row)) {
                entry.spanX = spans.x;
                entry.spanY = spans.y;
                mRepositionEntries.add(entry);
            } else {
                entry.spanX = spans.x;
                entry.spanY = spans.y;
                update(entry);

                ArrayList<DbEntry> intersectItems = findIntersectItems(entry, items);
                for (DbEntry dbEntry : intersectItems) {
                    occupied.markCells(dbEntry, false);
                    mRepositionEntries.add(dbEntry);
                }
                occupied.markCells(entry, true);

                if (DEBUG) {
                    LogUtils.d(TAG, "updateWidgetSpans: intersectItems.size = " + intersectItems.size());
                }
            }
        }

        if (!mRepositionEntries.isEmpty()) {
            IntSparseArrayMap<DbEntry> itemMap = new IntSparseArrayMap<>();
            for (DbEntry e : mRepositionEntries) {
                itemMap.put(e.id, e);
            }

            OptimalPlacementSolution placement = new OptimalPlacementSolution(occupied,
                    deepCopy(mRepositionEntries), startY, true);
            placement.find();
            if (placement.finalPlacedItems.size() > 0) {
                for (DbEntry item : placement.finalPlacedItems) {
                    if (mRepositionEntries.remove(itemMap.get(item.id))) {
                        item.screenId = screenId;
                        update(item);
                    } else {
                        LogUtils.e(TAG, "There is no item: " + item.id
                                + " in the reposition entry list.");
                    }
                }
            } else {
                LogUtils.w(TAG, "No reposition item can be put in this screen: " + screenId
                        + ". So try to put them in the next screen later.");
            }
        }

        for (DbEntry dbEntry : mRepositionEntries) {
            items.remove(dbEntry);
        }
        mSpanUpdatedWidgets.clear();
    }

    private ArrayList<DbEntry> findIntersectItems(DbEntry entry, ArrayList<DbEntry> items) {
        ArrayList<DbEntry> intersectItems = new ArrayList<>();
        if (entry == null || items == null || items.isEmpty()) {
            return intersectItems;
        }

        Rect r0 =new Rect(entry.cellX, entry.cellY, entry.cellX + entry.spanX,
                entry.cellY + entry.spanY);
        Rect r1 = new Rect();
        for (DbEntry item : items) {
            if (item.id == entry.id) continue;
            r1.set(item.cellX, item.cellY, item.cellX + item.spanX, item.cellY + item.spanY);
            if (Rect.intersects(r0, r1)) {
                intersectItems.add(item);
            }
        }
        return intersectItems;
    }

    /**
     * Updates an item in the DB.
     */
    protected void update(DbEntry item) {
        ContentValues values = new ContentValues();
        item.addToContentValues(values);
        mUpdateOperations.put(item.id, values);
    }

    /**
     * Tries the remove the provided row and column.
     *
     * @param items all the items on the screen under operation
     * @param outLoss array of size 2. The first entry is filled with weight loss, and the second
     * with the overall item movement.
     */
    private ArrayList<DbEntry> tryRemove(int col, int row, int startY,
            ArrayList<DbEntry> items, float[] outLoss) {
        GridOccupancy occupied = new GridOccupancy(mTrgX, mTrgY);
        occupied.markCells(0, 0, mTrgX, startY, true);

        col = mShouldRemoveX ? col : Integer.MAX_VALUE;
        row = mShouldRemoveY ? row : Integer.MAX_VALUE;

        ArrayList<DbEntry> finalItems = new ArrayList<>();
        ArrayList<DbEntry> removedItems = new ArrayList<>();

        for (DbEntry item : items) {
            if ((item.cellX <= col && (item.spanX + item.cellX) > col)
                || (item.cellY <= row && (item.spanY + item.cellY) > row)) {
                removedItems.add(item);
                if (item.cellX >= col) item.cellX --;
                if (item.cellY >= row) item.cellY --;
            } else {
                if (item.cellX > col) item.cellX --;
                if (item.cellY > row) item.cellY --;
                finalItems.add(item);
                occupied.markCells(item, true);
            }
        }

        OptimalPlacementSolution placement =
                new OptimalPlacementSolution(occupied, removedItems, startY);
        placement.find();
        finalItems.addAll(placement.finalPlacedItems);
        outLoss[0] = placement.lowestWeightLoss;
        outLoss[1] = placement.lowestMoveCost;
        return finalItems;
    }

    private class OptimalPlacementSolution {
        private final ArrayList<DbEntry> itemsToPlace;
        private final GridOccupancy occupied;

        // If set to true, item movement are not considered in move cost, leading to a more
        // linear placement.
        private final boolean ignoreMove;

        // The first row in the grid from where the placement should start.
        private final int startY;

        float lowestWeightLoss = Float.MAX_VALUE;
        float lowestMoveCost = Float.MAX_VALUE;
        ArrayList<DbEntry> finalPlacedItems;

        public OptimalPlacementSolution(
                GridOccupancy occupied, ArrayList<DbEntry> itemsToPlace, int startY) {
            this(occupied, itemsToPlace, startY, false);
        }

        public OptimalPlacementSolution(GridOccupancy occupied, ArrayList<DbEntry> itemsToPlace,
                int startY, boolean ignoreMove) {
            this.occupied = occupied;
            this.itemsToPlace = itemsToPlace;
            this.ignoreMove = ignoreMove;
            this.startY = startY;

            // Sort the items such that larger widgets appear first followed by 1x1 items
            Collections.sort(this.itemsToPlace);
        }

        public void find() {
            find(0, 0, 0, new ArrayList<DbEntry>());
        }

        /**
         * Recursively finds a placement for the provided items.
         *
         * @param index the position in {@link #itemsToPlace} to start looking at.
         * @param weightLoss total weight loss upto this point
         * @param moveCost total move cost upto this point
         * @param itemsPlaced all the items already placed upto this point
         */
        public void find(int index, float weightLoss, float moveCost,
                ArrayList<DbEntry> itemsPlaced) {
            if ((weightLoss >= lowestWeightLoss) ||
                    ((weightLoss == lowestWeightLoss) && (moveCost >= lowestMoveCost))) {
                // Abort, as we already have a better solution.
                return;

            } else if (index >= itemsToPlace.size()) {
                // End loop.
                lowestWeightLoss = weightLoss;
                lowestMoveCost = moveCost;

                // Keep a deep copy of current configuration as it can change during recursion.
                finalPlacedItems = deepCopy(itemsPlaced);
                return;
            }

            DbEntry me = itemsToPlace.get(index);
            int myX = me.cellX;
            int myY = me.cellY;

            // List of items to pass over if this item was placed.
            ArrayList<DbEntry> itemsIncludingMe = new ArrayList<>(itemsPlaced.size() + 1);
            itemsIncludingMe.addAll(itemsPlaced);
            itemsIncludingMe.add(me);

            if (me.spanX > 1 || me.spanY > 1) {
                // If the current item is a widget (and it greater than 1x1), try to place it at
                // all possible positions. This is because a widget placed at one position can
                // affect the placement of a different widget.
                int myW = me.spanX;
                int myH = me.spanY;

                for (int y = startY; y < mTrgY; y++) {
                    for (int x = 0; x < mTrgX; x++) {
                        float newMoveCost = moveCost;
                        if (x != myX) {
                            me.cellX = x;
                            newMoveCost ++;
                        }
                        if (y != myY) {
                            me.cellY = y;
                            newMoveCost ++;
                        }
                        if (ignoreMove) {
                            newMoveCost = moveCost;
                        }

                        if (occupied.isRegionVacant(x, y, myW, myH)) {
                            // place at this position and continue search.
                            occupied.markCells(me, true);
                            find(index + 1, weightLoss, newMoveCost, itemsIncludingMe);
                            occupied.markCells(me, false);
                        }

                        // Try resizing horizontally
                        if (myW > me.minSpanX && occupied.isRegionVacant(x, y, myW - 1, myH)) {
                            me.spanX --;
                            occupied.markCells(me, true);
                            // 1 extra move cost
                            find(index + 1, weightLoss, newMoveCost + 1, itemsIncludingMe);
                            occupied.markCells(me, false);
                            me.spanX ++;
                        }

                        // Try resizing vertically
                        if (myH > me.minSpanY && occupied.isRegionVacant(x, y, myW, myH - 1)) {
                            me.spanY --;
                            occupied.markCells(me, true);
                            // 1 extra move cost
                            find(index + 1, weightLoss, newMoveCost + 1, itemsIncludingMe);
                            occupied.markCells(me, false);
                            me.spanY ++;
                        }

                        // Try resizing horizontally & vertically
                        if (myH > me.minSpanY && myW > me.minSpanX &&
                                occupied.isRegionVacant(x, y, myW - 1, myH - 1)) {
                            me.spanX --;
                            me.spanY --;
                            occupied.markCells(me, true);
                            // 2 extra move cost
                            find(index + 1, weightLoss, newMoveCost + 2, itemsIncludingMe);
                            occupied.markCells(me, false);
                            me.spanX ++;
                            me.spanY ++;
                        }
                        me.cellX = myX;
                        me.cellY = myY;
                    }
                }

                // Finally also try a solution when this item is not included. Trying it in the end
                // causes it to get skipped in most cases due to higher weight loss, and prevents
                // unnecessary deep copies of various configurations.
                find(index + 1, weightLoss + me.weight, moveCost, itemsPlaced);
            } else {
                // Since this is a 1x1 item and all the following items are also 1x1, just place
                // it at 'the most appropriate position' and hope for the best.
                // The most appropriate position: one with lease straight line distance
                int newDistance = Integer.MAX_VALUE;
                int newX = Integer.MAX_VALUE, newY = Integer.MAX_VALUE;

                for (int y = startY; y < mTrgY; y++) {
                    for (int x = 0; x < mTrgX; x++) {
                        if (!occupied.cells[x][y]) {
                            int dist = ignoreMove ? 0 :
                                    ((me.cellX - x) * (me.cellX - x) + (me.cellY - y) * (me.cellY
                                            - y));
                            if (dist < newDistance) {
                                newX = x;
                                newY = y;
                                newDistance = dist;
                            }
                        }
                    }
                }

                if (newX < mTrgX && newY < mTrgY) {
                    float newMoveCost = moveCost;
                    if (newX != myX) {
                        me.cellX = newX;
                        newMoveCost ++;
                    }
                    if (newY != myY) {
                        me.cellY = newY;
                        newMoveCost ++;
                    }
                    if (ignoreMove) {
                        newMoveCost = moveCost;
                    }
                    occupied.markCells(me, true);
                    find(index + 1, weightLoss, newMoveCost, itemsIncludingMe);
                    occupied.markCells(me, false);
                    me.cellX = myX;
                    me.cellY = myY;

                    // Try to find a solution without this item, only if
                    //  1) there was at least one space, i.e., we were able to place this item
                    //  2) if the next item has the same weight (all items are already sorted), as
                    //     if it has lower weight, that solution will automatically get discarded.
                    //  3) ignoreMove false otherwise, move cost is ignored and the weight will
                    //      anyway be same.
                    if (index + 1 < itemsToPlace.size()
                            && itemsToPlace.get(index + 1).weight >= me.weight && !ignoreMove) {
                        find(index + 1, weightLoss + me.weight, moveCost, itemsPlaced);
                    }
                } else {
                    // No more space. Jump to the end.
                    for (int i = index + 1; i < itemsToPlace.size(); i++) {
                        weightLoss += itemsToPlace.get(i).weight;
                    }
                    find(itemsToPlace.size(), weightLoss + me.weight, moveCost, itemsPlaced);
                }
            }
        }
    }

    /**
     * Loads all widgets to check if need update.
     */
    protected boolean hasWidgetSpanUpdatedEntry() {
        // Go version disable widgets
        if (FeatureFlags.GO_DISABLE_WIDGETS) {
            return false;
        }
        boolean isNeedUpdate = false;
        Cursor c = queryWorkspace(
                new String[]{
                        Favorites._ID,                  // 0
                        Favorites.SPANX,                // 1
                        Favorites.SPANY,                // 2
                        Favorites.APPWIDGET_ID},        // 4
                Favorites.ITEM_TYPE + " = " + Favorites.ITEM_TYPE_APPWIDGET, null);

        final int indexId = c.getColumnIndexOrThrow(Favorites._ID);
        final int indexSpanX = c.getColumnIndexOrThrow(Favorites.SPANX);
        final int indexSpanY = c.getColumnIndexOrThrow(Favorites.SPANY);
        final int indexAppWidgetId = c.getColumnIndexOrThrow(Favorites.APPWIDGET_ID);

        while (c.moveToNext()) {
            DbEntry entry = new DbEntry();
            entry.id = c.getInt(indexId);
            entry.spanX = c.getInt(indexSpanX);
            entry.spanY = c.getInt(indexSpanY);

            try {
                int widgetId = c.getInt(indexAppWidgetId);
                LauncherAppWidgetProviderInfo pInfo = AppWidgetManagerCompat.getInstance(
                        mContext).getLauncherAppWidgetInfo(widgetId);
                if (pInfo != null) {
                    Point pMinSpans = pInfo.getMinSpans();
                    if (pMinSpans.y != -1 && pMinSpans.x != -1) {
                        isNeedUpdate = entry.spanX < pMinSpans.x || entry.spanY < pMinSpans.y;
                    } else {
                        isNeedUpdate = entry.spanX != pInfo.spanX || entry.spanY != pInfo.spanY;
                    }
                    if (isNeedUpdate) {
                        break;
                    }
                }
            } catch (Exception e) {
                LogUtils.e(TAG, "Can't load widgets from Db.", e);
            }
        }
        UtilitiesExt.closeCursorSilently(c);

        if (LogUtils.DEBUG_WIDGET) {
            LogUtils.d(TAG, "Widget span need update:" + isNeedUpdate);
        }
        return isNeedUpdate;
    }

    private ArrayList<DbEntry> loadHotseatEntries() {
        Cursor c =  queryWorkspace(
                new String[]{
                        Favorites._ID,                  // 0
                        Favorites.ITEM_TYPE,            // 1
                        Favorites.INTENT,               // 2
                        Favorites.SCREEN},              // 3
                Favorites.CONTAINER + " = " + Favorites.CONTAINER_HOTSEAT, Favorites.SCREEN);

        final int indexId = c.getColumnIndexOrThrow(Favorites._ID);
        final int indexItemType = c.getColumnIndexOrThrow(Favorites.ITEM_TYPE);
        final int indexIntent = c.getColumnIndexOrThrow(Favorites.INTENT);
        final int indexScreen = c.getColumnIndexOrThrow(Favorites.SCREEN);

        ArrayList<DbEntry> entries = new ArrayList<>();
        while (c.moveToNext()) {
            DbEntry entry = new DbEntry();
            entry.id = c.getInt(indexId);
            entry.itemType = c.getInt(indexItemType);
            entry.screenId = c.getInt(indexScreen);

            if (entry.screenId >= mSrcHotseatSize) {
                mEntryToRemove.add(entry.id);
                continue;
            }

            try {
                // calculate weight
                switch (entry.itemType) {
                    case Favorites.ITEM_TYPE_SHORTCUT:
                    case Favorites.ITEM_TYPE_DEEP_SHORTCUT:
                    case Favorites.ITEM_TYPE_APPLICATION: {
                        verifyIntent(c.getString(indexIntent));
                        entry.weight = entry.itemType == Favorites.ITEM_TYPE_APPLICATION ?
                                WT_APPLICATION : WT_SHORTCUT;
                        break;
                    }
                    case Favorites.ITEM_TYPE_FOLDER: {
                        int total = getFolderItemsCount(entry.id);
                        if (total == 0) {
                            throw new Exception("Folder is empty");
                        }
                        entry.weight = WT_FOLDER_FACTOR * total;
                        break;
                    }
                    default:
                        throw new Exception("Invalid item type");
                }
            } catch (Exception e) {
                if (DEBUG) {
                    Log.d(TAG, "Removing item " + entry.id, e);
                }
                mEntryToRemove.add(entry.id);
                continue;
            }
            entries.add(entry);
        }
        c.close();
        return entries;
    }


    /**
     * Loads entries for a particular screen id.
     */
    protected ArrayList<DbEntry> loadWorkspaceEntries(int screen) {
        Cursor c = queryWorkspace(
                new String[]{
                        Favorites._ID,                  // 0
                        Favorites.ITEM_TYPE,            // 1
                        Favorites.CELLX,                // 2
                        Favorites.CELLY,                // 3
                        Favorites.SPANX,                // 4
                        Favorites.SPANY,                // 5
                        Favorites.INTENT,               // 6
                        Favorites.APPWIDGET_PROVIDER,   // 7
                        Favorites.APPWIDGET_ID},        // 8
                Favorites.CONTAINER + " = " + Favorites.CONTAINER_DESKTOP
                        + " AND " + Favorites.SCREEN + " = " + screen, null);

        final int indexId = c.getColumnIndexOrThrow(Favorites._ID);
        final int indexItemType = c.getColumnIndexOrThrow(Favorites.ITEM_TYPE);
        final int indexCellX = c.getColumnIndexOrThrow(Favorites.CELLX);
        final int indexCellY = c.getColumnIndexOrThrow(Favorites.CELLY);
        final int indexSpanX = c.getColumnIndexOrThrow(Favorites.SPANX);
        final int indexSpanY = c.getColumnIndexOrThrow(Favorites.SPANY);
        final int indexIntent = c.getColumnIndexOrThrow(Favorites.INTENT);
        final int indexAppWidgetProvider = c.getColumnIndexOrThrow(Favorites.APPWIDGET_PROVIDER);
        final int indexAppWidgetId = c.getColumnIndexOrThrow(Favorites.APPWIDGET_ID);

        ArrayList<DbEntry> entries = new ArrayList<>();
        while (c.moveToNext()) {
            DbEntry entry = new DbEntry();
            entry.id = c.getInt(indexId);
            entry.itemType = c.getInt(indexItemType);
            entry.cellX = c.getInt(indexCellX);
            entry.cellY = c.getInt(indexCellY);
            entry.spanX = c.getInt(indexSpanX);
            entry.spanY = c.getInt(indexSpanY);
            entry.screenId = screen;

            try {
                // calculate weight
                switch (entry.itemType) {
                    case Favorites.ITEM_TYPE_SHORTCUT:
                    case Favorites.ITEM_TYPE_DEEP_SHORTCUT:
                    case Favorites.ITEM_TYPE_APPLICATION: {
                        verifyIntent(c.getString(indexIntent));
                        entry.weight = entry.itemType == Favorites.ITEM_TYPE_APPLICATION ?
                                WT_APPLICATION : WT_SHORTCUT;
                        break;
                    }
                    case Favorites.ITEM_TYPE_APPWIDGET: {
                        String provider = c.getString(indexAppWidgetProvider);
                        ComponentName cn = ComponentName.unflattenFromString(provider);
                        verifyPackage(cn.getPackageName());
                        entry.weight = Math.max(WT_WIDGET_MIN, WT_WIDGET_FACTOR
                                * entry.spanX * entry.spanY);

                        int widgetId = c.getInt(indexAppWidgetId);
                        LauncherAppWidgetProviderInfo pInfo = AppWidgetManagerCompat.getInstance(
                                mContext).getLauncherAppWidgetInfo(widgetId);
                        Point spans = null;
                        Point targetSpan = getWidgetTargetSpan(entry, pInfo);
                        if (pInfo != null) {
                            spans = pInfo.getMinSpans();
                        }
                        if (spans != null) {
                            entry.minSpanX = spans.x > 0 ? spans.x : targetSpan.x;
                            entry.minSpanY = spans.y > 0 ? spans.y : targetSpan.y;
                        } else {
                            // Assume that the widget be resized down to 2x2
                            entry.minSpanX = entry.minSpanY = 2;
                        }

                        if (entry.minSpanX > mTrgX || entry.minSpanY > mTrgY) {
                            throw new Exception("Widget can't be resized down to fit the grid");
                        }
                        // Make the spanX and spanY of DbEntry to be equal to or larger than
                        // the minSpanX and minSpanY of its {@link LauncherAppWidgetProviderInfo}
                        if (!targetSpan.equals(new Point(entry.spanX, entry.spanY))) {
                            mSpanUpdatedWidgets.put(entry, targetSpan);
                        }
                        break;
                    }
                    case Favorites.ITEM_TYPE_FOLDER: {
                        int total = getFolderItemsCount(entry.id);
                        if (total == 0) {
                            throw new Exception("Folder is empty");
                        }
                        entry.weight = WT_FOLDER_FACTOR * total;
                        break;
                    }
                    default:
                        throw new Exception("Invalid item type");
                }
            } catch (Exception e) {
                if (DEBUG) {
                    Log.d(TAG, "Removing item " + entry.id, e);
                }
                mEntryToRemove.add(entry.id);
                continue;
            }
            entries.add(entry);
        }
        c.close();
        return entries;
    }

    private Point getWidgetTargetSpan(DbEntry entry, LauncherAppWidgetProviderInfo info) {
        Point targetSpans = new Point(entry.spanX, entry.spanY);
        if (info == null) {
            return targetSpans;
        }
        if (info.supportResize()) {
            Point minSpans = info.getMinSpans();
            int minSpanX = minSpans != null && minSpans.x > 0 ? minSpans.x : info.spanX;
            int minSpanY = minSpans != null && minSpans.y > 0 ? minSpans.y : info.spanY;
            targetSpans.x = targetSpans.x < minSpanX ? minSpanX : targetSpans.x;
            targetSpans.y = targetSpans.y < minSpanY ? minSpanY : targetSpans.y;
        } else {
            targetSpans.x = info.spanX;
            targetSpans.y = info.spanY;
        }
        return targetSpans;
    }

    /**
     * @return the number of valid items in the folder.
     */
    private int getFolderItemsCount(int folderId) {
        Cursor c = queryWorkspace(
                new String[]{Favorites._ID, Favorites.INTENT},
                Favorites.CONTAINER + " = " + folderId, null);

        int total = 0;
        while (c.moveToNext()) {
            try {
                verifyIntent(c.getString(1));
                total++;
            } catch (Exception e) {
                mEntryToRemove.add(c.getInt(0));
            }
        }
        c.close();
        return total;
    }

    protected Cursor queryWorkspace(String[] columns, String where, String orderBy) {
        return mDb.query(Favorites.TABLE_NAME, columns, where, null, null, null, orderBy);
    }

    /**
     * Verifies if the intent should be restored.
     */
    private void verifyIntent(String intentStr) throws Exception {
        Intent intent = Intent.parseUri(intentStr, 0);
        if (intent.getComponent() != null) {
            verifyPackage(intent.getComponent().getPackageName());
        } else if (intent.getPackage() != null) {
            // Only verify package if the component was null.
            verifyPackage(intent.getPackage());
        }
    }

    /**
     * Verifies if the package should be restored
     */
    private void verifyPackage(String packageName) throws Exception {
        if (!mValidPackages.contains(packageName)) {
            throw new Exception("Package not available");
        }
    }

    protected static class DbEntry extends ItemInfo implements Comparable<DbEntry> {

        public float weight;

        private boolean mIsContainerChanged;

        public DbEntry() {
        }

        public DbEntry copy() {
            DbEntry entry = new DbEntry();
            entry.copyFrom(this);
            entry.weight = weight;
            entry.minSpanX = minSpanX;
            entry.minSpanY = minSpanY;
            return entry;
        }

        /**
         * Comparator such that larger widgets come first,  followed by all 1x1 items
         * based on their weights.
         */
        @Override
        public int compareTo(DbEntry another) {
            if (itemType == Favorites.ITEM_TYPE_APPWIDGET) {
                if (another.itemType == Favorites.ITEM_TYPE_APPWIDGET) {
                    return another.spanY * another.spanX - spanX * spanY;
                } else {
                    return -1;
                }
            } else if (another.itemType == Favorites.ITEM_TYPE_APPWIDGET) {
                return 1;
            } else {
                // Place higher weight before lower weight.
                return Float.compare(another.weight, weight);
            }
        }

        public boolean columnsSame(DbEntry org) {
            return org.cellX == cellX && org.cellY == cellY && org.spanX == spanX &&
                    org.spanY == spanY && org.screenId == screenId;
        }

        public void addToContentValues(ContentValues values) {
            if (mIsContainerChanged) {
                values.put(Favorites.CONTAINER, container);
            }
            values.put(Favorites.SCREEN, screenId);
            values.put(Favorites.CELLX, cellX);
            values.put(Favorites.CELLY, cellY);
            values.put(Favorites.SPANX, spanX);
            values.put(Favorites.SPANY, spanY);
        }

        public void updateContainerAndScreenId(int container, int screenId) {
            this.container = container;
            this.screenId = screenId;
            mIsContainerChanged = true;
        }
    }

    private static ArrayList<DbEntry> deepCopy(ArrayList<DbEntry> src) {
        ArrayList<DbEntry> dup = new ArrayList<>(src.size());
        for (DbEntry e : src) {
            dup.add(e.copy());
        }
        return dup;
    }

    public static void markForMigration(
            Context context, int gridX, int gridY, int hotseatSize) {
        String workspaceGridKey =
                MultiModeController.getKeyByMode(context, KEY_MIGRATION_SRC_WORKSPACE_SIZE);
        String hotseatGridKey =
                MultiModeController.getKeyByMode(context, KEY_MIGRATION_SRC_HOTSEAT_COUNT);
        String densityKey =
                MultiModeController.getKeyByMode(context, KEY_MIGRATION_SRC_DENSITY_DPI);

        Utilities.getPrefs(context).edit()
                .putString(workspaceGridKey, getPointString(gridX, gridY))
                .putInt(hotseatGridKey, hotseatSize)
                .putInt(densityKey, context.getResources().getConfiguration().densityDpi)
                .apply();
    }

    @SuppressLint("ApplySharedPref")
    private static void markForMigrationOnDbUpdate(
            Context context, int oldVersion, boolean isSingleLayerMode) {
        String oldworkspaceGridKey = MultiModeController.getKeyByMode(
                context, KEY_MIGRATION_SRC_WORKSPACE_SIZE, oldVersion, isSingleLayerMode);
        String oldHotseatGridKey = MultiModeController.getKeyByMode(
                context, KEY_MIGRATION_SRC_HOTSEAT_COUNT, oldVersion, isSingleLayerMode);

        String workspaceGridKey = MultiModeController.getKeyByMode(
                context, KEY_MIGRATION_SRC_WORKSPACE_SIZE, SCHEMA_VERSION, isSingleLayerMode);
        String hotseatGridKey = MultiModeController.getKeyByMode(
                context, KEY_MIGRATION_SRC_HOTSEAT_COUNT, SCHEMA_VERSION, isSingleLayerMode);
        String densityKey = MultiModeController.getKeyByMode(
                context, KEY_MIGRATION_SRC_DENSITY_DPI, SCHEMA_VERSION, isSingleLayerMode);

        InvariantDeviceProfile idp = LauncherAppState.getIDP(context);
        String gridSizeString = getPointString(idp.numColumns, idp.numRows);

        SharedPreferences prefs = Utilities.getPrefs(context);
        prefs.edit().putString(workspaceGridKey, prefs.getString(oldworkspaceGridKey, gridSizeString))
                .putInt(hotseatGridKey, prefs.getInt(oldHotseatGridKey, idp.numHotseatIcons))
                .putInt(densityKey, context.getResources().getConfiguration().densityDpi)
                .commit();
    }

    public static void onDbUpgrade(Context context, int oldVersion, int newVersion) {
        switch (oldVersion) {
            case 27:
                boolean isSingleLayerMode = MultiModeController.isSingleLayerMode();
                markForMigrationOnDbUpdate(context, oldVersion, isSingleLayerMode);
                if (MultiModeController.isSupportDynamicChange()) {
                    markForMigrationOnDbUpdate(context, oldVersion, !isSingleLayerMode);
                }
                break;
            default:
                // do nothing
                break;
        }
    }

    /**
     * Migrates the workspace and hotseat in case their sizes changed.
     *
     * @return false if the migration failed.
     */
    public static boolean migrateGridIfNeeded(Context context) {
        SharedPreferences prefs = Utilities.getPrefs(context);
        InvariantDeviceProfile idp = LauncherAppState.getIDP(context);


        int curDensity = context.getResources().getConfiguration().densityDpi;
        String gridSizeString = getPointString(idp.numColumns, idp.numRows);
        String workspaceSizeKey =
                MultiModeController.getKeyByMode(context, KEY_MIGRATION_SRC_WORKSPACE_SIZE);
        String hotseatSizeKey =
                MultiModeController.getKeyByMode(context, KEY_MIGRATION_SRC_HOTSEAT_COUNT);
        String densityKey =
                MultiModeController.getKeyByMode(context, KEY_MIGRATION_SRC_DENSITY_DPI);

        boolean shouldCheckWidget = !FeatureFlags.GO_DISABLE_WIDGETS &&
                curDensity != prefs.getInt(densityKey, curDensity);

        if (gridSizeString.equals(prefs.getString(workspaceSizeKey, "")) &&
                idp.numHotseatIcons == prefs.getInt(hotseatSizeKey, idp.numHotseatIcons)
                && !shouldCheckWidget) {
            // Skip if workspace sizes, hotseat sizes, density have not changed.
            return true;
        }

        long migrationStartTime = System.currentTimeMillis();
        try (SQLiteTransaction transaction = (SQLiteTransaction) Settings.call(
                context.getContentResolver(), Settings.METHOD_NEW_TRANSACTION)
                .getBinder(Settings.EXTRA_VALUE)) {

            int srcHotseatCount = prefs.getInt(hotseatSizeKey, idp.numHotseatIcons);
            Point sourceSize = parsePoint(prefs.getString(workspaceSizeKey, gridSizeString));

            boolean dbChanged = false;

            GridBackupTable backupTable = new GridBackupTable(context, transaction.getDb(),
                    srcHotseatCount, sourceSize.x, sourceSize.y);
            if (backupTable.backupOrRestoreAsNeeded()) {
                dbChanged = true;
                srcHotseatCount = backupTable.getRestoreHotseatAndGridSize(sourceSize);
            }

            HashSet<String> validPackages = getValidPackages(context);
            // Hotseat
            GridSizeMigrationTask hotseatMigrationTask = null;
            if (srcHotseatCount != idp.numHotseatIcons) {
                // Migrate hotseat.
                hotseatMigrationTask = new GridSizeMigrationTask(context, transaction.getDb(),
                        validPackages, srcHotseatCount, idp.numHotseatIcons);
                dbChanged = hotseatMigrationTask.migrateHotseat();
            }

            // Grid size
            Point targetSize = new Point(idp.numColumns, idp.numRows);
            if (new MultiStepMigrationTask(validPackages, context, transaction.getDb())
                    .migrate(sourceSize, targetSize)) {
                dbChanged = true;
            }

            // Density size, If Grid size changed, no need to redo migrate
            if (sourceSize.equals(targetSize) && shouldCheckWidget) {
                if (new WidgetMigrationTask(context, transaction.getDb(),
                        validPackages).migrateIfNeeded(targetSize)) {
                    dbChanged = true;
                }
            }

            // Migrate the dropped items from hotseat to workspace.
            if (FeatureOption.SPRD_DESKTOP_GRID_SUPPORT.get() && null != hotseatMigrationTask) {
                hotseatMigrationTask.setTargetSize(targetSize);
                dbChanged |= hotseatMigrationTask.migrateItemsFormHotseatToWorkspace();
            }

            if (dbChanged) {
                // Make sure we haven't removed everything.
                final Cursor c = context.getContentResolver().query(
                        Favorites.CONTENT_URI, null, null, null, null);
                boolean hasData = c.moveToNext();
                c.close();
                if (!hasData) {
                    throw new Exception("Removed every thing during grid resize");
                }
            }

            transaction.commit();
            Settings.call(context.getContentResolver(), Settings.METHOD_REFRESH_BACKUP_TABLE);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error during grid migration", e);

            return false;
        } finally {
            Log.v(TAG, "Workspace migration completed in "
                    + (System.currentTimeMillis() - migrationStartTime));

            // Save current configuration, so that the migration does not run again.
            prefs.edit()
                    .putString(workspaceSizeKey, gridSizeString)
                    .putInt(hotseatSizeKey, idp.numHotseatIcons)
                    .putInt(densityKey, curDensity)
                    .apply();
        }
    }

    protected static HashSet<String> getValidPackages(Context context) {
        // Initialize list of valid packages. This contain all the packages which are already on
        // the device and packages which are being installed. Any item which doesn't belong to
        // this set is removed.
        // Since the loader removes such items anyway, removing these items here doesn't cause
        // any extra data loss and gives us more free space on the grid for better migration.
        HashSet<String> validPackages = new HashSet<>();
        for (PackageInfo info : context.getPackageManager()
                .getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES)) {
            validPackages.add(info.packageName);
        }
        validPackages.addAll(PackageInstallerCompat.getInstance(context)
                .updateAndGetActiveSessionCache().keySet());
        return validPackages;
    }

    /**
     * Removes any broken item from the hotseat.
     *
     * @return a map with occupied hotseat position set to non-null value.
     */
    public static IntSparseArrayMap<Object> removeBrokenHotseatItems(Context context)
            throws Exception {
        try (SQLiteTransaction transaction = (SQLiteTransaction) Settings.call(
                context.getContentResolver(), Settings.METHOD_NEW_TRANSACTION)
                .getBinder(Settings.EXTRA_VALUE)) {
            GridSizeMigrationTask task = new GridSizeMigrationTask(
                    context, transaction.getDb(), getValidPackages(context),
                    Integer.MAX_VALUE, Integer.MAX_VALUE);

            // Load all the valid entries
            ArrayList<DbEntry> items = task.loadHotseatEntries();
            // Delete any entry marked for deletion by above load.
            task.applyOperations();
            IntSparseArrayMap<Object> positions = new IntSparseArrayMap<>();
            for (DbEntry item : items) {
                positions.put(item.screenId, item);
            }
            transaction.commit();
            return positions;
        }
    }

    /**
     * Task to run grid migration in multiple steps when the size difference is more than 1.
     */
    protected static class MultiStepMigrationTask {
        private final HashSet<String> mValidPackages;
        private final Context mContext;
        private final SQLiteDatabase mDb;

        public MultiStepMigrationTask(HashSet<String> validPackages, Context context,
                SQLiteDatabase db) {
            mValidPackages = validPackages;
            mContext = context;
            mDb = db;
        }

        public boolean migrate(Point sourceSize, Point targetSize) throws Exception {
            boolean dbChanged = false;
            if (!targetSize.equals(sourceSize)) {
//                if (sourceSize.x < targetSize.x) {
//                    // Source is smaller that target, just expand the grid without actual migration.
//                    sourceSize.x = targetSize.x;
//                }
//                if (sourceSize.y < targetSize.y) {
//                    // Source is smaller that target, just expand the grid without actual migration.
//                    sourceSize.y = targetSize.y;
//                }

                // Migrate the workspace grid, such that the points differ by max 1 in x and y
                // each on every step.
                while (!targetSize.equals(sourceSize)) {
                    // Get the next size, such that the points differ by max 1 in x and y each
                    Point nextSize = new Point(sourceSize);
                    if (targetSize.x < nextSize.x) {
                        nextSize.x--;
                    } else if (targetSize.x > nextSize.x) {
                        nextSize.x = targetSize.x;
                    }
                    if (targetSize.y < nextSize.y) {
                        nextSize.y--;
                    } else if (targetSize.y > nextSize.y) {
                        nextSize.y = targetSize.y;
                    }
                    if (runStepTask(sourceSize, nextSize)) {
                        dbChanged = true;
                    }
                    sourceSize.set(nextSize.x, nextSize.y);
                }
            }
            return dbChanged;
        }

        protected boolean runStepTask(Point sourceSize, Point nextSize) throws Exception {
            return new GridSizeMigrationTask(mContext, mDb,
                    mValidPackages, sourceSize, nextSize).migrateWorkspace();
        }
    }

    /**
     * Task to run widget migration in multiple steps when the span size is difference.
     */
    protected static class WidgetMigrationTask {
        private final HashSet<String> mValidPackages;
        private final Context mContext;
        private final SQLiteDatabase mDb;

        public WidgetMigrationTask(Context context,
                                   SQLiteDatabase db, HashSet<String> validPackages) {
            mValidPackages = validPackages;
            mContext = context;
            mDb = db;
        }

        public boolean migrateIfNeeded(Point gridSize) throws Exception {
            boolean dbChanged = false;
            GridSizeMigrationTask migrationTask = new GridSizeMigrationTask(mContext, mDb,
                    mValidPackages, gridSize, gridSize);
            if (migrationTask.hasWidgetSpanUpdatedEntry()) {
                if (migrationTask.migrateWorkspace()) {
                    dbChanged = true;
                }
            }
            return dbChanged;
        }
    }
}
