package cn.iflyos.open.ota.internal.pm;

import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.PackageManager;
import android.net.Uri;

import java.lang.reflect.InvocationTargetException;

public class PackageManagerCompat {

    public static final int INSTALL_REPLACE_EXISTING = 0x00000002;
    public static final int INSTALL_ALLOW_TEST = 0x00000004;

    public static final int INSTALL_SUCCEEDED = 1;

    public static void installPackage(
            PackageManager pm,
            Uri packageURI,
            IPackageInstallObserver observer,
            int flags,
            String installerPackageName)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        pm.getClass()
                .getMethod("installPackage", Uri.class, IPackageInstallObserver.class, int.class, String.class)
                .invoke(pm, packageURI, observer, flags, installerPackageName);
    }

    public static void deletePackage(
            PackageManager pm,
            String packageName,
            IPackageDeleteObserver observer,
            int flags)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        pm.getClass()
                .getMethod("deletePackage", String.class, IPackageDeleteObserver.class, int.class)
                .invoke(pm, packageName, observer, flags);
    }

}
