package com.noinvasion.autotest;

import com.noinvasion.autotest.uiautomator2.UiAutomator;
import com.noinvasion.autotest.uiautomator2.UiDevice;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Test {
    public static void main(String[] args) {
        UiAutomator.connect();
        UiDevice uiDevice = UiDevice.getInstance(UiAutomator.getUiAutomation());
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            uiDevice.dumpWindowHierarchy(os);
            os.close();
            System.out.println(os.toString("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
//        UiObject2 object2 = uiDevice.findObject(By.res("com.android.mms:id/embedded_text_editor"));
//        object2.clear();
//        object2.setText("水电费电风扇");

//        UiAutomator.disconnect();
    }
}
