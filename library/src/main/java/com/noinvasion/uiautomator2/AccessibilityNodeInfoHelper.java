package com.noinvasion.uiautomator2;


import android.graphics.Rect;
import android.os.Build;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * This class contains static helper methods to work with
 * {@link AccessibilityNodeInfo}
 */
public class AccessibilityNodeInfoHelper {

    /**
     * Returns the node's bounds clipped to the size of the display
     *
     * @param node
     * @param width  pixel width of the display
     * @param height pixel height of the display
     * @return null if node is null, else a Rect containing visible bounds
     */
    public static Rect getVisibleBoundsInScreen(AccessibilityNodeInfo node, int width, int height) {
        if (node == null) {
            return null;
        }
        // targeted node's bounds
        Rect nodeRect = new Rect();
        node.getBoundsInScreen(nodeRect);

        Rect displayRect = new Rect(0, 0, width, height);

        nodeRect.intersect(displayRect);

        // On platforms that give us access to the node's window
        if (UiDevice.API_LEVEL_ACTUAL >= Build.VERSION_CODES.LOLLIPOP) {
            // Trim any portion of the bounds that are outside the window
            Rect window = new Rect();
            if (node.getWindow() != null) {
                node.getWindow().getBoundsInScreen(window);
                nodeRect.intersect(window);
            }
        }

        return nodeRect;
    }
}
