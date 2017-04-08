package com.bignerdranch.stockwatcher.model.service.repository;

import com.bignerdranch.stockwatcher.model.service.StockInfoForSymbol;
import com.bignerdranch.stockwatcher.model.service.StockInfoResponse;
import com.bignerdranch.stockwatcher.model.service.StockService;
import com.bignerdranch.stockwatcher.model.service.StockSymbol;
import com.bignerdranch.stockwatcher.model.service.StockSymbolError;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import timber.log.Timber;

public final class StockDataRepository extends BaseRepository {

    private static final String CACHE_PREFIX_GET_STOCK_INFO = "stockInfo";
    private static final String CACHE_PREFIX_GET_STOCK_INFO_FOR_SYMBOL = "getStockInfoForSymbol";
    private static final String CACHE_PREFIX_GET_STOCK_SYMBOLS = "lookupStockSymbols";

    private final StockService mStockService;

    public StockDataRepository(StockService stockService) {
        this.mStockService = stockService;
    }

    public Observable<StockInfoForSymbol> getStockInfoForSymbol(String symbol) {
        Timber.i("method: %s, symbol: %s", CACHE_PREFIX_GET_STOCK_INFO_FOR_SYMBOL, symbol);

        Observable<StockInfoForSymbol> stockInfoForSymbolObservable = Observable.combineLatest(
                lookupStockSymbol(symbol),
                fetchStockInfoFromSymbol(symbol),
                StockInfoForSymbol::new);

        return cacheObservable(CACHE_PREFIX_GET_STOCK_INFO_FOR_SYMBOL + symbol,
                stockInfoForSymbolObservable);
    }

    //stock info request, which depends on the first result from lookup stock request
    private Observable<StockInfoResponse> fetchStockInfoFromSymbol(String symbol) {
        return lookupStockSymbol(symbol)
                .flatMap(stockSymbol -> getStockInfo(stockSymbol.getSymbol()));
    }

    //return a single symbol from the list of symbols, or an error to catch if not.
    private Observable<StockSymbol> lookupStockSymbol(final String symbol) {
        return this.lookupStockSymbols(symbol)
                .doOnNext(stockSymbols -> {
                    if (stockSymbols.isEmpty()) {
                        throw new StockSymbolError(symbol);
                    }
                })
                .flatMap(Observable::fromIterable)
                .take(1);
    }

    private Observable<List<StockSymbol>> lookupStockSymbols(String symbol) {
        Timber.i("%s, symbol: %s", CACHE_PREFIX_GET_STOCK_SYMBOLS, symbol);

        return super.cacheObservable(CACHE_PREFIX_GET_STOCK_SYMBOLS + symbol,
                mStockService.lookupStock(symbol).cache());
    }

    private Observable<StockInfoResponse> getStockInfo(String symbol) {
        Timber.i("method: %s, symbol: %s", CACHE_PREFIX_GET_STOCK_INFO, symbol);

        Observable<StockInfoResponse> observableToCache = mStockService
                .stockInfo(symbol)
                .delay(3, TimeUnit.SECONDS)
                .cache();

        return super.cacheObservable(CACHE_PREFIX_GET_STOCK_INFO + symbol,
                observableToCache);
    }

}
