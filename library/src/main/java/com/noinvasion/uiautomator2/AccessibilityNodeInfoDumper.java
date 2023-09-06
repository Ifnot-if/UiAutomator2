package com.noinvasion.uiautomator2;

import android.annotation.SuppressLint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.text.TextUtils;
import android.util.Xml;
import android.view.Display;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import androidx.annotation.DoNotInline;
import androidx.annotation.RequiresApi;

import org.xmlpull.v1.XmlSerializer;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Pattern;

public class AccessibilityNodeInfoDumper {
    private final static String TAG = "AccessibilityNodeInfoDumper";
    private static final String[] NAF_EXCLUDED_CLASSES = new String[]{
            android.widget.GridView.class.getName(), android.widget.GridLayout.class.getName(),
            android.widget.ListView.class.getName(), android.widget.TableLayout.class.getName()
    };
    private static final Pattern PATTERN_BLANK = Pattern.compile("\t|\r|\n");
    private static boolean isWebView;
    private static Map<String, Integer> map;

    public static String dumpWindowHierarchy(UiDevice device) {
        long startTime = System.currentTimeMillis();
        StringWriter xmlDump = new StringWriter();
        try {
            XmlSerializer serializer = Xml.newSerializer();
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.setOutput(xmlDump);

            serializer.startDocument("UTF-8", true);
            serializer.startTag("", "hierarchy");

            Display display = device.getDisplayById();
            Point displayPoint = new Point();
            display.getRealSize(displayPoint);
            serializer.attribute("", "rotation", Integer.toString(display.getRotation()));

            isWebView = false;
            map = new HashMap<>();
            for (AccessibilityNodeInfo root : device.getWindowRoots()) {
                dumpNodeRec(root, serializer, 0, displayPoint.x, displayPoint.y, 1, 1, 1);
            }

            serializer.endTag("", "hierarchy");
            serializer.endDocument();
        } catch (Exception e) {
            LogUtil.e(TAG, e);
        }
        LogUtil.d("Fetch time: " + (System.currentTimeMillis() - startTime) + "ms");
        return xmlDump.toString();
    }

    @SuppressLint("DefaultLocale")
    private static void dumpNodeRec(AccessibilityNodeInfo node, XmlSerializer serializer, int index,
                                    int width, int height, int idSequence, int textSequence, int classSequence) throws Exception {
        serializer.startTag("", "node");
        if (!nafExcludedClass(node) && !nafCheck(node)) {
            serializer.attribute("", "NAF", Boolean.toString(true));
        }
        serializer.attribute("", "index", Integer.toString(index));

        String resourceID = safeCharSeqToString(node.getViewIdResourceName());
        serializer.attribute("", "resource-id", resourceID);
        if (!TextUtils.isEmpty(resourceID)) {
            if (map.containsKey(resourceID)) {
                idSequence = map.get(resourceID);
                idSequence++;
            } else {
                idSequence = 1;
            }
            map.put(resourceID, idSequence);
            serializer.attribute("", "id-sequence", Integer.toString(idSequence));
        }

        String text = safeCharSeqToString(node.getText());
        serializer.attribute("", "text", text);
        if (!TextUtils.isEmpty(text)) {
            if (map.containsKey(text)) {
                textSequence = map.get(text);
                textSequence++;
            } else {
                textSequence = 1;
            }
            map.put(text, textSequence);
            serializer.attribute("", "text-sequence", Integer.toString(textSequence));
        }

        String clazz = safeCharSeqToString(node.getClassName());
        serializer.attribute("", "class", clazz);
        if (!TextUtils.isEmpty(clazz)) {
            if (map.containsKey(clazz)) {
                classSequence = map.get(clazz);
                classSequence++;
            } else {
                classSequence = 1;
            }
            map.put(clazz, classSequence);
            serializer.attribute("", "class-sequence", Integer.toString(classSequence));
        }

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

        if (Build.VERSION.SDK_INT >= 26) {
            serializer.attribute("", "hint", safeCharSeqToString(Api26Impl.getHintText(node)));
        }
        if (Build.VERSION.SDK_INT >= 30) {
            serializer.attribute("", "display-id",
                    Integer.toString(Api30Impl.getDisplayId(node)));
        }

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
                        LogUtil.d(String.format("visitedBounds skip: %s ", node));
                        continue;
                    }
                    visitedBounds.add(rect);
                    dumpNodeRec(child, serializer, index, width, height, idSequence, textSequence, classSequence);
                    child.recycle();
                }
            } else {
                LogUtil.d(String.format("Null child %d/%d, parent: %s", i, count, node));
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
            if (childNode != null) {
                if (!safeCharSeqToString(childNode.getContentDescription()).isEmpty() ||
                        !safeCharSeqToString(childNode.getText()).isEmpty()) {
                    return true;
                }
                if (childNafCheck(childNode)) {
                    return true;
                }
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

        return PATTERN_BLANK.matcher(ret.toString()).replaceAll(" ");
    }

    /**
     * Returns the node's bounds clipped to the size of the display
     *
     * @param node
     * @param width  pixel width of the display
     * @param height pixel height of the display
     * @return null if node is null, else a Rect containing visible bounds
     */
    @SuppressLint("CheckResult")
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

    @RequiresApi(26)
    static class Api26Impl {
        @DoNotInline
        static String getHintText(AccessibilityNodeInfo accessibilityNodeInfo) {
            CharSequence chars = accessibilityNodeInfo.getHintText();
            return (chars != null) ? chars.toString() : null;
        }
    }

    @RequiresApi(30)
    static class Api30Impl {
        @DoNotInline
        static int getDisplayId(AccessibilityNodeInfo accessibilityNodeInfo) {
            AccessibilityWindowInfo windowInfo = accessibilityNodeInfo.getWindow();
            if (windowInfo == null) {
                LogUtil.d("Api30Impl windowInfo == null , return 0");
                return 0;
            } else {
                return windowInfo.getDisplayId();
            }
        }
    }

}
