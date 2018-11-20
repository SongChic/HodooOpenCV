package com.ahqlab.hodooopencv.http;

import com.ahqlab.hodooopencv.domain.HsvValue;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface RetrofitService {
    @POST("color/detector")
    Call<HsvValue> getHsv(@Body HsvValue value);
    @GET("color/test")
    Call<String> test();
}
