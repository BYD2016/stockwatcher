package com.bignerdranch.stockwatcher;

import com.bignerdranch.stockwatcher.model.service.ContentTypeHeaderInterceptor;
import com.bignerdranch.stockwatcher.model.service.ServiceConfig;
import com.bignerdranch.stockwatcher.model.service.repository.StockDataRepository;
import com.bignerdranch.stockwatcher.model.service.StockService;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
class AppModule {

    private static final String STOCK_SERVICE_ENDPOINT = "http://dev.markitondemand.com/MODApis/Api/v2/";
    private static final int HTTP_READ_TIMEOUT = 60;
    private static final int HTTP_CONNECT_TIMEOUT = 60;

    @Provides
    ServiceConfig provideServiceConfig() {
        return new ServiceConfig(STOCK_SERVICE_ENDPOINT);
    }

    @Provides
    HttpLoggingInterceptor provideLoggingInterceptor() {
        return new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY);
    }

    @Provides
    OkHttpClient provideOkHttpClient(HttpLoggingInterceptor loggingInterceptor) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .readTimeout(HTTP_READ_TIMEOUT, TimeUnit.SECONDS)
                .connectTimeout(HTTP_CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .addInterceptor(new ContentTypeHeaderInterceptor());

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(loggingInterceptor);
        }

        return builder.build();
    }

    @Provides
    Retrofit provideRetrofit(OkHttpClient client, ServiceConfig serviceConfig) {
        return new Retrofit.Builder()
                .baseUrl(serviceConfig.getBaseServiceUrl())
                .client(client)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    @Provides
    StockService provideStockService(Retrofit retrofit) {
        return new StockService(retrofit);
    }

    @Provides
    @Singleton
    StockDataRepository provideStockDataRepository(StockService stockService) {
        return new StockDataRepository(stockService);
    }

}
