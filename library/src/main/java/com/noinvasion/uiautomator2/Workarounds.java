package com.noinvasion.uiautomator2;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class Workarounds {

    private static Class<?> activityThreadClass;
    private static Object activityThread;
    private final static String TAG = "Workarounds";

    private Workarounds() {
        // not instantiable
    }

    public static void apply() {
        Looper.prepareMainLooper();

//        fillAppInfo();
        fillBaseContext();
//        fillAppContext();
    }

    @SuppressLint("PrivateApi,DiscouragedPrivateApi")
    private static void fillActivityThread() throws Exception {
        if (activityThread == null) {
            // ActivityThread activityThread = new ActivityThread();
            activityThreadClass = Class.forName("android.app.ActivityThread");
            Constructor<?> activityThreadConstructor = activityThreadClass.getDeclaredConstructor();
            activityThreadConstructor.setAccessible(true);
            activityThread = activityThreadConstructor.newInstance();

            // ActivityThread.sCurrentActivityThread = activityThread;
            Field sCurrentActivityThreadField = activityThreadClass.getDeclaredField("sCurrentActivityThread");
            sCurrentActivityThreadField.setAccessible(true);
            sCurrentActivityThreadField.set(null, activityThread);
        }
    }

    @SuppressLint("PrivateApi,DiscouragedPrivateApi")
    private static void fillAppInfo() {
        try {
            fillActivityThread();

            // ActivityThread.AppBindData appBindData = new ActivityThread.AppBindData();
            Class<?> appBindDataClass = Class.forName("android.app.ActivityThread$AppBindData");
            Constructor<?> appBindDataConstructor = appBindDataClass.getDeclaredConstructor();
            appBindDataConstructor.setAccessible(true);
            Object appBindData = appBindDataConstructor.newInstance();

            ApplicationInfo applicationInfo = new ApplicationInfo();
            applicationInfo.packageName = FakeContext.PACKAGE_NAME;

            // appBindData.appInfo = applicationInfo;
            Field appInfoField = appBindDataClass.getDeclaredField("appInfo");
            appInfoField.setAccessible(true);
            appInfoField.set(appBindData, applicationInfo);

            // activityThread.mBoundApplication = appBindData;
            Field mBoundApplicationField = activityThreadClass.getDeclaredField("mBoundApplication");
            mBoundApplicationField.setAccessible(true);
            mBoundApplicationField.set(activityThread, appBindData);
        } catch (Throwable throwable) {
            Log.d(TAG, "Could not fill app info: " + throwable.getMessage());
        }
    }

    @SuppressLint("PrivateApi,DiscouragedPrivateApi")
    private static void fillAppContext() {
        try {
            fillActivityThread();

            Application app = Application.class.newInstance();
            Field baseField = ContextWrapper.class.getDeclaredField("mBase");
            baseField.setAccessible(true);
            baseField.set(app, FakeContext.get());

            // activityThread.mInitialApplication = app;
            Field mInitialApplicationField = activityThreadClass.getDeclaredField("mInitialApplication");
            mInitialApplicationField.setAccessible(true);
            mInitialApplicationField.set(activityThread, app);
        } catch (Throwable throwable) {
            Log.d(TAG, "Could not fill app context: " + throwable.getMessage());
        }
    }

    private static void fillBaseContext() {
        try {
            fillActivityThread();

            Method getSystemContextMethod = activityThreadClass.getDeclaredMethod("getSystemContext");
            Context context = (Context) getSystemContextMethod.invoke(activityThread);
            FakeContext.get().setBaseContext(context);
        } catch (Throwable throwable) {
            Log.d(TAG, "Could not fill base context: " + throwable.getMessage());
        }
    }
}
