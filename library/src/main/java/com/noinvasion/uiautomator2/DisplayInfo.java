package com.noinvasion.uiautomator2;

import android.graphics.Point;

public final class DisplayInfo {
    private final Point size;
    private final int rotation;

    public DisplayInfo(Point size, int rotation) {
        this.size = size;
        this.rotation = rotation;
    }

    public Point getSize() {
        return size;
    }

    public int getRotation() {
        return rotation;
    }
}

