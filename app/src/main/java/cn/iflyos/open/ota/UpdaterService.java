package cn.iflyos.open.ota;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import cn.iflyos.open.ota.internal.okhttp3.SimpleOkHttpCallback;
import cn.iflyos.open.ota.internal.pm.PackageManagerCompat;
import cn.iflyos.open.ota.internal.pm.SimplePackageInstallObserver;
import cn.iflyos.open.ota.internal.retrofit2.SimpleRetrofitCallback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Response;

public class UpdaterService extends Service {

    private static final String TAG = "UpdaterService";

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final HashMap<String, ManifestEntity> installed = new HashMap<>();
    private final LinkedList<PackageEntity> queue = new LinkedList<>();

    private UpdaterApplication app;
    private PackageManager pm;
    private ManifestStorage manifest;

    private boolean running = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "onCreate");
        app = UpdaterApplication.from(this);
        pm = getPackageManager();
        manifest = new ManifestStorage(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy");
        running = false;
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand, running? " + running);
        if (running) {
            return START_NOT_STICKY;
        }

        checkForUpdate();

        running = true;
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void checkForUpdate() {
        app.getUpdaterApi().getPackages().enqueue(new SimpleRetrofitCallback<List<PackageEntity>>() {
            @Override
            public void onSuccess(List<PackageEntity> body, retrofit2.Response<List<PackageEntity>> response) {
                handler.post(() -> handleUpdatePackages(body));
            }

            @Override
            public void onHttpFailure(int code, JsonObject body, retrofit2.Response<List<PackageEntity>> response) {
                Log.e(TAG, "Failed getting update: " + code + " " + body);
                stopSelf();
            }

            @Override
            public void onNetworkFailure(Throwable t) {
                Log.e(TAG, "Failed getting update", t);
                stopSelf();
            }
        });
    }

    private void handleUpdatePackages(List<PackageEntity> packages) {
        queue.addAll(packages);

        installed.clear();
        manifest.getManifest(installed);

        runQueue();
    }

    private void runQueue() {
        final PackageEntity pkg = queue.poll();

        if (pkg == null) {
            Log.d(TAG, "Queue emptied, reporting result...");
            reportUpdateResult();
            return;
        }

        handleInstall(pkg);
    }

    private void handleInstall(PackageEntity pkg) {
        if (!pkg.valid()) {
            Log.d(TAG, "Invalid package meta, skip");
            runQueue();
            return;
        }

        final ManifestEntity current = installed.get(pkg.identity);
        if (current == null) {
            Log.d(TAG, "Installing " + pkg.identity);
        } else if (pkg.revision > current.revision) {
            Log.d(TAG, "Updating " + pkg.identity + " " + current.revision + " -> " + pkg.revision);
        } else {
            Log.d(TAG, pkg.identity + " is up-to-date");
            runQueue();
            return;
        }

        final File destDir = new File(getExternalCacheDir(), "apk");
        final File destApk = new File(destDir, String.format(Locale.US, "%s-%d.apk",
                pkg.identity, pkg.revision));

        final Request request;
        try {
            request = new Request.Builder().url(pkg.url).build();
        } catch (Exception e) {
            Log.e(TAG, "Construct request to " + pkg.identity + " (" + pkg.url + ") failed", e);
            runQueue();
            return;
        }

        new OkHttpClient().newCall(request).enqueue(new SimpleOkHttpCallback() {
            @Override
            public void onSuccess(ResponseBody body, okhttp3.Response response) {
                downloadPackage(pkg, body, destApk);
            }

            @Override
            public void onHttpFailure(int code, JsonObject body, okhttp3.Response response) {
                Log.e(TAG, "Failed downloading package " + pkg.identity + ": " + code);
                runQueue();
            }

            @Override
            public void onNetworkFailure(Throwable t) {
                Log.e(TAG, "Failed downloading package " + pkg.identity, t);
                runQueue();
            }
        });
    }

    private void downloadPackage(PackageEntity pkg, ResponseBody body, File apk) {
        byte[] buf = new byte[2048];
        int len;

        //noinspection ResultOfMethodCallIgnored
        apk.getParentFile().mkdirs();

        try (InputStream input = body.byteStream(); FileOutputStream output = new FileOutputStream(apk)) {
            while ((len = input.read(buf)) != -1) {
                output.write(buf, 0, len);
            }
            output.flush();

            Log.d(TAG, "Downloaded " + pkg.identity + ", size: " + apk.length());
            handler.post(() -> doInstall(pkg, apk));
        } catch (IOException e) {
            Log.e(TAG, "Failed downloading package " + pkg.identity, e);
            handler.post(this::runQueue);
        }
    }

    private void doInstall(PackageEntity pkg, File apk) {
        Log.d(TAG, "Installing downloaded " + pkg.identity + ": " + apk.getName());

        final Uri uri = Uri.fromFile(apk);

        try {
            PackageManagerCompat.installPackage(pm, uri, new SimplePackageInstallObserver() {
                @Override
                public void packageInstalled(String packageName, int returnCode) {
                    handlePackageInstalled(pkg, returnCode);
                }
            }, PackageManagerCompat.INSTALL_REPLACE_EXISTING, null);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            Log.e(TAG, "Failed to invoke PackageManager", e);
            stopSelf();
        }
    }

    private void handlePackageInstalled(PackageEntity pkg, int returnCode) {
        if (returnCode == PackageManagerCompat.INSTALL_SUCCEEDED) {
            Log.d(TAG, "Installed " + pkg.identity + " succeeded");
            manifest.updateManifest(ManifestEntity.from(pkg));
            runQueue();
        } else {
            Log.d(TAG, "Installed " + pkg.identity + " failed: " + returnCode);
            runQueue();
        }
    }

    private void reportUpdateResult() {
        final ReportEntity report = ReportEntity.from(manifest.getManifest());
        app.getUpdaterApi().putPackages(report).enqueue(new SimpleRetrofitCallback<Void>() {
            @Override
            public void onSuccess(Void body, Response<Void> response) {
                Log.d(TAG, "Update reported, exit");
                stopSelf();
            }

            @Override
            public void onHttpFailure(int code, JsonObject body, Response<Void> response) {
                Log.e(TAG, "Failed reporting update: " + code + " " + body);
                stopSelf();
            }

            @Override
            public void onNetworkFailure(Throwable t) {
                Log.e(TAG, "Failed reporting update", t);
                stopSelf();
            }
        });
    }

}
