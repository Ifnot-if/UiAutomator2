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

    // During a pattern selector search, the recursive pattern search
    // methods will track their counts and indexes here.
    private int mPatternCounter = 0;

    // These help show each selector's search context as it relates to the previous sub selector
    // matched. When a compound selector fails, it is hard to tell which part of it is failing.
    // Seeing how a selector is being parsed and which sub selector failed within a long list
    // of compound selectors is very helpful.
    private int mLogIndent = 0;

    private String mLastTraversedText = "";

    private OnAccessibilityEventListener mEventListener = new OnAccessibilityEventListener() {
        @Override
        public void onAccessibilityEvent(AccessibilityEvent event) {
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
                        LogUtil.d("Last text selection reported: " +
                                mLastTraversedText);
                        break;
                    default:
                        break;
                }
                mLock.notifyAll();
            }
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

    private void initializeNewSearch() {
        mPatternCounter = 0;
        mLogIndent = 0;
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

    private String formatLog(String str) {
        StringBuilder l = new StringBuilder();
        for (int space = 0; space < mLogIndent; space++) {
            l.append(". . ");
        }
        if (mLogIndent > 0) {
            l.append(String.format(". . [%d]: %s", mPatternCounter, str));
        } else {
            l.append(String.format(". . [%d]: %s", mPatternCounter, str));
        }
        return l.toString();
    }

}

