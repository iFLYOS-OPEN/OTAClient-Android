package cn.iflyos.open.ota.internal.pm;

import android.content.pm.IPackageDeleteObserver;
import android.os.IBinder;

public abstract class SimplePackageDeleteObserver implements IPackageDeleteObserver {
    @Override
    public IBinder asBinder() {
        return null;
    }
}
