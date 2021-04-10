package com.android.quickstep.views;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.BaseDraggingActivity;
import com.android.launcher3.R;
import com.android.launcher3.anim.AnimationSuccessListener;
import com.android.launcher3.views.BaseDragLayer;
import com.android.quickstep.TaskActionController;
import com.android.quickstep.TaskHolder;
import com.android.quickstep.TaskSystemShortcut;
import com.android.systemui.shared.recents.model.Task;

import java.util.List;

public class TaskMenuView extends AbstractFloatingView {

    private static final int REVEAL_OPEN_DURATION = 150;
    private static final int REVEAL_CLOSE_DURATION = 100;
    private BaseDraggingActivity mActivity;
    private TextView mTaskName;
    private ImageView mTaskIcon;
    private ObjectAnimator mOpenCloseAnimator;
    private LinearLayout mOptionLayout;

    public TaskMenuView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TaskMenuView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mActivity = BaseDraggingActivity.fromContext(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mTaskName = findViewById(R.id.task_name);
        mTaskIcon = findViewById(R.id.task_icon);
        mOptionLayout = findViewById(R.id.menu_option_layout);
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            BaseDragLayer dl = mActivity.getDragLayer();
            if (!dl.isEventOverView(this, ev)) {
                close(true);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void handleClose(boolean animate) {
        if (animate) {
            animateClose();
        } else {
            closeComplete();
        }
    }

    @Override
    public void logActionCommand(int command) {
        // TODO
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    protected boolean isOfType(int type) {
        return (type & TYPE_TASK_MENU) != 0;
    }

    public static TaskMenuView showForTask(TaskHolder taskHolder, TaskActionController controller) {
        if (!taskHolder.getTask().isPresent()) {
            return null;
        }
        Task task = taskHolder.getTask().get();
        BaseDraggingActivity activity = BaseDraggingActivity.fromContext(
                taskHolder.getTaskItemView().getContext());
        final TaskMenuView taskMenuView = (TaskMenuView) activity.getLayoutInflater().inflate(
                R.layout.task_item_menu, activity.getDragLayer(), false);
        return taskMenuView.populateAndShowForTask(controller, task) ? taskMenuView : null;
    }

    private boolean populateAndShowForTask(TaskActionController controller, Task task) {
        if (isAttachedToWindow()) {
            return false;
        }
        final List<TaskSystemShortcut> shortcuts =
                TaskSystemShortcut.getEnabledShortcuts(getContext(), controller, task);
        if (shortcuts.isEmpty()) {
            return false;
        }
        mActivity.getDragLayer().addView(this);
        addMenuOptions(controller, shortcuts, task);
        post(this::animateOpen);
        return true;
    }

    private void addMenuOptions(TaskActionController controller,
                                List<TaskSystemShortcut> shortcuts, Task task) {
        Drawable icon = null;
        if (task.icon != null) {
            Drawable.ConstantState constantState = task.icon.getConstantState();
            if (constantState != null) {
                icon = constantState.newDrawable();
            }
        }
        if (icon == null) {
            icon = Resources.getSystem()
                    .getDrawable(android.R.drawable.sym_def_app_icon, null);
        }
        mTaskIcon.setImageDrawable(icon);
        mTaskIcon.setOnClickListener(v -> close(true));
        mTaskName.setText(task.titleDescription);
        mTaskName.setOnClickListener(v -> close(true));
        final int count = shortcuts.size();
        for (int i = 0; i < count; ++i) {
            final TaskSystemShortcut menuOption = shortcuts.get(i);
            addMenuOption(menuOption, menuOption.getOnClickListener(mActivity, controller, task));
        }
    }

    private void addMenuOption(TaskSystemShortcut menuOption, OnClickListener onClickListener) {
        ViewGroup menuOptionView = (ViewGroup) mActivity.getLayoutInflater().inflate(
                R.layout.task_view_menu_option, this, false);
        menuOption.setIconAndLabelFor(
                menuOptionView.findViewById(R.id.icon), menuOptionView.findViewById(R.id.text));
        menuOptionView.setOnClickListener(onClickListener);
        mOptionLayout.addView(menuOptionView);
    }

    private void animateOpen() {
        animateOpenOrClosed(false);
        mIsOpen = true;
    }

    private void animateClose() {
        animateOpenOrClosed(true);
    }

    private void animateOpenOrClosed(boolean closing) {
        if (mOpenCloseAnimator != null && mOpenCloseAnimator.isRunning()) {
            mOpenCloseAnimator.end();
        }
        mOpenCloseAnimator = ObjectAnimator.ofFloat(this, ALPHA, closing ? 0f : 1f);
        mOpenCloseAnimator.addListener(new AnimationSuccessListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                setVisibility(VISIBLE);
            }

            @Override
            public void onAnimationSuccess(Animator animator) {
                if (closing) {
                    closeComplete();
                }
            }
        });
        mOpenCloseAnimator.setDuration(closing ? REVEAL_CLOSE_DURATION : REVEAL_OPEN_DURATION);
        mOpenCloseAnimator.start();
    }

    private void closeComplete() {
        mIsOpen = false;
        mActivity.getDragLayer().removeView(this);
    }
}
