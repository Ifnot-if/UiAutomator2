package com.noinvasion.uiautomator2;

import android.annotation.SuppressLint;
import android.app.UiAutomation;
import android.os.HandlerThread;
import android.os.Looper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * @author zzh
 * UiAutomator类用于连接和断开UiAutomation服务，并确保只有一个实例存在。
 */
@SuppressLint({"SoonBlockedPrivateApi", "PrivateApi"})
public class UiAutomator {
    private static UiAutomation uiAutomation;
    private static HandlerThread handlerThread;
    private static volatile boolean isRunning;

    /**
     * 私有构造函数，防止外部实例化该类。
     */
    private UiAutomator() {

    }

    /**
     * 创建一个新的UiAutomation对象并连接服务。
     *
     * @return 创建并连接成功的UiAutomation对象
     * @throws Exception 如果创建或连接过程中出现异常，将抛出异常
     */
    private static UiAutomation createUiAutomation() throws Exception {
        // 创建一个新的HandlerThread，用于处理UiAutomation的连接与断开
        handlerThread = new HandlerThread("UiAutomationHandlerThread");
        handlerThread.start();

        // 反射创建UiAutomationConnection对象
        Class<?> uiAutomationConnection = Class.forName("android.app.UiAutomationConnection");
        Constructor<?> newInstance = uiAutomationConnection.getDeclaredConstructor();
        newInstance.setAccessible(true);
        Object connection = newInstance.newInstance();

        // 反射创建UiAutomation对象
        Class<?> iUiAutomationConnection = Class.forName("android.app.IUiAutomationConnection");
        Constructor<?> newUiAutomation = UiAutomation.class.getDeclaredConstructor(Looper.class, iUiAutomationConnection);
        return (UiAutomation) newUiAutomation.newInstance(handlerThread.getLooper(), connection);
    }

    /**
     * 连接UiAutomation服务。
     */
    public static synchronized void connect() {
        if (!isRunning) {
            try {
                // 创建并连接UiAutomation服务
                uiAutomation = createUiAutomation();
                Method connect = UiAutomation.class.getDeclaredMethod("connect");
                connect.invoke(uiAutomation);
                isRunning = true;
                LogUtil.d("connect success");
            } catch (Exception e) {
                // 连接失败时记录异常信息并断开服务
                LogUtil.e("connect", e);
                disconnect();
            }
        }
    }

    /**
     * 断开UiAutomation服务。
     */
    public static synchronized void disconnect() {
        if (uiAutomation != null) {
            try {
                // 断开UiAutomation服务
                Method disconnect = UiAutomation.class.getDeclaredMethod("disconnect");
                disconnect.invoke(uiAutomation);
                LogUtil.d("disconnect success");
            } catch (Exception e) {
                // 断开失败时记录异常信息
                LogUtil.e("disconnect", e);
            }
            // 清空UiAutomation对象
            uiAutomation = null;
        }
        if (handlerThread != null) {
            // 结束HandlerThread线程
            handlerThread.quit();
        }
        // 标识UiAutomation已断开
        isRunning = false;
    }

    public static UiAutomation getUiAutomation() {
        return uiAutomation;
    }

    /**
     * 判断UiAutomation是否正在运行。
     *
     * @return 如果UiAutomation正在运行，则返回true；否则返回false。
     */
    public static synchronized boolean isRunning() {
        return isRunning;
    }
}