package com.bignerdranch.stockwatcher.model.service.repository;

import android.support.annotation.NonNull;
import android.support.v4.util.LruCache;

import io.reactivex.Observable;

abstract class BaseRepository {

    private final static int CACHE_ENTITY_MAX_SIZE = 50;

    private LruCache<String, Observable<?>> mCache = createLruCache();

    @NonNull
    private LruCache<String, Observable<?>> createLruCache() {
        return new LruCache<>(CACHE_ENTITY_MAX_SIZE);
    }

    <T> Observable<T> cacheObservable(String symbol, Observable<T> observable) {
        Observable<T> cachedObservable = (Observable<T>) mCache.get(symbol);

        if (cachedObservable != null) {
            return cachedObservable;
        }

        cachedObservable = observable;
        updateCache(symbol, cachedObservable);
        return cachedObservable;
    }

    private <T> void updateCache(String stockSymbol, Observable<T> observable) {
        mCache.put(stockSymbol, observable);
    }

    //remove cache for just one symbol
    public void removeCache(String symbol) {
        mCache.remove(symbol);
    }

    //clear cache for all symbols
    public void clearCache() {
        mCache = createLruCache();
    }

}
