package org.noirofficial.presenter.activities.utils;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.noirofficial.NoirWallet;
import org.noirofficial.presenter.activities.models.AddressInfo;
import org.noirofficial.presenter.activities.models.MetaModel;
import org.noirofficial.presenter.activities.models.SendAssetResponse;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

public class RetrofitManager {
    public static RetrofitManager instance = new RetrofitManager();
    private Retrofit assetsApi;
    private final Cache cache = new Cache(new File(NoirWallet.getContext().getCacheDir(), "assets"), 1024 * 1024 * 10);
    private Handler handler = new Handler(Looper.getMainLooper());

    private RetrofitManager() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.retryOnConnectionFailure(true);
        builder.cache(cache);
        builder.addNetworkInterceptor(chain -> {
            okhttp3.Response response = chain.proceed(chain.request());

            CacheControl cacheControl = new CacheControl.Builder()
                    .maxAge(365, TimeUnit.DAYS)
                    .build();

            return response.newBuilder()
                    .removeHeader("Cache-Control")
                    .header("Cache-Control", cacheControl.toString())
                    .build();
        });
        assetsApi = new Retrofit.Builder()
                .baseUrl("https://api.digiassets.net:443/v3/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(builder.build())
                .build();
    }

    public void clearCache(String[] addresses) {
        Completable.fromRunnable(() -> {
            synchronized (cache) {
                try {
                    Iterator<String> urls = cache.urls();
                    while (urls.hasNext()) {
                        String url = urls.next();
                        for (String address : addresses) {
                            if (!TextUtils.isEmpty(address) && url.toLowerCase().contains(address.toLowerCase())) {
                                urls.remove();
                            }
                        }
                    }
                } catch (Exception e) {
                    Crashlytics.logException(e);
                    e.printStackTrace();
                }
            }
        }).subscribeOn(Schedulers.io()).subscribe();
    }

    public void clearCache(String address) {
        clearCache(new String[]{address});
    }

    public void clearMetaCache(String assetId) {
        Completable.fromRunnable(() -> {
            synchronized (cache) {
                try {
                    Iterator<String> urls = cache.urls();
                    while (urls.hasNext()) {
                        String url = urls.next();
                        if (url.toLowerCase().contains(assetId.toLowerCase())) {
                            urls.remove();
                        }
                    }
                } catch (IOException e) {
                    Crashlytics.logException(e);
                    e.printStackTrace();
                }
            }
        }).subscribeOn(Schedulers.io()).subscribe();
    }

    public void clearCache() {
        Completable.fromRunnable(() -> {
            synchronized (cache) {
                try {
                    Iterator<String> urls = cache.urls();
                    while (urls.hasNext()) {
                        urls.next();
                        urls.remove();
                    }
                } catch (IOException e) {
                    Crashlytics.logException(e);
                    e.printStackTrace();
                }
            }
        }).subscribeOn(Schedulers.io()).subscribe();
    }

    private interface AssetEndpoints {
        @GET("addressinfo/{address}")
        Observable<AddressInfo> getAssets(@Path("address") String address);

        @GET("assetmetadata/{assetid}/{utxotxid}:{index}")
        Observable<MetaModel> getMeta(@Path("assetid") String assetid, @Path("utxotxid") String utxotxid,
                                      @Path("index") String index);

        @GET("assetmetadata/{assetid}")
        Observable<MetaModel> getMetaSparse(@Path("assetid") String assetid);

        @POST("sendasset/")
        @Headers({"cache-control: no-cache", "Content-Type: application/json"})
        Call<ResponseBody> sendAsset(@Body RequestBody body);

        @POST("broadcast/")
        Call<ResponseBody> broadcastTx(@Body RequestBody body);
    }

    public Observable<AddressInfo> getAssets(String address) {
        AssetEndpoints apiService = assetsApi.create(AssetEndpoints.class);
        return apiService.getAssets(address);
    }

    public Observable<MetaModel> getAssetMeta(String assetid, String utxotdid, String index) {
        AssetEndpoints apiService = assetsApi.create(AssetEndpoints.class);
        return apiService.getMeta(assetid, utxotdid, index);
    }

    public Observable<MetaModel> getAssetMetaSparse(String assetid) {
        AssetEndpoints apiService = assetsApi.create(AssetEndpoints.class);
        return apiService.getMetaSparse(assetid);
    }

    public interface SendAssetCallback {
        void success(SendAssetResponse sendAssetResponse);

        void error(String message, Throwable throwable);
    }

    public void sendAsset(String sendAsset, SendAssetCallback sendAssetCallback) {
        AssetEndpoints apiService = assetsApi.create(AssetEndpoints.class);
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), sendAsset);
        Call<ResponseBody> call = apiService.sendAsset(body);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                handler.post(() -> {
                    try {
                        if (response.code() != 200) {
                            try {
                                JSONObject error = new JSONObject(response.errorBody().string());
                                String message = error.getString("message");
                                Log.d(RetrofitManager.class.getSimpleName(),
                                        "Send Asset Error: " + message);
                                sendAssetCallback.error(message, new Exception("non 200 response for: " + call.request().body().toString()));
                            } catch (Exception e) {
                                Crashlytics.logException(e);
                                sendAssetCallback.error("", e);
                            }
                        } else {
                            Gson gson = new Gson();
                            Type listType = new TypeToken<SendAssetResponse>() {
                            }.getType();
                            SendAssetResponse sendAssetResponse = gson.fromJson(response.body().string(), listType);
                            sendAssetCallback.success(sendAssetResponse);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Crashlytics.logException(t);
                handler.post(() -> sendAssetCallback.error("", t));
            }
        });
    }

    public interface BroadcastTransaction {
        void success(String broadcastResponse);

        void onError(String errorMessage);
    }

    public void broadcast(String txHex, BroadcastTransaction broadcastTransaction) {
        Map<String, Object> jsonParams = new ArrayMap<>();
        jsonParams.put("txHex", txHex);
        RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), (new JSONObject(jsonParams)).toString());
        AssetEndpoints assetService = assetsApi.create(AssetEndpoints.class);
        Call<ResponseBody> call = assetService.broadcastTx(body);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                handler.post(() -> {
                    Log.d(RetrofitManager.class.getSimpleName(), "Status Code: " + response.code());
                    Log.d(RetrofitManager.class.getSimpleName(), "Status Message: " + response.message());
                    if (response.code() == 200) {
                        String txId = "";
                        try {
                            txId = response.body().string();
                        } catch (IOException e) {

                        }
                        broadcastTransaction.success(txId);
                    } else {
                        String errorMessage = "";
                        try {
                            errorMessage = response.errorBody().string();
                        } catch (Exception e) {

                        }
                        broadcastTransaction.onError(errorMessage);
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                handler.post(() -> {
                    broadcastTransaction.onError("");
                    t.printStackTrace();
                });
            }
        });
    }
}