package com.responcity.dataplayer.dataplayer.network;

import com.responcity.dataplayer.dataplayer.network.POJOS.SmhiPoint;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface SmhiService {
    @GET("api/category/pmp1.5g/version/2/geotype/point/lon/{lon}/lat/{lat}/data.json")
    Call<SmhiPoint> getPoint(@Path("lon") String lon, @Path("lat") String lat);
}
