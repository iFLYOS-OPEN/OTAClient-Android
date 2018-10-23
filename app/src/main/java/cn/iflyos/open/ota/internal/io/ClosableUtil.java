package cn.iflyos.open.ota.internal.io;

import java.io.Closeable;

public class ClosableUtil {

    public static void safeClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }

}
