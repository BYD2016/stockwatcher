
# stockwatcher
## A modern android development stack showcase

### showcase items:
- rxjava2
- retrofit2
- dagger2
- lombok code gen
- databinding
- the _perfect_ rxjava2 + retrofit2 networking and caching setup
- how to handle ui lifecyle & device configuration changes with network request resume and caching!
- other goodies!

### future showcase items: 
- how to test the retrofit service layer of your app by replaying mock server responses
- robolectric "integration" tests
- assertj & junit unit tests
- MVVM


### Setup:
- Insure you are on Android Studio 2.3.1, and that you have added the Lombok plugin to android studio.
- To install the lombok plugin, navigate to Preferences... > Plugins, type in "Lombok" and click install

## Core Code

### RxUtil
```java
package com.bignerdranch.stockwatcher.util;

import com.bignerdranch.stockwatcher.ui.RxFragment;

import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public final class RxUtil {

    private static final String LOADING_MESSAGE = "Loading";

    private static final ObservableTransformer schedulersTransformer =
            observable -> observable.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread());

    public static <T> ObservableTransformer<T, T> applyUIDefaults(RxFragment rxFragment) {
        return upstream -> upstream
                .compose(RxUtil.addToCompositeDisposable(rxFragment))
                .compose(RxUtil.applySchedulers())
                .compose(RxUtil.applyRequestStatus(rxFragment))
                .compose(RxUtil.showLoadingDialog(rxFragment));
    }

    private static <T> ObservableTransformer<T, T> applySchedulers() {
        return (ObservableTransformer<T, T>) schedulersTransformer;
    }

    private static <T> ObservableTransformer<T, T> addToCompositeDisposable(RxFragment rxFragment) {
        return upstream -> upstream.doOnSubscribe(
                disposable -> rxFragment.getCompositeDisposable().add(disposable));
    }

    private static <T> ObservableTransformer<T, T> applyRequestStatus(RxFragment rxFragment) {
        return upstream -> upstream
                .doOnSubscribe(disposable -> rxFragment.setRequestInProgress(true))
                .doOnTerminate(() -> rxFragment.setRequestInProgress(false));
    }

    private static <T> ObservableTransformer<T, T> showLoadingDialog(RxFragment rxFragment) {
        return observable -> observable
                .doOnSubscribe(disposable -> DialogUtils.showProgressDialog(
                        rxFragment.getFragmentManager(), LOADING_MESSAGE))
                .doOnTerminate(() -> DialogUtils.hideProgressDialog(rxFragment.getFragmentManager()));
    }
}

```

### RxFragment

```java
package com.bignerdranch.stockwatcher.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import io.reactivex.disposables.CompositeDisposable;
import lombok.Getter;
import lombok.Setter;

public abstract class RxFragment extends Fragment {

    private static final String EXTRA_RX_REQUEST_IN_PROGRESS = "EXTRA_RX_REQUEST_IN_PROGRESS";

    @Getter
    @Setter
    private boolean requestInProgress;

    @Getter
    private CompositeDisposable compositeDisposable;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        compositeDisposable = new CompositeDisposable();
        if (savedInstanceState != null) {
            this.requestInProgress = savedInstanceState.getBoolean(EXTRA_RX_REQUEST_IN_PROGRESS, false);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_RX_REQUEST_IN_PROGRESS, requestInProgress);
    }


    @Override
    public void onResume() {
        super.onResume();
        if (isRequestInProgress()) {
            loadRxData();
        }
    }

    @Override
    public void onPause() {
        this.compositeDisposable.clear();
        super.onPause();
    }

    public abstract void loadRxData();
}

```

### StockInfoFragment

```javav
package com.bignerdranch.stockwatcher.ui;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bignerdranch.stockwatcher.R;
import com.bignerdranch.stockwatcher.StockWatcherApplication;
import com.bignerdranch.stockwatcher.databinding.FragmentStockInfoBinding;
import com.bignerdranch.stockwatcher.model.service.StockInfoForSymbol;
import com.bignerdranch.stockwatcher.model.service.repository.StockDataRepository;
import com.bignerdranch.stockwatcher.util.RxUtil;

import java.util.NoSuchElementException;

import javax.inject.Inject;

import io.reactivex.Observable;

public final class StockInfoFragment extends RxFragment {

    @Inject
    StockDataRepository stockDataRepository;

    private FragmentStockInfoBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StockWatcherApplication.getAppComponent(getContext()).inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_stock_info, container, false);

        // Fetch Stock Data
        binding.fetchDataButton.setOnClickListener(v -> {
            binding.errorMessage.setVisibility(View.GONE);
            loadRxData();
        });

        binding.tickerSymbol.setOnEditorActionListener((v, actionId, event) -> {
            if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                loadRxData();
                return true;
            }
            return false;
        });

        // Clear Observable Cache
        binding.clearCacheButton.setOnClickListener(v -> {
            stockDataRepository.clearCache();
            Toast.makeText(getContext(), "observable cache cleared!", Toast.LENGTH_LONG).show();
        });

        return binding.getRoot();
    }

    @Override
    public void loadRxData() {
        Observable.just(binding.tickerSymbol.getText().toString())
                .filter(symbolText -> symbolText.length() > 0)
                .singleOrError()
                .toObservable()
                .flatMap(s -> stockDataRepository.getStockInfoForSymbol(s))
                .compose(RxUtil.applyUIDefaults(StockInfoFragment.this))
                .subscribe(this::displayStockResults, this::displayErrors);
    }

    private void displayErrors(Throwable throwable) {
        String message = throwable.getMessage();
        if (throwable instanceof NoSuchElementException) {
            message = "Enter a stock symbol first!!";
        }

        binding.errorMessage.setVisibility(View.VISIBLE);
        binding.errorMessage.setText(message);
    }

    private void displayStockResults(StockInfoForSymbol stockInfoForSymbol) {
        binding.stockValue.setText(stockInfoForSymbol.toString());
    }
}

```