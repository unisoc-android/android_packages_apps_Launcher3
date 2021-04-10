package com.sprd.ext.gestures;

import android.graphics.PointF;
import android.view.MotionEvent;

import com.android.launcher3.Launcher;

import java.util.ArrayList;

public class LauncherRootViewGestures {

    private final Launcher mLauncher;
    private static final float MIN_DISTANCE = 200f;
    private enum FingerMode {NONE, ONE_FINGER_MODE, MULTI_FINGER_MODE}
    private FingerMode mFingerMode = FingerMode.NONE;
    // You can add different gestures here
    public enum Gesture {NONE,
        ONE_FINGER_SLIDE_UP, ONE_FINGER_SLIDE_DOWN, ONE_FINGER_SLIDE_LEFT, ONE_FINGER_SLIDE_RIGHT}

    private PointF startPoint = new PointF();
    private PointF curPoint = new PointF();
    private ArrayList<OnGestureListener> mOnGestureListeners = new ArrayList<>();

    public interface OnGestureListener{
        boolean onGesture(Launcher launcher, Gesture gesture);
    }

    public LauncherRootViewGestures(Launcher launcher) {
        mLauncher = launcher;
    }

    public boolean onTouchEvent(MotionEvent event) {
        // Do nothing
        if(!mLauncher.isEnableGestures() || mOnGestureListeners.isEmpty()){
            mFingerMode = FingerMode.NONE;
            return false;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startPoint.set(event.getX(), event.getY());
                mFingerMode = FingerMode.ONE_FINGER_MODE;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                mFingerMode = FingerMode.MULTI_FINGER_MODE;
                break;
            case MotionEvent.ACTION_UP:
                boolean ret = notifyListeners(eventAnalysis(event));
                mFingerMode = FingerMode.NONE;
                return ret;
        }
        return false;
    }

    private Gesture eventAnalysis(MotionEvent event){
        Gesture gesture = Gesture.NONE;
        switch (mFingerMode) {
            case ONE_FINGER_MODE:
                curPoint.set(event.getX(), event.getY());
                float fingerDist = distance(startPoint, curPoint);
                if (fingerDist > MIN_DISTANCE) {
                    if (Math.abs(startPoint.x - curPoint.x) > Math.abs(startPoint.y - curPoint.y)) {
                        gesture = startPoint.x > curPoint.x ?
                                Gesture.ONE_FINGER_SLIDE_LEFT : Gesture.ONE_FINGER_SLIDE_RIGHT;
                    } else {
                        gesture = startPoint.y > curPoint.y ?
                                Gesture.ONE_FINGER_SLIDE_UP : Gesture.ONE_FINGER_SLIDE_DOWN;
                    }
                }
                break;
            default:
                break;
        }
        return gesture;
    }

    private boolean notifyListeners(Gesture gesture) {
        boolean result = false;
        for (OnGestureListener listener : mOnGestureListeners) {
            if (listener != null) {
                result |= listener.onGesture(mLauncher, gesture);
            }
        }
        return result;
    }

    void registerOnGestureListener(OnGestureListener listener) {
        if (mOnGestureListeners.contains(listener)) {
            return;
        }
        mOnGestureListeners.add(listener);
    }

    void unregisterOnGestureListener(OnGestureListener listener) {
        if (!mOnGestureListeners.isEmpty()) {
            mOnGestureListeners.remove(listener);
        }
    }

    private float distance(PointF point1, PointF point2) {
        float x = point1.x - point2.x;
        float y = point1.y - point2.y;
        return (float) Math.hypot(x, y);
    }
}
