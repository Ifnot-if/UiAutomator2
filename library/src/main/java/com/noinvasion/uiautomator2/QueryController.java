package com.noinvasion.uiautomator2;

import android.app.UiAutomation;
import android.app.UiAutomation.OnAccessibilityEventListener;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.concurrent.TimeoutException;

public class QueryController {

    /**
     * This value has the greatest bearing on the appearance of test execution speeds.
     * This value is used as the minimum time to wait before considering the UI idle after
     * each action.
     */
    private static final long QUIET_TIME_TO_BE_CONSIDERED_IDLE_STATE = 500;//ms

    private final UiAutomation uiAutomation;

    private final Object mLock = new Object();

    private String mLastActivityName = null;

    String mLastTraversedText = "";

    private final OnAccessibilityEventListener mEventListener = event -> {
        synchronized (mLock) {
            switch (event.getEventType()) {
                case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                    // don't trust event.getText(), check for nulls
                    if (event.getText() != null && event.getText().size() > 0) {
                        if (event.getText().get(0) != null) {
                            mLastActivityName = event.getText().get(0).toString();
                        }
                    }
                    break;
                case AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY:
                    // don't trust event.getText(), check for nulls
                    if (event.getText() != null && event.getText().size() > 0) {
                        if (event.getText().get(0) != null) {
                            mLastTraversedText = event.getText().get(0).toString();
                        }
                    }
                    break;
            }
            mLock.notifyAll();
        }
    };

    public QueryController(UiAutomation uiAutomation) {
        this.uiAutomation = uiAutomation;
        uiAutomation.setOnAccessibilityEventListener(mEventListener);
    }

    /**
     * Returns the last text selection reported by accessibility
     * event TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY. One way to cause
     * this event is using a DPad arrows to focus on UI elements.
     */
    public String getLastTraversedText() {
        synchronized (mLock) {
            if (mLastTraversedText.length() > 0) {
                return mLastTraversedText;
            }
        }
        return null;
    }

    /**
     * Clears the last text selection value saved from the TYPE_VIEW_TEXT_SELECTION_CHANGED
     * event
     */
    public void clearLastTraversedText() {
        synchronized (mLock) {
            mLastTraversedText = "";
        }
    }

    /**
     * Gets the root node from accessibility and if it fails to get one it will
     * retry every 250ms for up to 1000ms.
     *
     * @return null if no root node is obtained
     */
    AccessibilityNodeInfo getRootNode() {
        final int maxRetry = 6;
        long waitInterval = 250;
        AccessibilityNodeInfo rootNode = null;
        for (int x = 0; x < maxRetry; x++) {
            rootNode = uiAutomation.getRootInActiveWindow();
            if (rootNode != null) {
                return rootNode;
            }
            if (x < maxRetry - 1) {
                LogUtil.e("Got null root node from accessibility - Retrying...");
                SystemClock.sleep(waitInterval);
                waitInterval *= 2;
            }
        }
        return rootNode;
    }

    /**
     * Last activity to report accessibility events.
     *
     * @return String name of activity
     * @deprecated The results returned should be considered unreliable
     */
    @Deprecated
    public String getCurrentActivityName() {
        synchronized (mLock) {
            return mLastActivityName;
        }
    }

    /**
     * Last package to report accessibility events
     *
     * @return String name of package
     */
    public String getCurrentPackageName() {
        AccessibilityNodeInfo rootNode = getRootNode();
        if (rootNode == null) {
            return null;
        }
        return rootNode.getPackageName() != null ? rootNode.getPackageName().toString() : null;
    }

    /**
     * Waits for the current application to idle.
     * Default wait timeout is 10 seconds
     */
    public void waitForIdle() {
        waitForIdle(Configurator.getInstance().getWaitForIdleTimeout());
    }

    /**
     * Waits for the current application to idle.
     *
     * @param timeout in milliseconds
     */
    public void waitForIdle(long timeout) {
        try {
            uiAutomation.waitForIdle(QUIET_TIME_TO_BE_CONSIDERED_IDLE_STATE, timeout);
        } catch (TimeoutException e) {
            LogUtil.e("Could not detect idle state.");
        }
    }

}

