package cn.iflyos.open.ota;

import android.app.Application;
import android.content.Context;

import com.facebook.stetho.Stetho;
import com.facebook.stetho.okhttp3.StethoInterceptor;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okio.ByteString;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static cn.iflyos.open.ota.BuildConfig.IFLYOS_CLIENT_ID;
import static cn.iflyos.open.ota.BuildConfig.IFLYOS_OTA_SECRET;

public class UpdaterApplication extends Application {

    private UpdaterApi updaterApi;

    public static UpdaterApplication from(Context context) {
        return (UpdaterApplication) context.getApplicationContext();
    }

    private static String sha1(String data) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(data.getBytes());
            return ByteString.of(digest.digest()).hex();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Stetho.initializeWithDefaults(this);

        final String deviceId = DeviceId.get();

        final OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    final long timestamp = System.currentTimeMillis() / 1000;
                    final String nonce = String.valueOf(Math.random());

                    final String signature = sha1(String.format(Locale.US, "%s:%s:%s:%s:%s",
                            IFLYOS_CLIENT_ID, deviceId, timestamp, nonce, IFLYOS_OTA_SECRET));

                    final Request request = chain.request()
                            .newBuilder()
                            .addHeader("X-Client-ID", IFLYOS_CLIENT_ID)
                            .addHeader("X-Device-ID", deviceId)
                            .addHeader("X-Timestamp", String.valueOf(timestamp))
                            .addHeader("X-Nonce", nonce)
                            .addHeader("X-Signature", signature)
                            .build();

                    return chain.proceed(request);
                })
                .addNetworkInterceptor(new StethoInterceptor())
                .build();

        final Retrofit retrofit = new Retrofit.Builder()
                .client(client)
                .baseUrl(BuildConfig.IFLYOS_OTA_SERVER)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        updaterApi = retrofit.create(UpdaterApi.class);
    }

    public UpdaterApi getUpdaterApi() {
        return updaterApi;
    }

}
