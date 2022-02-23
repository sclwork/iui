package com.scliang.iui.demo;

import com.scliang.annotations.CreateModel;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

@CreateModel(baseUrl = "https://github.com/")
public interface IVersionApi {
    @GET("scliangml")
    Call<VersionBean> checkVersion(@Query("version") String version, @Query("need") boolean need);
}
