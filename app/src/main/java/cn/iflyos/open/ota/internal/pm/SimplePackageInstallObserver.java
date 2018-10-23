package cn.iflyos.open.ota.internal.pm;

import android.content.pm.IPackageInstallObserver;
import android.os.IBinder;

public abstract class SimplePackageInstallObserver implements IPackageInstallObserver {
    @Override
    public IBinder asBinder() {
        return null;
    }
}
