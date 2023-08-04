package com.noinvasion.uiautomator2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@SuppressLint({"PrivateApi", "DiscouragedPrivateApi"})
public final class Workarounds {
    private static final String TAG = "Workarounds";

    private static volatile Workarounds instance;

    private Workarounds() {
    }

    public static Workarounds getInstance() {
        if (instance == null) {
            synchronized (Workarounds.class) {
                if (instance == null) {
                    instance = new Workarounds();
                }
            }
        }
        return instance;
    }

    public Context getSystemContext() {
        try {
            // ActivityThread activityThread = new ActivityThread();
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Constructor<?> activityThreadConstructor = activityThreadClass.getDeclaredConstructor();
            activityThreadConstructor.setAccessible(true);
            Object activityThread = activityThreadConstructor.newInstance();

            // ActivityThread.sCurrentActivityThread = activityThread;
            Field sCurrentActivityThreadField = activityThreadClass.getDeclaredField("sCurrentActivityThread");
            sCurrentActivityThreadField.setAccessible(true);
            sCurrentActivityThreadField.set(null, activityThread);

            // ActivityThread.AppBindData appBindData = new ActivityThread.AppBindData();
            Class<?> appBindDataClass = Class.forName("android.app.ActivityThread$AppBindData");
            Constructor<?> appBindDataConstructor = appBindDataClass.getDeclaredConstructor();
            appBindDataConstructor.setAccessible(true);
            Object appBindData = appBindDataConstructor.newInstance();

            ApplicationInfo applicationInfo = new ApplicationInfo();
            applicationInfo.packageName = "com.test.uiautomator2";

            // appBindData.appInfo = applicationInfo;
            Field appInfoField = appBindDataClass.getDeclaredField("appInfo");
            appInfoField.setAccessible(true);
            appInfoField.set(appBindData, applicationInfo);

            // activityThread.mBoundApplication = appBindData;
            Field mBoundApplicationField = activityThreadClass.getDeclaredField("mBoundApplication");
            mBoundApplicationField.setAccessible(true);
            mBoundApplicationField.set(activityThread, appBindData);

            // Context ctx = activityThread.getSystemContext();
            Method getSystemContextMethod = activityThreadClass.getDeclaredMethod("getSystemContext");
            return (Context) getSystemContextMethod.invoke(activityThread);
        } catch (Throwable throwable) {
            // this is a workaround, so failing is not an error
            LogUtil.d("Could not fill app info: " + throwable.getMessage());
        }
        return null;
    }
}
