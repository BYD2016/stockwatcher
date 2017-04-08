package com.bignerdranch.stockwatcher;

import android.app.Application;
import android.content.Context;

import lombok.Getter;

public class StockWatcherApplication extends Application {

    @Getter
    private AppComponent appComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        this.initAppComponent();
    }

    public static StockWatcherApplication get(Context context) {
        return (StockWatcherApplication) context.getApplicationContext();
    }

    public static AppComponent getAppComponent(Context context) {
        StockWatcherApplication stockWatcherApplication = StockWatcherApplication.get(context);
        return stockWatcherApplication.getAppComponent();
    }

    private void initAppComponent() {
        this.appComponent = DaggerAppComponent.builder()
                .appModule(new AppModule())
                .build();
    }

}
