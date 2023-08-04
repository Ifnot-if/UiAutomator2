package com.noinvasion.uiautomator2;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

import java.util.List;

public class WifiUtil {
    private final WifiManager wifiManager;

    public WifiUtil(Context context) {
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    public boolean setWifiEnabled(boolean enable) {
        return wifiManager.setWifiEnabled(enable);
    }

    public boolean isWifiEnabled() {
        return wifiManager.isWifiEnabled();
    }

    public boolean connect(String netWorkName, String password) {
        List<WifiConfiguration> configurationInfos = wifiManager.getConfiguredNetworks();

        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();

        // 指定对应的SSID
        config.SSID = "\"" + netWorkName + "\"";

        if (password.equals("")) {
//            config.wepKeys[0] = "";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
//            config.wepTxKeyIndex = 0;
        } else {
            config.preSharedKey = "\"" + password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }

        int netId = wifiManager.addNetwork(config);

        if (netId == -1) {
            for (WifiConfiguration wifiConfiguration : configurationInfos) {
                if (config.SSID.equals(wifiConfiguration.SSID)) {
                    netId = wifiConfiguration.networkId;
                }
            }
        }
        wifiManager.enableNetwork(netId, true);
        return wifiManager.reconnect();
    }
}


