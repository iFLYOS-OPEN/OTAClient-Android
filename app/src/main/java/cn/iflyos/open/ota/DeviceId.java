package cn.iflyos.open.ota;

import java.io.File;
import java.io.IOException;

import cn.iflyos.open.ota.internal.io.ClosableUtil;
import okhttp3.internal.io.FileSystem;
import okio.BufferedSource;
import okio.Okio;

public class DeviceId {

    private static final String WLAN_ADDRESS_FILE = "/sys/class/net/wlan0/address";

    // TODO: 厂商应该根据自己设备重新实现这个方法
    public static String get() {
        BufferedSource source = null;
        try {
            source = Okio.buffer(FileSystem.SYSTEM.source(new File(WLAN_ADDRESS_FILE)));
            return source.readUtf8().trim().replace(":", "").toLowerCase();
        } catch (IOException e) {
            return "";
        } finally {
            ClosableUtil.safeClose(source);
        }
    }

}
