package com.noinvasion.uiautomator2;


import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.app.UiAutomation.AccessibilityEventFilter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 * UiDevice provides access to state information about the device.
 * You can also use this class to simulate user actions on the device,
 * such as pressing the d-pad or pressing the Home and Menu buttons.
 *
 * @since API Level 16
 */
public class UiDevice implements Searchable {

    // Sometimes HOME and BACK key presses will generate no events if already on
    // home page or there is nothing to go back to, Set low timeouts.
    private static final long KEY_PRESS_EVENT_TIMEOUT = 1000;

    /**
     * keep a reference of {@link Instrumentation} instance
     */
    private final UiAutomation uiAutomation;
    private final QueryController mQueryController;
    private final InteractionController mInteractionController;

    private final DisplayManager mDisplayManager;

    // Singleton instance
    private static UiDevice sInstance;

    // Get wait functionality from a mixin
    private final WaitMixin<UiDevice> mWaitMixin = new WaitMixin<>(this);

    /**
     * A forward-looking API Level for development platform builds
     * <p>
     * This will be the actual API level on a released platform build, and will be last released
     * API Level + 1 on development platform build
     */
    static final int API_LEVEL_ACTUAL = Build.VERSION.SDK_INT + ("REL".equals(Build.VERSION.CODENAME) ? 0 : 1);

    /**
     * @deprecated Should use {@link UiDevice} instead.
     */
    @Deprecated

    /**
     * Private constructor. Clients should use {@link UiDevice}.
     */
    UiDevice(UiAutomation uiAutomation) {

        // Enable multi-window support for API level 21 and up
        // Subscribe to window information
        AccessibilityServiceInfo info = uiAutomation.getServiceInfo();
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS | AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS | AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY | AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS | AccessibilityServiceInfo.DEFAULT;

        uiAutomation.setServiceInfo(info);

        this.uiAutomation = uiAutomation;
        mQueryController = new QueryController(uiAutomation);
        mInteractionController = new InteractionController(uiAutomation);

        Workarounds.apply();
        this.mDisplayManager = (DisplayManager) FakeContext.get().getBaseContext().getSystemService(Context.DISPLAY_SERVICE);

    }

    /**
     * Returns whether there is a match for the given {@code selector} criteria.
     */
    public boolean hasObject(BySelector selector) {
        AccessibilityNodeInfo node = ByMatcher.findMatch(getDisplaySize(), selector, getWindowRoots());
        if (node != null) {
            node.recycle();
            return true;
        }
        return false;
    }

    /**
     * Returns the first object to match the {@code selector} criteria,
     * or null if no matching objects are found.
     */
    public UiObject2 findObject(BySelector selector) {
        UiObject2 object2 = null;
        if (!TextUtils.isEmpty(selector.getXPath())) {
            try {
                Document document = DocumentHelper.parseText(dumpWindowHierarchy());
                Node node = document.selectSingleNode(selector.getXPath());
                if (node != null) {
                    object2 = new UiObject2(this, selector, getAccessibilityNodeInfo(node));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            AccessibilityNodeInfo node = ByMatcher.findMatch(getDisplaySize(), selector, getWindowRoots());
            object2 = node != null ? new UiObject2(this, selector, node) : null;
        }
        return object2;
    }

    /**
     * Returns all objects that match the {@code selector} criteria.
     */
    public List<UiObject2> findObjects(BySelector selector) {
        List<UiObject2> ret = new ArrayList<>();
        if (!TextUtils.isEmpty(selector.getXPath())) {
            try {
                Document document = DocumentHelper.parseText(dumpWindowHierarchy());
                List<Node> nodeList = document.selectNodes(selector.getXPath());
                int size = nodeList.size();
                if (size > 0) {
                    for (int i = 0; i < size; i++) {
                        Node node = nodeList.get(i);
                        ret.add(new UiObject2(this, selector, getAccessibilityNodeInfo(node)));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            for (AccessibilityNodeInfo node : ByMatcher.findMatches(getDisplaySize(), selector, getWindowRoots())) {
                ret.add(new UiObject2(this, selector, node));
            }
        }
        return ret;
    }

    private AccessibilityNodeInfo getAccessibilityNodeInfo(Node node) {

        AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain();

        info.setViewIdResourceName(node.valueOf("@resource-id"));
        info.setText(node.valueOf("@text"));
        info.setClassName(node.valueOf("@class"));
        info.setContentDescription(node.valueOf("@content-desc"));
        info.setPackageName(node.valueOf("@package"));
        info.setClickable(Boolean.parseBoolean(node.valueOf("@clickable")));
        info.setChecked(Boolean.parseBoolean(node.valueOf("@checked")));
        info.setCheckable(Boolean.parseBoolean(node.valueOf("@checkable")));
        info.setEnabled(Boolean.parseBoolean(node.valueOf("@enabled")));
        info.setFocused(Boolean.parseBoolean(node.valueOf("@focused")));
        info.setFocusable(Boolean.parseBoolean(node.valueOf("@focusable")));
        info.setLongClickable(Boolean.parseBoolean(node.valueOf("@long-clickable")));
        info.setSelected(Boolean.parseBoolean(node.valueOf("@selected")));
        info.setScrollable(Boolean.parseBoolean(node.valueOf("@scrollable")));
        info.setVisibleToUser(Boolean.parseBoolean(node.valueOf("@isVisibleToUser")));
        info.setPassword(Boolean.parseBoolean(node.valueOf("@password")));
        info.setBoundsInScreen(new Rect(node.valueOf("@bounds")).toRect());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            info.setHintText(node.valueOf("@hint"));
        }
        return info;
    }

    /**
     * Waits for given the {@code condition} to be met.
     *
     * @param condition The {@link SearchCondition} to evaluate.
     * @param timeout   Maximum amount of time to wait in milliseconds.
     * @return The final result returned by the {@code condition}, or null if the {@code condition}
     * was not met before the {@code timeout}.
     */
    public <R> R wait(SearchCondition<R> condition, long timeout) {
        return mWaitMixin.wait(condition, timeout);
    }

    /**
     * Performs the provided {@code action} and waits for the {@code condition} to be met.
     *
     * @param action    The {@link Runnable} action to perform.
     * @param condition The {@link EventCondition} to evaluate.
     * @param timeout   Maximum amount of time to wait in milliseconds.
     * @return The final result returned by the condition.
     */
    public <R> R performActionAndWait(Runnable action, EventCondition<R> condition, long timeout) {

        AccessibilityEvent event = null;
        try {
            event = uiAutomation.executeAndWaitForEvent(action, new EventForwardingFilter(condition), timeout);
        } catch (TimeoutException e) {
            // Ignore
        }

        if (event != null) {
            event.recycle();
        }

        return condition.getResult();
    }

    /**
     * Proxy class which acts as an {@link AccessibilityEventFilter} and forwards calls to an
     * {@link EventCondition} instance.
     */
    private static class EventForwardingFilter implements AccessibilityEventFilter {
        private final EventCondition<?> mCondition;

        public EventForwardingFilter(EventCondition<?> condition) {
            mCondition = condition;
        }

        @Override
        public boolean accept(AccessibilityEvent event) {
            // Guard against nulls
            return Boolean.TRUE.equals(mCondition.apply(event));
        }
    }

    /**
     * Enables or disables layout hierarchy compression.
     * <p>
     * If compression is enabled, the layout hierarchy derived from the Acessibility
     * framework will only contain nodes that are important for uiautomator
     * testing. Any unnecessary surrounding layout nodes that make viewing
     * and searching the hierarchy inefficient are removed.
     *
     * @param compressed true to enable compression; else, false to disable
     * @since API Level 18
     */
    public void setCompressedLayoutHeirarchy(boolean compressed) {
        AccessibilityServiceInfo info = uiAutomation.getServiceInfo();
        if (compressed) {
            info.flags &= ~AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        } else {
            info.flags |= AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        }
        uiAutomation.setServiceInfo(info);
    }

    /**
     * Retrieves a singleton instance of UiDevice
     *
     * @return UiDevice instance
     * @since API Level 16
     * @deprecated Should use {@link } instead. This version hides
     * UiDevice's dependency on having an Instrumentation reference and is prone to misuse.
     */
    @Deprecated
    public static UiDevice getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("UiDevice singleton not initialized");
        }
        return sInstance;
    }

    /**
     * Retrieves a singleton instance of UiDevice
     *
     * @return UiDevice instance
     */
    public static UiDevice getInstance(UiAutomation uiAutomation) {
        if (sInstance == null) {
            sInstance = new UiDevice(uiAutomation);
        }
        return sInstance;
    }

    /**
     * Returns the display size in dp (device-independent pixel)
     * <p>
     * The returned display size is adjusted per screen rotation. Also this will return the actual
     * size of the screen, rather than adjusted per system decorations (like status bar).
     *
     * @return a Point containing the display size in dp
     */
    public Point getDisplaySizeDp() {
        Display display = getDisplayById();
        Point p = new Point();
        display.getRealSize(p);
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);
        float dpx = p.x / metrics.density;
        float dpy = p.y / metrics.density;
        p.x = Math.round(dpx);
        p.y = Math.round(dpy);
        return p;
    }

    /**
     * Retrieves the product name of the device.
     * <p>
     * This method provides information on what type of device the test is running on. This value is
     * the same as returned by invoking #adb shell getprop ro.product.name.
     *
     * @return product name of the device
     * @since API Level 17
     */
    public String getProductName() {
        return Build.PRODUCT;
    }

    /**
     * Retrieves the text from the last UI traversal event received.
     * <p>
     * You can use this method to read the contents in a WebView container
     * because the accessibility framework fires events
     * as each text is highlighted. You can write a test to perform
     * directional arrow presses to focus on different elements inside a WebView,
     * and call this method to get the text from each traversed element.
     * If you are testing a view container that can return a reference to a
     * Document Object Model (DOM) object, your test should use the view's
     * DOM instead.
     *
     * @return text of the last traversal event, else return an empty string
     * @since API Level 16
     */
    public String getLastTraversedText() {
        return mQueryController.getLastTraversedText();
    }

    /**
     * Clears the text from the last UI traversal event.
     * See {@link #getLastTraversedText()}.
     *
     * @since API Level 16
     */
    public void clearLastTraversedText() {
        mQueryController.clearLastTraversedText();
    }

    /**
     * Simulates a short press on the MENU button.
     *
     * @return true if successful, else return false
     * @since API Level 16
     */
    public boolean pressMenu() {
        return getInteractionController().sendKeyAndWaitForEvent(KeyEvent.KEYCODE_MENU, 0, AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, KEY_PRESS_EVENT_TIMEOUT);
    }

    /**
     * Simulates a short press on the BACK button.
     *
     * @return true if successful, else return false
     * @since API Level 16
     */
    public boolean pressBack() {
        return getInteractionController().sendKeyAndWaitForEvent(KeyEvent.KEYCODE_BACK, 0, AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, KEY_PRESS_EVENT_TIMEOUT);
    }

    /**
     * Simulates a short press on the HOME button.
     *
     * @return true if successful, else return false
     * @since API Level 16
     */
    public boolean pressHome() {
        return getInteractionController().sendKeyAndWaitForEvent(KeyEvent.KEYCODE_HOME, 0, AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, KEY_PRESS_EVENT_TIMEOUT);
    }

    /**
     * Simulates a short press on the SEARCH button.
     *
     * @return true if successful, else return false
     * @since API Level 16
     */
    public boolean pressSearch() {
        return pressKeyCode(KeyEvent.KEYCODE_SEARCH);
    }

    /**
     * Simulates a short press on the CENTER button.
     *
     * @return true if successful, else return false
     * @since API Level 16
     */
    public boolean pressDPadCenter() {
        return pressKeyCode(KeyEvent.KEYCODE_DPAD_CENTER);
    }

    /**
     * Simulates a short press on the DOWN button.
     *
     * @return true if successful, else return false
     * @since API Level 16
     */
    public boolean pressDPadDown() {
        return pressKeyCode(KeyEvent.KEYCODE_DPAD_DOWN);
    }

    /**
     * Simulates a short press on the UP button.
     *
     * @return true if successful, else return false
     * @since API Level 16
     */
    public boolean pressDPadUp() {
        return pressKeyCode(KeyEvent.KEYCODE_DPAD_UP);
    }

    /**
     * Simulates a short press on the LEFT button.
     *
     * @return true if successful, else return false
     * @since API Level 16
     */
    public boolean pressDPadLeft() {
        return pressKeyCode(KeyEvent.KEYCODE_DPAD_LEFT);
    }

    /**
     * Simulates a short press on the RIGHT button.
     *
     * @return true if successful, else return false
     * @since API Level 16
     */
    public boolean pressDPadRight() {
        return pressKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT);
    }

    /**
     * Simulates a short press on the DELETE key.
     *
     * @return true if successful, else return false
     * @since API Level 16
     */
    public boolean pressDelete() {
        return pressKeyCode(KeyEvent.KEYCODE_DEL);
    }

    /**
     * Simulates a short press on the ENTER key.
     *
     * @return true if successful, else return false
     * @since API Level 16
     */
    public boolean pressEnter() {
        return pressKeyCode(KeyEvent.KEYCODE_ENTER);
    }

    /**
     * Simulates a short press using a key code.
     * <p>
     * See {@link KeyEvent}
     *
     * @return true if successful, else return false
     * @since API Level 16
     */
    public boolean pressKeyCode(int keyCode) {
        return getInteractionController().sendKey(keyCode, 0);
    }

    /**
     * Simulates a short press using a key code.
     * <p>
     * See {@link KeyEvent}.
     *
     * @param keyCode   the key code of the event.
     * @param metaState an integer in which each bit set to 1 represents a pressed meta key
     * @return true if successful, else return false
     * @since API Level 16
     */
    public boolean pressKeyCode(int keyCode, int metaState) {
        return getInteractionController().sendKey(keyCode, metaState);
    }

    /**
     * Simulates a short press on the Recent Apps button.
     *
     * @return true if successful, else return false
     * @throws RemoteException
     * @since API Level 16
     */
    public boolean pressRecentApps() throws RemoteException {
        return getInteractionController().toggleRecentApps();
    }

    /**
     * Opens the notification shade.
     *
     * @return true if successful, else return false
     * @since API Level 18
     */
    public boolean openNotification() {
        return getInteractionController().openNotification();
    }

    /**
     * Opens the Quick Settings shade.
     *
     * @return true if successful, else return false
     * @since API Level 18
     */
    public boolean openQuickSettings() {
        return getInteractionController().openQuickSettings();
    }

    /**
     * Gets the width of the display, in pixels. The width and height details
     * are reported based on the current orientation of the display.
     *
     * @return width in pixels or zero on failure
     * @since API Level 16
     */
    public int getDisplayWidth() {
        return getDisplaySize().x;
    }

    /**
     * Gets the height of the display, in pixels. The size is adjusted based
     * on the current orientation of the display.
     *
     * @return height in pixels or zero on failure
     * @since API Level 16
     */
    public int getDisplayHeight() {
        return getDisplaySize().y;
    }

    /**
     * Perform a click at arbitrary coordinates specified by the user
     *
     * @param x coordinate
     * @param y coordinate
     * @return true if the click succeeded else false
     * @since API Level 16
     */
    public boolean click(int x, int y) {
        if (x >= getDisplayWidth() || y >= getDisplayHeight()) {
            return (false);
        }
        return getInteractionController().clickNoSync(x, y);
    }

    /**
     * Performs a swipe from one coordinate to another using the number of steps
     * to determine smoothness and speed. Each step execution is throttled to 5ms
     * per step. So for a 100 steps, the swipe will take about 1/2 second to complete.
     *
     * @param startX
     * @param startY
     * @param endX
     * @param endY
     * @param steps  is the number of move steps sent to the system
     * @return false if the operation fails or the coordinates are invalid
     * @since API Level 16
     */
    public boolean swipe(int startX, int startY, int endX, int endY, int steps) {
        return getInteractionController().swipe(startX, startY, endX, endY, steps);
    }

    /**
     * Performs a swipe from one coordinate to another coordinate. You can control
     * the smoothness and speed of the swipe by specifying the number of steps.
     * Each step execution is throttled to 5 milliseconds per step, so for a 100
     * steps, the swipe will take around 0.5 seconds to complete.
     *
     * @param startX X-axis value for the starting coordinate
     * @param startY Y-axis value for the starting coordinate
     * @param endX   X-axis value for the ending coordinate
     * @param endY   Y-axis value for the ending coordinate
     * @param steps  is the number of steps for the swipe action
     * @return true if swipe is performed, false if the operation fails
     * or the coordinates are invalid
     * @since API Level 18
     */
    public boolean drag(int startX, int startY, int endX, int endY, int steps) {
        return getInteractionController().swipe(startX, startY, endX, endY, steps, true);
    }

    /**
     * Performs a swipe between points in the Point array. Each step execution is throttled
     * to 5ms per step. So for a 100 steps, the swipe will take about 1/2 second to complete
     *
     * @param segments     is Point array containing at least one Point object
     * @param segmentSteps steps to inject between two Points
     * @return true on success
     * @since API Level 16
     */
    public boolean swipe(Point[] segments, int segmentSteps) {
        return swipe(segments, segmentSteps, false);
    }

    /**
     * 滑动（惯性）
     *
     * @param segments      滑动坐标
     * @param segmentSteps  滑动步骤
     * @param isStopInertia 滑动惯性 true or false
     * @return
     */
    public boolean swipe(Point[] segments, int segmentSteps, boolean isStopInertia) {
        if (isStopInertia) {
            int length = segments.length;
            Point[] points = new Point[length + 1];
            for (int i = 0; i < length; i++) {
                points[i] = new Point(segments[i]);
            }
            points[length] = new Point(points[length - 1].x + 1, points[length - 1].y + 1);
            return getInteractionController().swipe(points, segmentSteps);
        } else {
            return getInteractionController().swipe(segments, segmentSteps);
        }
    }

    /**
     * Retrieves the last activity to report accessibility events.
     *
     * @return String name of activity
     * @since API Level 16
     * @deprecated The results returned should be considered unreliable
     */
    @Deprecated
    public String getCurrentActivityName() {
        return mQueryController.getCurrentActivityName();
    }

    /**
     * Retrieves the name of the last package to report accessibility events.
     *
     * @return String name of package
     * @since API Level 16
     */
    public String getCurrentPackageName() {
        return mQueryController.getCurrentPackageName();
    }


    /**
     * Check if the device is in its natural orientation. This is determined by checking if the
     * orientation is at 0 or 180 degrees.
     *
     * @return true if it is in natural orientation
     * @since API Level 17
     */
    public boolean isNaturalOrientation() {
        int ret = getDisplayRotation();
        return ret == UiAutomation.ROTATION_FREEZE_0 || ret == UiAutomation.ROTATION_FREEZE_180;
    }

    /**
     * Returns the current rotation of the display, as defined in {@link Surface}
     *
     * @since API Level 17
     */
    public int getDisplayRotation() {
        return getDisplayById().getRotation();
    }

    /**
     * Disables the sensors and freezes the device rotation at its
     * current rotation state.
     *
     * @throws RemoteException
     * @since API Level 16
     */
    public void freezeRotation() throws RemoteException {
        getInteractionController().freezeRotation();
    }

    /**
     * Re-enables the sensors and un-freezes the device rotation allowing its contents
     * to rotate with the device physical rotation. During a test execution, it is best to
     * keep the device frozen in a specific orientation until the test case execution has completed.
     *
     * @throws RemoteException
     */
    public void unfreezeRotation() throws RemoteException {
        getInteractionController().unfreezeRotation();
    }

    /**
     * Simulates orienting the device to the left and also freezes rotation
     * by disabling the sensors.
     * <p>
     * If you want to un-freeze the rotation and re-enable the sensors
     * see {@link #unfreezeRotation()}.
     *
     * @throws RemoteException
     * @since API Level 17
     */
    public void setOrientationLeft() throws RemoteException {
        getInteractionController().setRotationLeft();
        // we don't need to check for idle on entry for this. We'll sync on exit
    }

    /**
     * Simulates orienting the device to the right and also freezes rotation
     * by disabling the sensors.
     * <p>
     * If you want to un-freeze the rotation and re-enable the sensors
     * see {@link #unfreezeRotation()}.
     *
     * @throws RemoteException
     * @since API Level 17
     */
    public void setOrientationRight() throws RemoteException {
        getInteractionController().setRotationRight();
        // we don't need to check for idle on entry for this. We'll sync on exit
    }

    /**
     * Simulates orienting the device into its natural orientation and also freezes rotation
     * by disabling the sensors.
     * <p>
     * If you want to un-freeze the rotation and re-enable the sensors
     * see {@link #unfreezeRotation()}.
     *
     * @throws RemoteException
     * @since API Level 17
     */
    public void setOrientationNatural() throws RemoteException {
        getInteractionController().setRotationNatural();
        // we don't need to check for idle on entry for this. We'll sync on exit
    }

    /**
     * This method simulates pressing the power button if the screen is OFF else
     * it does nothing if the screen is already ON.
     * <p>
     * If the screen was OFF and it just got turned ON, this method will insert a 500ms delay
     * to allow the device time to wake up and accept input.
     *
     * @throws RemoteException
     * @since API Level 16
     */
    public void wakeUp() throws RemoteException {
        if (getInteractionController().wakeDevice()) {
            // sync delay to allow the window manager to start accepting input
            // after the device is awakened.
            SystemClock.sleep(500);
        }
    }

    /**
     * Checks the power manager if the screen is ON.
     *
     * @return true if the screen is ON else false
     * @throws RemoteException
     * @since API Level 16
     */
    public boolean isScreenOn() throws RemoteException {
        return getInteractionController().isScreenOn();
    }

    /**
     * This method simply presses the power button if the screen is ON else
     * it does nothing if the screen is already OFF.
     *
     * @throws RemoteException
     * @since API Level 16
     */
    public void sleep() throws RemoteException {
        getInteractionController().sleepDevice();
    }

    /**
     * Dump the current window hierarchy.
     */
    public String dumpWindowHierarchy() {
        return AccessibilityNodeInfoDumper.dumpWindowHierarchy(this);
    }

    /**
     * Waits for a window content update event to occur.
     * <p>
     * If a package name for the window is specified, but the current window
     * does not have the same package name, the function returns immediately.
     *
     * @param packageName the specified window package name (can be <code>null</code>).
     *                    If <code>null</code>, a window update from any front-end window will end the wait
     * @param timeout     the timeout for the wait
     * @return true if a window update occurred, false if timeout has elapsed or if the current
     * window does not have the specified package name
     * @since API Level 16
     */
    public boolean waitForWindowUpdate(final String packageName, long timeout) {
        if (packageName != null) {
            if (!packageName.equals(getCurrentPackageName())) {
                return false;
            }
        }
        Runnable emptyRunnable = new Runnable() {
            @Override
            public void run() {
            }
        };
        AccessibilityEventFilter checkWindowUpdate = new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent t) {
                if (t.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                    return packageName == null || packageName.equals(t.getPackageName());
                }
                return false;
            }
        };
        try {
            uiAutomation.executeAndWaitForEvent(emptyRunnable, checkWindowUpdate, timeout);
        } catch (TimeoutException e) {
            return false;
        } catch (Exception e) {
            LogUtil.e("waitForWindowUpdate: general exception from bridge", e);
            return false;
        }
        return true;
    }

    /**
     * Take a screenshot of current window and store it as PNG
     * <p>
     * Default scale of 1.0f (original size) and 90% quality is used
     * The screenshot is adjusted per screen rotation
     *
     * @param storePath where the PNG should be written to
     * @return true if screen shot is created successfully, false otherwise
     * @since API Level 17
     */
    public boolean takeScreenshot(File storePath) {
        return takeScreenshot(storePath, 1.0f, 90);
    }

    /**
     * Take a screenshot of current window and store it as PNG
     * <p>
     * The screenshot is adjusted per screen rotation
     *
     * @param storePath where the PNG should be written to
     * @param scale     scale the screenshot down if needed; 1.0f for original size
     * @param quality   quality of the PNG compression; range: 0-100
     * @return true if screen shot is created successfully, false otherwise
     * @since API Level 17
     */
    public boolean takeScreenshot(File storePath, float scale, int quality) {
        Bitmap screenshot = uiAutomation.takeScreenshot();
        if (screenshot == null) {
            return false;
        }
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(storePath))) {
            screenshot.compress(Bitmap.CompressFormat.PNG, quality, bos);
            bos.flush();
        } catch (IOException ioe) {
            LogUtil.e("failed to save screen shot to file", ioe);
            return false;
        } finally {
            // Ignore
            screenshot.recycle();
        }
        return true;
    }

    /**
     * Retrieves default launcher package name
     *
     * @return package name of the default launcher
     */
    public String getLauncherPackageName() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        PackageManager pm = FakeContext.get().getBaseContext().getPackageManager();
        ResolveInfo resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo.activityInfo.packageName;
    }

    public String getLaunchIntentForPackage(String packageName) {
        Context context = FakeContext.get().getBaseContext();
        if (context == null) {
            return "";
        }

        String component;
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null) {
            component = intent.getComponent().getClassName();
        } else {
            component = HarmonyUtils.getHarmonyMainAbility(context, packageName) + "ShellActivity";
        }
        return component;
    }

    /**
     * Executes a shell command using shell user identity, and return the standard output in string.
     * <p>
     * Calling function with large amount of output will have memory impacts, and the function call
     * will block if the command executed is blocking.
     * <p>Note: calling this function requires API level 21 or above
     *
     * @param cmd the command to run
     * @return the standard output of the command
     * @throws IOException
     * @since API Level 21
     */
    public String executeShellCommand(String cmd) throws IOException {
        ParcelFileDescriptor pfd = uiAutomation.executeShellCommand(cmd);
        byte[] buf = new byte[512];
        int bytesRead;
        FileInputStream fis = new ParcelFileDescriptor.AutoCloseInputStream(pfd);
        StringBuffer stdout = new StringBuffer();
        while ((bytesRead = fis.read(buf)) != -1) {
            stdout.append(new String(buf, 0, bytesRead));
        }
        fis.close();
        return stdout.toString();
    }

    public Display getDisplayById() {
//        return mDisplayManager.getDisplay(0);
        Display display = null;
        Display[] displays = mDisplayManager.getDisplays();
        for (Display d : displays) {
            Point point = new Point();
            try {
                d.getRealSize(point);
            } catch (NullPointerException e) {
                continue;
            }
            display = d;
            break;
        }
        return display;
    }

    public Point getDisplaySize() {
        Point p = new Point();
        Display display = getDisplayById();
        display.getRealSize(p);
        return p;
    }

    /**
     * Returns a list containing the root {@link AccessibilityNodeInfo}s for each active window
     */
    AccessibilityNodeInfo[] getWindowRoots() {
        clearAccessibilityCache();
        Set<AccessibilityNodeInfo> roots = new HashSet<>();
        // 进入触摸模式，用完需立即放开！！！
        setServiceToTouchMode(uiAutomation, true);
        // Start with the active window, which seems to sometimes be missing from the list returned
        // by the UiAutomation.
        String pkgName = "";
        AccessibilityNodeInfo activeRoot = uiAutomation.getRootInActiveWindow();
        if (activeRoot != null) {
            roots.add(activeRoot);
            pkgName = (String) activeRoot.getPackageName();
        }

        // Support multi-window searches for API level 21 and up.
        List<AccessibilityWindowInfo> accessibilityWindowInfo = uiAutomation.getWindows();

        for (AccessibilityWindowInfo window : accessibilityWindowInfo) {
            AccessibilityNodeInfo root = window.getRoot();
            if (root != null) {
                // 过滤包名
                if (pkgName.equals((String) root.getPackageName())) {
                    roots.add(root);
                }
            }
        }
        // 退出触摸模式
        setServiceToTouchMode(uiAutomation, false);
        return roots.toArray(new AccessibilityNodeInfo[0]);
    }

    /**
     * 清理缓存
     */
    private static void clearAccessibilityCache() {
        try {
            final Class<?> c = Class.forName("android.view.accessibility.AccessibilityInteractionClient");
            final Method getInstance = method(c, "getInstance");
            if (getInstance != null) {
                final Object instance = getInstance.invoke(null);
                if (instance != null) {
                    final Method clearCache = method(instance.getClass(), "clearCache");
                    if (clearCache != null) {
                        clearCache.invoke(instance);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 反射方法
     */
    private static Method method(final Class<?> clazz, final String methodName, final Class<?>... parameterTypes) {
        try {
            final Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (Exception e) {
//            final String msg = String.format("error while getting method %s from class %s with parameter types %s", methodName, clazz, Arrays.toString(parameterTypes));
//            LogUtil.e(msg + ":" + e.getMessage());
        }
        return null;
    }

    /**
     * 设置触摸模式的开关
     */
    private static void setServiceToTouchMode(UiAutomation uiAutomation, boolean enable) {
        AccessibilityServiceInfo info = uiAutomation.getServiceInfo();
        if (enable) {
            info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE;
        } else {
            info.flags &= ~AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE;
        }
        uiAutomation.setServiceInfo(info);
    }

    public UiAutomation getUiAutomation() {
        return uiAutomation;
    }

    public QueryController getQueryController() {
        return mQueryController;
    }

    public InteractionController getInteractionController() {
        return mInteractionController;
    }
}

