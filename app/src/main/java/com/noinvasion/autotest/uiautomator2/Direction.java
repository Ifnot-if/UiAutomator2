package com.noinvasion.autotest.uiautomator2;

/**
 * An enumeration used to specify the primary direction of certain gestures.
 */
public enum Direction {
    LEFT, RIGHT, UP, DOWN;

    private Direction mOpposite;

    static {
        LEFT.mOpposite = RIGHT;
        RIGHT.mOpposite = LEFT;
        UP.mOpposite = DOWN;
        DOWN.mOpposite = UP;
    }

    /**
     * Returns the reverse of the given direction.
     */
    public static Direction reverse(Direction direction) {
        return direction.mOpposite;
    }
}
