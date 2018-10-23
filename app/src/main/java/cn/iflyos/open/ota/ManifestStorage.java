package cn.iflyos.open.ota;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ManifestStorage {

    private static final String TAG = "ManifestStorage";

    private static final Type MANIFEST_TYPE = new TypeToken<List<ManifestEntity>>() {
    }.getType();

    private final Gson gson = new Gson();
    private final File storage;

    private final Object lock = new Object();

    ManifestStorage(Context context) {
        storage = new File(context.getFilesDir(), "manifest.json");
    }

    List<ManifestEntity> getManifest() {
        synchronized (lock) {
            try (final FileReader reader = new FileReader(storage)) {
                final List<ManifestEntity> manifest = gson.fromJson(reader, MANIFEST_TYPE);
                return manifest == null ? new ArrayList<>() : manifest;
            } catch (FileNotFoundException e) {
                Log.w(TAG, "Local manifest not found, will generate next time");
                return new ArrayList<>();
            } catch (Exception e) {
                Log.w(TAG, "Failed reading local manifest", e);
                return new ArrayList<>();
            }
        }
    }

    void getManifest(Map<String, ManifestEntity> map) {
        for (ManifestEntity pkg : getManifest()) {
            map.put(pkg.identity, pkg);
        }
    }

    void updateManifest(ManifestEntity pkg) {
        synchronized (lock) {
            final Map<String, ManifestEntity> map = new HashMap<>();
            getManifest(map);

            final ManifestEntity current = map.get(pkg.identity);
            if (current != null && pkg.revision <= current.revision) {
                return;
            }

            map.put(pkg.identity, pkg);

            final List<ManifestEntity> list = new ArrayList<>(map.values());
            try (final FileWriter writer = new FileWriter(storage)) {
                gson.toJson(list, MANIFEST_TYPE, writer);
                writer.flush();
                Log.d(TAG, "Local manifest updated");
            } catch (IOException e) {
                Log.w(TAG, "Failed writing local manifest", e);
            }
        }
    }

}
