package com.noinvasion.uiautomator2;

import android.content.Context;
import android.text.TextUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import ohos.bundle.AbilityInfo;
import ohos.bundle.BundleInfo;
import ohos.bundle.HapModuleInfo;
import ohos.bundle.IBundleManager;

public class HarmonyUtils {
    /**
     * 是否为鸿蒙系统
     *
     * @return true为鸿蒙系统
     */
    public static boolean isHarmonyOs() {
        try {
            Class<?> buildExClass = Class.forName("com.huawei.system.BuildEx");
            Object osBrand = buildExClass.getMethod("getOsBrand").invoke(buildExClass);
            return "Harmony".equalsIgnoreCase(osBrand != null ? osBrand.toString() : null);
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * 获取鸿蒙系统版本号
     *
     * @return 版本号
     */
    public static String getHarmonyVersion() {
        return getProp("hw_sc.build.platform.version");
    }

    /**
     * 获得鸿蒙系统版本号（含小版本号）
     *
     * @return 版本号
     */
    public static String getHarmonyDisplayVersion() {
        return getProp("ro.huawei.build.display.id");
    }

    public static boolean isFoldDisplay() {
        String result = getProp("hw_mc.audio.fold_noti_earpiece");
        return !TextUtils.isEmpty(result) && result.equalsIgnoreCase("true");
    }


    /**
     * 获取属性
     *
     * @param property
     * @return
     */
    private static String getProp(String property) {
        String value = "";
        try {
            Class spClz = Class.forName("android.os.SystemProperties");
            Method method = spClz.getDeclaredMethod("get", String.class);
            value = (String) method.invoke(spClz, property);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return value;
    }

    /**
     * 获取鸿蒙应用的mainAbility
     *
     * @param context
     * @param bundleName 应用包名
     * @return mainAbility
     */
    public static String getHarmonyMainAbility(Context context, String bundleName) {
        String mainAbility = null;
        Context mContext = context.getApplicationContext();

        ClassLoader classLoader = mContext.getClassLoader();
        try {
            Class ohosApplication = Class.forName("ohos.app.Application");
            Object mOhosApplication = ohosApplication.newInstance();

            Class contextDeal = Class.forName("ohos.app.ContextDeal");
            Constructor contextDealConstructor = contextDeal.getConstructor(Context.class, ClassLoader.class);
            Object mContextDeal = contextDealConstructor.newInstance(mContext, classLoader);

            Method setApplication;
            try {
                setApplication = contextDeal.getDeclaredMethod("setApplication", ohosApplication);
            } catch (Exception e) {
                setApplication = contextDeal.getDeclaredMethod("O000000", ohosApplication);
            }
            setApplication.invoke(mContextDeal, mOhosApplication);

            Method attachBaseContext = ohosApplication.getMethod("attachBaseContext", ohos.app.Context.class);
            attachBaseContext.invoke(mOhosApplication, mContextDeal);

            Method getApplicationContext = ohosApplication.getMethod("getApplicationContext");
            ohos.app.Context hContext = (ohos.app.Context) getApplicationContext.invoke(mOhosApplication);

            IBundleManager iBundleManager = hContext.getBundleManager();
            BundleInfo bundleInfo = iBundleManager.getBundleInfo(bundleName, 0);
            if (bundleInfo != null) {

                Method getHapModuleInfo = bundleInfo.getClass().getMethod("getHapModuleInfo", String.class);
                HapModuleInfo moduleInfo = null;
                if (!TextUtils.isEmpty(bundleInfo.getEntryModuleName())) {
                    moduleInfo = (HapModuleInfo) getHapModuleInfo.invoke(bundleInfo, bundleInfo.getEntryModuleName());
                } else {
                    moduleInfo = (HapModuleInfo) getHapModuleInfo.invoke(bundleInfo, "app");
                    if (moduleInfo == null) {
                        for (String moduleName : bundleInfo.getModuleNames()) {
                            moduleInfo = (HapModuleInfo) getHapModuleInfo.invoke(bundleInfo, moduleName);
                            break;
                        }
                    }
                }

                if (moduleInfo != null) {
                    if (!TextUtils.isEmpty(moduleInfo.getMainAbility().getClassName())) {
                        mainAbility = moduleInfo.getMainAbility().getClassName();
                    } else {
                        List<AbilityInfo> abilityInfoList = moduleInfo.getAbilityInfos();
                        if (abilityInfoList.size() > 0) {
                            mainAbility = abilityInfoList.get(0).getClassName();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mainAbility;
    }
}
