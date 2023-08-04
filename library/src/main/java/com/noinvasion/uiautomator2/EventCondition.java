package com.noinvasion.uiautomator2;

import android.view.accessibility.AccessibilityEvent;

/**
 * An {@link EventCondition} is a condition which depends on an event or series of events having
 * occurred.
 */
public abstract class EventCondition<R> extends Condition<AccessibilityEvent, Boolean> {
    abstract Boolean apply(AccessibilityEvent event);

    abstract R getResult();
}
