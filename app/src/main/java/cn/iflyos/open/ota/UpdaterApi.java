package cn.iflyos.open.ota;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;

public interface UpdaterApi {

    @GET("/ota/client/packages")
    Call<List<PackageEntity>> getPackages();

    @PUT("/ota/client/packages")
    Call<Void> putPackages(@Body ReportEntity report);

}
