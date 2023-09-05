package com.noinvasion.uiautomator2;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Rect {
    private int _top;
    private int _bottom;
    private int _left;
    private int _right;

    public static Rect from(android.graphics.Rect r) {
        Rect rect = new Rect();
        rect._top = r.top;
        rect._bottom = r.bottom;
        rect._left = r.left;
        rect._right = r.right;
        return rect;
    }

    public Rect() {

    }

    public Rect(String bounds) {
        String regex = "\\[(.*?),(.*?)\\]\\[(.*?),(.*?)\\]";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(bounds);
        if (matcher.find()) {
            this._left = Integer.parseInt(Objects.requireNonNull(matcher.group(1)));
            this._top = Integer.parseInt(Objects.requireNonNull(matcher.group(2)));
            this._right = Integer.parseInt(Objects.requireNonNull(matcher.group(3)));
            this._bottom = Integer.parseInt(Objects.requireNonNull(matcher.group(4)));
        }
    }

    public int getTop() {
        return _top;
    }

    public void setTop(int top) {
        this._top = top;
    }

    public int getBottom() {
        return _bottom;
    }

    public void setBottom(int bottom) {
        this._bottom = bottom;
    }

    public int getLeft() {
        return _left;
    }

    public void setLeft(int left) {
        this._left = left;
    }

    public int getRight() {
        return _right;
    }

    public void setRight(int right) {
        this._right = right;
    }

    public android.graphics.Rect toRect() {
        return new android.graphics.Rect(_left, _top, _right, _bottom);
    }

    public int width() {
        return _right - _left;
    }

    public int height() {
        return _bottom - _top;
    }

    public int centerX() {
        return (_left + _right) >> 1;
    }

    public int centerY() {
        return (_top + _bottom) >> 1;
    }

    public boolean isNull() {
        return (this._right - this._left) == 0 && (this._bottom - this._top) == 0;
    }

    public boolean contains(int x, int y) {
        return _left < _right && _top < _bottom && x >= _left && x < _right && y >= _top && y < _bottom;
    }
}
