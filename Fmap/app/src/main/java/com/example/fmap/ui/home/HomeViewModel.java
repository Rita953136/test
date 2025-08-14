package com.example.fmap.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.fmap.data.DailyQuotaManager;
import com.example.fmap.data.MockPlacesRepository;
import com.example.fmap.data.PlacesRepository;
import com.example.fmap.model.Place;
import com.example.fmap.model.SwipeAction;
import com.example.fmap.model.SwipeRecord;
import com.example.fmap.data.RepositoryProvider;
import com.example.fmap.data.FavoritesStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeViewModel extends AndroidViewModel {
    public static final int DAILY_LIMIT = 10;
    private final FavoritesStore favorites;
    private PlacesRepository repo = RepositoryProvider.getPlaces();
    private final DailyQuotaManager quota;

    private final MutableLiveData<List<Place>> places = new MutableLiveData<>();
    private final MutableLiveData<Integer> currentIndex = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> exhausted = new MutableLiveData<>(false);

    private final List<Place> liked = new ArrayList<>();
    private final List<SwipeRecord> swipes = new ArrayList<>();

    private Integer devOverrideLimit = null;

    public HomeViewModel(@NonNull Application app) {
        super(app);
        this.quota = new DailyQuotaManager(app.getApplicationContext());
        this.favorites = new FavoritesStore(app.getApplicationContext());
        this.repo = new MockPlacesRepository(app.getAssets());
    }

    public LiveData<List<Place>> getPlaces() { return places; }
    public LiveData<Integer> getCurrentIndex() { return currentIndex; }
    public LiveData<Boolean> getExhausted() { return exhausted; }

    public void loadRecommendations() {
        int limit = (devOverrideLimit != null) ? devOverrideLimit : DAILY_LIMIT;
        int remain = quota.getRemaining(limit);
        if (remain <= 0) {
            places.setValue(Collections.<Place>emptyList());
            exhausted.setValue(true);
            return;
        }
        List<Place> all = repo.getRecommendations();
        if (all.size() > remain) all = all.subList(0, remain);
        places.setValue(all);
        currentIndex.setValue(0);
        exhausted.setValue(false);
    }

    public void onSwipedRight(Place p) {
        liked.add(p);
        swipes.add(new SwipeRecord(p.getId(), SwipeAction.LIKE, System.currentTimeMillis()));
        favorites.add(p);
        quota.increment();
        advanceAndCheck();
    }

    public void onSwipedLeft(Place p) {
        swipes.add(new SwipeRecord(p.getId(), SwipeAction.DISLIKE, System.currentTimeMillis()));
        quota.increment();
        advanceAndCheck();
    }

    private void advanceAndCheck() {
        Integer idx = currentIndex.getValue();
        if (idx == null) idx = 0;
        currentIndex.setValue(idx + 1);
        int limit = (devOverrideLimit != null) ? devOverrideLimit : DAILY_LIMIT;
        if (quota.getRemaining(limit) <= 0) exhausted.setValue(true);
    }

    // —— 開發者捷徑 —— //
    public void devResetQuotaAndReload() { quota.resetToday(); loadRecommendations(); }
    public void devSetLimitEnabled(boolean enabled) { quota.setEnabled(enabled); loadRecommendations(); }
    public void devSetDailyLimit(Integer newLimitOrNull) { this.devOverrideLimit = newLimitOrNull; loadRecommendations(); }
}
