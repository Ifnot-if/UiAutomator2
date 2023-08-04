package com.noinvasion.autotest.uiautomator2;

import static com.noinvasion.autotest.uiautomator2.AccessibilityNodeInfoHelper.getVisibleBoundsInScreen;

import android.annotation.SuppressLint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Xml;
import android.view.Display;
import android.view.accessibility.AccessibilityNodeInfo;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;

public class AccessibilityNodeInfoDumper {

    private static final String[] NAF_EXCLUDED_CLASSES = new String[]{
            android.widget.GridView.class.getName(), android.widget.GridLayout.class.getName(),
            android.widget.ListView.class.getName(), android.widget.TableLayout.class.getName()
    };
    private static boolean isWebView;

    public static void dumpWindowHierarchy(UiDevice device, OutputStream out) throws IOException {
        long startTime = System.currentTimeMillis();
        XmlSerializer serializer = Xml.newSerializer();
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        serializer.setOutput(out, "UTF-8");

        serializer.startDocument("UTF-8", true);
        serializer.startTag("", "hierarchy");

        Display display = device.getDisplayById();
        Point displayPoint = new Point();
        display.getRealSize(displayPoint);
        serializer.attribute("", "rotation", Integer.toString(display.getRotation()));

        isWebView = false;
        for (AccessibilityNodeInfo root : device.getWindowRoots()) {
            dumpNodeRec(root, serializer, 0, displayPoint.x, displayPoint.y);
        }

        serializer.endTag("", "hierarchy");
        serializer.endDocument();
        LogUtil.d("Fetch time: " + (System.currentTimeMillis() - startTime) + "ms");
    }

    @SuppressLint("DefaultLocale")
    private static void dumpNodeRec(AccessibilityNodeInfo node, XmlSerializer serializer, int index,
                                    int width, int height) throws IOException {
        serializer.startTag("", "node");
        if (!nafExcludedClass(node) && !nafCheck(node)) {
            serializer.attribute("", "NAF", Boolean.toString(true));
        }
        serializer.attribute("", "index", Integer.toString(index));
        serializer.attribute("", "text", safeCharSeqToString(node.getText()));
        serializer.attribute("", "resource-id", safeCharSeqToString(node.getViewIdResourceName()));
        String clazz = safeCharSeqToString(node.getClassName());
        serializer.attribute("", "class", clazz);
        serializer.attribute("", "package", safeCharSeqToString(node.getPackageName()));
        serializer.attribute("", "content-desc", safeCharSeqToString(node.getContentDescription()));
        serializer.attribute("", "checkable", Boolean.toString(node.isCheckable()));
        serializer.attribute("", "checked", Boolean.toString(node.isChecked()));
        serializer.attribute("", "clickable", Boolean.toString(node.isClickable()));
        serializer.attribute("", "enabled", Boolean.toString(node.isEnabled()));
        serializer.attribute("", "focusable", Boolean.toString(node.isFocusable()));
        serializer.attribute("", "focused", Boolean.toString(node.isFocused()));
        serializer.attribute("", "scrollable", Boolean.toString(node.isScrollable()));
        serializer.attribute("", "long-clickable", Boolean.toString(node.isLongClickable()));
        serializer.attribute("", "password", Boolean.toString(node.isPassword()));
        serializer.attribute("", "selected", Boolean.toString(node.isSelected()));
        // isVisibleToUser 属性(是否对用户可见)
        serializer.attribute("", "isVisibleToUser", Boolean.toString(node.isVisibleToUser()));
        serializer.attribute("", "bounds", getVisibleBoundsInScreen(
                node, width, height).toShortString());

        // 用于跟踪已访问的边界
        HashSet<Rect> visitedBounds = new HashSet<>();
        if (!isWebView) {
            isWebView = clazz.toLowerCase().contains("webview");
        }
        // 获取子节点数量
        int count = node.getChildCount();
        for (int i = 0; i < count; i++) {
            if (i == 0) {
                index = 0;
            } else {
                index++;
            }
            AccessibilityNodeInfo child = node.getChild(i);
            // 判断是否有子节点
            if (child != null) {
                Rect rect = getVisibleBoundsInScreen(child, width, height);
                // 判断子节点的rect是否在当前屏幕内
                if (rect.left >= 0 && rect.right >= 0 && rect.bottom >= 0 && rect.left < width && rect.top < height) {
                    // 检查边界是否已经存在
                    if (isWebView && rect.top != 0 && visitedBounds.contains(rect)) {
                        LogUtil.i(String.format("visitedBounds skip: %s ", node));
                        continue;
                    }
                    visitedBounds.add(rect);
                    dumpNodeRec(child, serializer, index, width, height);
                    child.recycle();
                }
            } else {
                LogUtil.i(String.format("Null child %d/%d, parent: %s", i, count, node));
            }
        }
        serializer.endTag("", "node");
    }

    /**
     * The list of classes to exclude my not be complete. We're attempting to
     * only reduce noise from standard layout classes that may be falsely
     * configured to accept clicks and are also enabled.
     *
     * @return true if node is excluded.
     */
    private static boolean nafExcludedClass(AccessibilityNodeInfo node) {
        String className = safeCharSeqToString(node.getClassName());
        for (String excludedClassName : NAF_EXCLUDED_CLASSES) {
            if (className.endsWith(excludedClassName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * We're looking for UI controls that are enabled, clickable but have no
     * text nor content-description. Such controls configuration indicate an
     * interactive control is present in the UI and is most likely not
     * accessibility friendly. We refer to such controls here as NAF controls
     * (Not Accessibility Friendly)
     *
     * @return false if a node fails the check, true if all is OK
     */
    private static boolean nafCheck(AccessibilityNodeInfo node) {
        boolean isNaf = node.isClickable() && node.isEnabled()
                && safeCharSeqToString(node.getContentDescription()).isEmpty()
                && safeCharSeqToString(node.getText()).isEmpty();

        if (!isNaf) {
            return true;
        }

        // check children since sometimes the containing element is clickable
        // and NAF but a child's text or description is available. Will assume
        // such layout as fine.
        return childNafCheck(node);
    }

    /**
     * This should be used when it's already determined that the node is NAF and
     * a further check of its children is in order. A node maybe a container
     * such as LinerLayout and may be set to be clickable but have no text or
     * content description but it is counting on one of its children to fulfill
     * the requirement for being accessibility friendly by having one or more of
     * its children fill the text or content-description. Such a combination is
     * considered by this dumper as acceptable for accessibility.
     *
     * @return false if node fails the check.
     */
    private static boolean childNafCheck(AccessibilityNodeInfo node) {
        int childCount = node.getChildCount();
        for (int x = 0; x < childCount; x++) {
            AccessibilityNodeInfo childNode = node.getChild(x);

            if (!safeCharSeqToString(childNode.getContentDescription()).isEmpty()
                    || !safeCharSeqToString(childNode.getText()).isEmpty()) {
                return true;
            }

            if (childNafCheck(childNode)) {
                return true;
            }
        }
        return false;
    }

    private static String safeCharSeqToString(CharSequence cs) {
        if (cs == null) {
            return "";
        } else {
            return stripInvalidXMLChars(cs);
        }
    }

    private static String stripInvalidXMLChars(CharSequence cs) {
        StringBuilder ret = new StringBuilder();
        char ch;
        for (int i = 0; i < cs.length(); i++) {
            ch = cs.charAt(i);
            // http://www.w3.org/TR/xml11/#charsets
            if ((ch >= 0x1 && ch <= 0x8)
                    || (ch >= 0xB && ch <= 0xC)
                    || (ch >= 0xE && ch <= 0x1F)
                    || (ch >= 0x7F && ch <= 0x84)
                    || (ch >= 0x86 && ch <= 0x9F)
                    || (ch >= 0xFDD0 && ch <= 0xFDDF)) {
                ret.append(".");
            } else {
                ret.append(ch);
            }
        }
        return ret.toString();
    }

}
