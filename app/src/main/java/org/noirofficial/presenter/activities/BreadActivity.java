package org.noirofficial.presenter.activities;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.LayoutTransition;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.GravityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ToxicBakery.viewpager.transforms.CubeOutTransformer;
import com.appolica.flubber.Flubber;
import com.crashlytics.android.Crashlytics;
import com.google.android.material.appbar.AppBarLayout;
import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import com.scottyab.rootbeer.RootBeer;

import org.apache.commons.codec.binary.Hex;

import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import butterknife.Unbinder;
import org.noirofficial.NoirWallet;
import org.noirofficial.R;
import org.noirofficial.databinding.ActivityBreadBinding;
import org.noirofficial.presenter.activities.adapters.TxAdapter;
import org.noirofficial.presenter.activities.base.BRActivity;
import org.noirofficial.presenter.activities.callbacks.AssetClickCallback;
import org.noirofficial.presenter.activities.callbacks.BRAuthCompletion;
import org.noirofficial.presenter.activities.models.AddressInfo;
import org.noirofficial.presenter.activities.models.AssetModel;
import org.noirofficial.presenter.activities.models.MetaModel;
import org.noirofficial.presenter.activities.models.SendAsset;
import org.noirofficial.presenter.activities.models.SendAssetResponse;
import org.noirofficial.presenter.activities.settings.SecurityCenterActivity;
import org.noirofficial.presenter.activities.settings.SettingsActivity;
import org.noirofficial.presenter.activities.settings.SyncBlockchainActivity;
import org.noirofficial.presenter.activities.utils.ActivityUtils;
import org.noirofficial.presenter.activities.utils.RetrofitManager;
import org.noirofficial.presenter.activities.utils.TransactionUtils;
import org.noirofficial.presenter.adapter.MultiTypeDataBoundAdapter;
import org.noirofficial.presenter.entities.AddressTxSet;
import org.noirofficial.presenter.entities.TxItem;
import org.noirofficial.tools.animation.BRAnimator;
import org.noirofficial.tools.database.Database;
import org.noirofficial.tools.list.items.ListItemTransactionData;
import org.noirofficial.tools.manager.BRApiManager;
import org.noirofficial.tools.manager.BRClipboardManager;
import org.noirofficial.tools.manager.BRSharedPrefs;
import org.noirofficial.tools.manager.JobsHelper;
import org.noirofficial.tools.manager.SyncManager;
import org.noirofficial.tools.manager.TxManager;
import org.noirofficial.tools.manager.TxManager.onStatusListener;
import org.noirofficial.tools.security.AuthManager;
import org.noirofficial.tools.security.BRKeyStore;
import org.noirofficial.tools.sqlite.TransactionDataSource;
import org.noirofficial.tools.threads.BRExecutor;
import org.noirofficial.tools.util.BRConstants;
import org.noirofficial.tools.util.TypesConverter;
import org.noirofficial.tools.util.ViewUtils;
import org.noirofficial.wallet.BRPeerManager;
import org.noirofficial.wallet.BRWalletManager;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * <p/>
 * Created by Noah Seidman <noah@noahseidman.com> on 4/14/18.
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class BreadActivity extends BRActivity implements BRWalletManager.OnBalanceChanged,
        BRPeerManager.OnTxStatusUpdate, BRSharedPrefs.OnIsoChangedListener, AssetClickCallback,
        TransactionDataSource.OnTxAddedListener, SyncManager.onStatusListener, onStatusListener, SwipeRefreshLayout.OnRefreshListener {

    ActivityBreadBinding bindings;
    private Unbinder unbinder;
    private Handler handler = new Handler(Looper.getMainLooper());
    public TxAdapter adapter;
    @BindView(R.id.assets_recycler)
    RecyclerView assetRecycler;
    private MultiTypeDataBoundAdapter assetAdapter;
    private Executor txDataExecutor = Executors.newSingleThreadExecutor();
    private CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindings = DataBindingUtil.setContentView(this, R.layout.activity_bread);
        RootBeer rootBeer = new RootBeer(this);
        if (rootBeer.isRootedWithoutBusyBoxCheck()) {
            bindings.rootBanner.setVisibility(View.VISIBLE);
        }
        bindings.assetRefresh.setOnRefreshListener(this);
        /*bindings.digiSymbolBackground.
                setBackground(AppCompatResources.getDrawable(NoirWallet.getContext(),
                        R.drawable.nav_drawer_header));*/
        bindings.balanceVisibility.setImageResource(
                BRSharedPrefs.getBalanceVisibility(this) ? R.drawable.show_balance : R.drawable.hide_balance);
        bindings.setPagerAdapter(adapter = new TxAdapter(this));
        bindings.txPager.setOffscreenPageLimit(2);
        bindings.txPager.setPageTransformer(true, new CubeOutTransformer());
        bindings.tabLayout.setupWithViewPager(bindings.txPager);
        bindings.contentContainer.getLayoutTransition()
                .enableTransitionType(LayoutTransition.CHANGING);
        ViewUtils.increaceClickableArea(bindings.qrButton);
        ViewUtils.increaceClickableArea(bindings.navDrawer);
        /*ViewUtils.increaceClickableArea(bindings.digiidButton);*/
        unbinder = ButterKnife.bind(this);
        Animator animator = AnimatorInflater.loadAnimator(this, R.animator.from_bottom);
        animator.setTarget(bindings.bottomNavigationLayout);
        animator.setDuration(1000);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
        animator = AnimatorInflater.loadAnimator(this, R.animator.from_top);
        animator.setTarget(bindings.tabLayout);
        animator.setDuration(1000);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
        assetAdapter = new MultiTypeDataBoundAdapter(this, (Object[]) null);
        assetRecycler.setLayoutManager(new LinearLayoutManager(this));
        assetRecycler.setAdapter(assetAdapter);
        bindings.drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerClosed(View drawerView) {
                bindings.assetRefresh.post(() -> bindings.assetRefresh.setRefreshing(false));
            }
        });
    }

    private Runnable nodeConnectionCheck = new Runnable() {
        private String heights;
        private boolean heightsMatch = false;
        @Override
        public void run() {
            txDataExecutor.execute(() -> {

                try {
                    //int currentBlockHeight = BRWalletManager.getLatestBlockData().getInt("height");
                    int walletBlockHeight = BRSharedPrefs.getLastBlockHeight(BreadActivity.this);
                    //heightsMatch = walletBlockHeight == currentBlockHeight;
                    heights = String.format(getString(R.string.block_heights), walletBlockHeight);
                } catch (Exception e) {
                    heightsMatch = false;
                    handler.post(() -> {
                        bindings.heightCheck.setVisibility(View.INVISIBLE);
                    });
                }
                int connectionStatus = BRPeerManager.connectionStatus();
                handler.post(() -> {
                    bindings.heightCheck.setVisibility(View.VISIBLE);
                    bindings.heightCheck.setText(heights);
                    bindings.heightCheck.setTextColor(getResources().getColor(R.color.white));
                    switch (connectionStatus) {
                        case 1:
                            if (!bindings.nodeConnectionStatus.isAnimating()) {
                                bindings.nodeConnectionStatus.setMaxFrame(90);
                                bindings.nodeConnectionStatus.playAnimation();
                            }
                            break;
                        case 2:
                            bindings.nodeConnectionStatus.cancelAnimation();
                            bindings.nodeConnectionStatus.setFrame(50);
                            break;
                        default:
                            bindings.nodeConnectionStatus.cancelAnimation();
                            bindings.nodeConnectionStatus.setFrame(150);
                            break;
                    }
                    handler.postDelayed(nodeConnectionCheck, 5000);
                });
            });
        }
    };

    private Runnable showSyncButtonRunnable = new Runnable() {
        @Override
        public void run() {
            bindings.syncButton.setVisibility(View.VISIBLE);
            Flubber.with()
                    .animation(Flubber.AnimationPreset.FLIP_Y)
                    .interpolator(Flubber.Curve.BZR_EASE_IN_OUT_QUAD)
                    .duration(1000)
                    .autoStart(true)
                    .createFor(findViewById(R.id.sync_button));
        }
    };

    @Override
    public void onSyncManagerStarted() {
        handler.postDelayed(showSyncButtonRunnable, 10000);
        CoordinatorLayout.LayoutParams coordinatorLayoutParams =
                (CoordinatorLayout.LayoutParams) bindings.contentContainer.getLayoutParams();
        coordinatorLayoutParams.setBehavior(null);
        bindings.syncContainer.setVisibility(View.VISIBLE);
        bindings.toolbarLayout.setVisibility(View.GONE);
        bindings.animationView.playAnimation();
        updateSyncText();
    }

    @Override
    public void onSyncManagerUpdate() {
        handler.removeCallbacks(showSyncButtonRunnable);
        bindings.syncButton.setVisibility(View.GONE);
        updateSyncText();
    }

    @Override
    public void onSyncManagerFinished() {
        CoordinatorLayout.LayoutParams coordinatorLayoutParams =
                (CoordinatorLayout.LayoutParams) bindings.contentContainer.getLayoutParams();
        coordinatorLayoutParams.setBehavior(new AppBarLayout.ScrollingViewBehavior());
        handler.removeCallbacks(showSyncButtonRunnable);
        bindings.syncButton.setVisibility(View.GONE);
        bindings.syncContainer.setVisibility(View.GONE);
        bindings.toolbarLayout.setVisibility(View.VISIBLE);
        bindings.animationView.cancelAnimation();
    }

    @Override
    public void onSyncFailed() {
        //Do not clear visual sync state when sync fails
        //Gives the wrong impression that sync is complete
        /*CoordinatorLayout.LayoutParams coordinatorLayoutParams =
                (CoordinatorLayout.LayoutParams) bindings.contentContainer.getLayoutParams();
        coordinatorLayoutParams.setBehavior(new AppBarLayout.ScrollingViewBehavior());
        handler.removeCallbacks(showSyncButtonRunnable);
        bindings.syncButton.setVisibility(View.GONE);
        bindings.syncContainer.setVisibility(View.GONE);
        bindings.toolbarLayout.setVisibility(View.VISIBLE);
        bindings.animationView.cancelAnimation();*/
    }

    private void updateSyncText() {
        Locale current = getResources().getConfiguration().locale;
        Date time = new Date(SyncManager.getInstance().getLastBlockTimestamp() * 1000);

        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.CEILING);

        bindings.syncText.setText(SyncManager.getInstance().getLastBlockTimestamp() == 0
                ? NoirWallet.getContext().getString(R.string.NodeSelector_statusLabel) + ": "
                + NoirWallet.getContext().getString(R.string.SyncingView_connecting)
                : df.format(Double.valueOf(SyncManager.getInstance().getProgress() * 100d)) + "%"
                + " - " + DateFormat.getDateInstance(DateFormat.SHORT, current).format(time)
                + ", " + DateFormat.getTimeInstance(DateFormat.SHORT, current).format(
                time));
    }

    @Override
    public void onTxManagerUpdate(TxItem[] newTxItems) {
        txDataExecutor.execute(() -> {
            BRWalletManager.getInstance().refreshBalance(NoirWallet.getContext());
            if (newTxItems == null || newTxItems.length == 0) {
                return;
            }
            ArrayList<ListItemTransactionData> newTransactions =
                    TransactionUtils.getNewTransactionsData(newTxItems);
            ArrayList<ListItemTransactionData> transactionsToAdd = removeAllExistingEntries(
                    newTransactions);
            if (transactionsToAdd.size() > 0) {
                handler.post(() -> {
                    adapter.getAllAdapter().addTransactions(transactionsToAdd);
                    adapter.getSentAdapter().addTransactions(
                            TransactionUtils.
                                    convertNewTransactionsForAdapter(
                                            TransactionUtils.Adapter.SENT,
                                            transactionsToAdd
                                    ));
                    adapter.getReceivedAdapter().addTransactions(
                            TransactionUtils.
                                    convertNewTransactionsForAdapter(
                                            TransactionUtils.Adapter.RECEIVED,
                                            transactionsToAdd
                                    ));
                    adapter.getAllRecycler().smoothScrollToPosition(0);
                    adapter.getSentRecycler().smoothScrollToPosition(0);
                    adapter.getReceivedRecycler().smoothScrollToPosition(0);
                });
            } else {
                handler.post(() -> {
                    adapter.getAllAdapter().updateTransactions(newTransactions);
                    adapter.getSentAdapter().updateTransactions(newTransactions);
                    adapter.getReceivedAdapter().updateTransactions(newTransactions);
                });
            }

            handler.post(() -> {
                boolean isAssetSend = isPossibleNewAssetSend(transactionsToAdd);
                if (isAssetSend) {
                    RetrofitManager.instance.clearCache(transactionsToAdd.get(0).transactionItem.getTo());
                }
                processTxAssets(
                        new CopyOnWriteArrayList<>(isAssetSend ? adapter.getAllAdapter().getTransactions() : transactionsToAdd),
                        isAssetSend,
                        isAssetSend
                );
            });
        });
    }

    @Override
    public void onRefresh() {
        assetAdapter.clear();
        assetAdapter.notifyDataSetChanged();
        disposables.add(Completable.fromRunnable(() -> {
            RetrofitManager.instance.clearCache();
            processTxAssets(
                    new CopyOnWriteArrayList<>(adapter.getAllAdapter().getTransactions()),
                    true,
                    false
            );
        }).subscribeOn(Schedulers.io()).subscribe());
    }

    private boolean isPossibleNewAssetSend(ArrayList<ListItemTransactionData> newTransactions) {
        return newTransactions.size() == 1 && newTransactions.get(0).transactionItem.getSent() != 0;
    }

    private void processTxAssets(List<ListItemTransactionData> transactions, boolean forceAssetMeta, boolean clearAssetUtxo) {
        HashSet<AddressTxSet> outputs = new HashSet<>();
        HashSet<AddressTxSet> allAddresses = new HashSet<>();
        for (ListItemTransactionData transaction : transactions) {
            if (!transaction.transactionItem.isAsset) {
                continue;
            }
            for (String address : transaction.transactionItem.getTo()) {
                if (!TextUtils.isEmpty(address)) {
                    if (BRWalletManager.addressContainedInWallet(address)) {
                        outputs.add(new AddressTxSet(address, transaction));
                    }
                    allAddresses.add(new AddressTxSet(address, transaction));
                }
            }
            for (String address : transaction.transactionItem.getFrom()) {
                if (!TextUtils.isEmpty(address)) {
                    allAddresses.add(new AddressTxSet(address, transaction));
                }
            }
        }
        List<Observable<MetaModel>> assetObservables = new LinkedList<>();
        List<Observable<MetaModel>> nameObservables = new LinkedList<>();
        for (AddressTxSet set : outputs) {
            assetObservables.add(processAssets(set.address, forceAssetMeta, clearAssetUtxo));
        }
        for (AddressTxSet set : allAddresses) {
            nameObservables.add(processAssetNames(set.address, set.listItemTransactionData, forceAssetMeta));
        }
        Observable<MetaModel> assets = Observable.merge(assetObservables);
        Observable<MetaModel> names = Observable.merge(nameObservables);
        Observable<Boolean> completion = Observable.fromCallable(() -> {
            bindings.assetRefresh.setRefreshing(false);
            if (assetAdapter.getItemCount() > 0 && bindings.noAssetsSwitcher.getDisplayedChild() != 1) {
                bindings.noAssetsSwitcher.setDisplayedChild(1);
            }
            return true;
        }).subscribeOn(AndroidSchedulers.mainThread()).delay(3, TimeUnit.SECONDS);
        disposables.add(Observable.concat(assets, names, completion).delaySubscription(1, TimeUnit.SECONDS).subscribe());
    }

    private Observable<MetaModel> processAssets(@NonNull final String address, boolean forceAssetMeta, boolean clearAssetUtxo) {
        return RetrofitManager.instance.getAssets(address).onErrorResumeNext(Observable.empty()).flatMap(addressInfo -> {
            List<Observable<MetaModel>> metaObservables = new LinkedList<>();
            for (final AddressInfo.Asset asset : addressInfo.getAssets()) {
                if (forceAssetMeta) {
                    RetrofitManager.instance.clearMetaCache(asset.assetId);
                }
                Observable<MetaModel> metaObservable = RetrofitManager.instance.getAssetMeta(asset.assetId, asset.txid, String.valueOf(asset.getIndex()))
                        .onErrorResumeNext(RetrofitManager.instance.getAssetMetaSparse(asset.assetId)).onErrorResumeNext(Observable.fromCallable(() -> {
                            Crashlytics.logException(new Exception("Meta Failure: " + asset.assetId + ", " + asset.txid + ", " + asset.getIndex()));
                            return MetaModel.empty();
                        })).subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread()).doOnNext(metaModel -> {
                            if (metaModel.empty) return;
                            AssetModel assetModel = new AssetModel(metaModel, asset);
                            if (!assetAdapter.containsItem(assetModel)) {
                                assetAdapter.addItem(assetModel);
                            } else if (assetModel.isAggregable()) {
                                addAssetToModel(assetModel, asset, clearAssetUtxo);
                            } else {
                                assetAdapter.addItem(assetModel);
                            }
                            updateAssetCounts();
                        });
                metaObservables.add(metaObservable);
            }
            return Observable.merge(metaObservables);
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    private Observable<MetaModel> processAssetNames(@NonNull final String address,
                                                    @NonNull final ListItemTransactionData listItemTransactionData, boolean forceAssetMeta) {
        return RetrofitManager.instance.getAssets(address).onErrorResumeNext(Observable.empty()).flatMap(addressInfo -> {
            List<Observable<MetaModel>> metaObservables = new LinkedList<>();
            for (final AddressInfo.Asset asset : addressInfo.getAssets(listItemTransactionData.transactionItem.txReversed)) {
                if (forceAssetMeta) {
                    RetrofitManager.instance.clearMetaCache(asset.assetId);
                }
                Observable<MetaModel> metaObservable = RetrofitManager.instance.getAssetMeta(asset.assetId, asset.txid, String.valueOf(asset.getIndex()))
                        .onErrorResumeNext(Observable.empty()).subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread()).doOnNext(metaModel -> {
                            Database.instance.saveAssetName(metaModel.metadataOfIssuence.data.assetName, listItemTransactionData);
                        });
                metaObservables.add(metaObservable);
            }
            return Observable.merge(metaObservables);
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    private void updateAssetCounts() {
        int totalQuantity = 0;
        for (Object object : assetAdapter.getItems()) {
            AssetModel model = (AssetModel) object;
            totalQuantity += model.getAssetQuantityInt();
        }
        String assetCountsText = String.format(getString(R.string.asset_counts), assetAdapter.getItems().size(), totalQuantity);
        bindings.assetCounts.setText(assetCountsText);
    }

    private void addAssetToModel(final AssetModel assetModel, final AddressInfo.Asset asset, boolean clear) {
        AssetModel existingAssetModel =
                (AssetModel) assetAdapter.getItem(assetModel);
        if (existingAssetModel != null) {
            existingAssetModel.addAsset(asset, clear);
        }
    }

    private ArrayList<ListItemTransactionData> removeAllExistingEntries(
            ArrayList<ListItemTransactionData> newTransactions) {
        return new ArrayList<ListItemTransactionData>(newTransactions) {{
            removeAll(adapter.getAllAdapter().getTransactions());
        }};
    }

    private void updateAmounts() {
        handler.post(() -> ActivityUtils.updateNoirDollarValues(
                BreadActivity.this,
                bindings.primaryPrice,
                bindings.secondaryPrice
        ));
    }

    @Override
    public void onStatusUpdate() {
        TxManager.getInstance().updateTxList();
    }

    @Override
    public void onIsoChanged(String iso) {
        updateAmounts();
    }

    @Override
    public void onTxAdded() {
        TxManager.getInstance().updateTxList();
    }

    @Override
    public void onBalanceChanged(final long balance) {
        updateAmounts();
    }

    @Override
    public void showSendConfirmDialog(final String message, final int error, byte[] txHash) {
        BRExecutor.getInstance().forMainThreadTasks().execute(() -> {
            BRAnimator.showBreadSignal(BreadActivity.this,
                    error == 0 ? getString(R.string.Alerts_sendSuccess)
                            : getString(R.string.Alert_error),
                    error == 0 ? getString(R.string.Alerts_sendSuccessSubheader)
                            : message, error == 0 ? R.raw.success_check
                            : R.raw.error_check, () -> {
                        try {
                            getSupportFragmentManager().popBackStack();
                        } catch (IllegalStateException e) {
                        }
                    });
        });
    }

    @Override
    public void onAssetClick(View v, AssetModel assetModel) {
        if (assetModel == null || assetModel.getAssetImage() == null) {
            return;
        }
        String url = assetModel.hasVideo() ? assetModel.getVideoUrl() : assetModel.getAssetImage().url;
        String mime = assetModel.hasVideo() ? "video" : "image";
        AssetImageActivity.Companion.show(this, v.findViewById(R.id.asset_drawable), url, mime);
    }

    @OnClick(R.id.balance_visibility)
    void onBalanceVisibilityToggle(View view) {
        BRSharedPrefs.setBalanceVisibility(
                this,
                !BRSharedPrefs.getBalanceVisibility(this)
        );
        bindings.balanceVisibility.setImageResource(
                BRSharedPrefs.getBalanceVisibility(this) ? R.drawable.show_balance : R.drawable.hide_balance);
        updateAmounts();
        notifyDataSetChangeForAll();
    }

    @OnClick(R.id.nav_drawer)
    void onNavButtonClick(View view) {
        try {
            bindings.drawerLayout.openDrawer(GravityCompat.START);
        } catch (IllegalArgumentException e) {
            //Race condition inflating the hierarchy?
        }
    }

    /*@OnClick(R.id.assets_action)
    public void onAssetsButtonClick(View view) {
        bindings.drawerLayout.openDrawer(GravityCompat.END);
    }*/

    @OnClick(R.id.main_action)
    void onMenuButtonClick(View view) {
        BRAnimator.showMenuFragment(BreadActivity.this);
    }

    /*@OnClick(R.id.digiid_button)
    void onDigiIDButtonClick(View view) {
        BRAnimator.openScanner(this);
    }*/

    @OnClick(R.id.primary_price)
    void onPrimaryPriceClick(View view) {
        BRSharedPrefs.putPreferredBTC(BreadActivity.this, true);
        notifyDataSetChangeForAll();
    }

    @OnClick(R.id.secondary_price)
    void onSecondaryPriceClick(View view) {
        BRSharedPrefs.putPreferredBTC(BreadActivity.this, false);
        notifyDataSetChangeForAll();
    }

    @OnClick(R.id.sync_button)
    void onSyncButtonClick(View view) {
        startActivity(new Intent(BreadActivity.this,
                SyncBlockchainActivity.class));
    }

    @OnClick(R.id.node_connection_status)
    void onNodeConnectionStatusClick(View view) {
        Disposable statusDisposable = Observable.fromCallable(() -> BRPeerManager.connectionStatus())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(connectionStatus -> {
                    switch (connectionStatus) {
                        case 1:
                            Toast.makeText(this, R.string.node_connecting, Toast.LENGTH_SHORT).show();
                            break;
                        case 2:
                            Toast.makeText(this, R.string.node_connected, Toast.LENGTH_SHORT).show();
                            break;
                        default:
                            Toast.makeText(this, R.string.node_disconnected, Toast.LENGTH_SHORT).show();
                            break;
                    }
                });
        disposables.add(statusDisposable);
    }

    @OnClick(R.id.root_banner)
    void onRootBannerClick(View view) {
        ActivityUtils.showJailbrokenDialog(this);
    }

    private void notifyDataSetChangeForAll() {
        adapter.getAllAdapter().notifyDataSetChanged();
        adapter.getSentAdapter().notifyDataSetChanged();
        adapter.getReceivedAdapter().notifyDataSetChanged();
    }

    @OnClick(R.id.security_center)
    void onSecurityCenterClick(View view) {
        startActivity(new Intent(this, SecurityCenterActivity.class));
    }

    @OnClick(R.id.settings)
    void onSettingsClick(View view) {
        startActivity(new Intent(this, SettingsActivity.class));
    }


    @OnClick(R.id.lock)
    void onLockClick(View view) {
        BRAnimator.startBreadActivity(this, true);
    }

    @OnClick(R.id.qr_button)
    void onQRClick(View view) {
        BRAnimator.openScanner(this);
    }

    @OnLongClick(R.id.qr_button)
    boolean onQRLongClick() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"),
                BRActivity.QR_IMAGE_PROCESS);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAmounts();
        BRWalletManager.getInstance().addBalanceChangedListener(this);
        BRPeerManager.getInstance().addStatusUpdateListener(this);
        BRSharedPrefs.addIsoChangedListener(this);
        TxManager.getInstance().addListener(this);
        SyncManager.getInstance().addListener(this);
        BRWalletManager.getInstance().refreshBalance(this);
        JobsHelper.SyncBlockchainJob.scheduleJob();
        JobsHelper.updateRecurringPaymentJobs();
        TxManager.getInstance().updateTxList();
        BRApiManager.getInstance().asyncUpdateCurrencyData(this);
        SyncManager.getInstance().startSyncingProgressThread();
        handler.postDelayed(nodeConnectionCheck, 1000);
        bindings.nodeConnectionStatus.setFrame(150);
    }

    @Override
    protected void onPause() {
        super.onPause();
        BRWalletManager.getInstance().removeListener(this);
        BRPeerManager.getInstance().removeListener(this);
        BRSharedPrefs.removeListener(this);
        TxManager.getInstance().removeListener(this);
        SyncManager.getInstance().removeListener(this);
        SyncManager.getInstance().stopSyncingProgressThread();
        handler.removeCallbacks(nodeConnectionCheck);
        disposables.dispose();
        disposables = new CompositeDisposable();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }

    @Override
    public void onBackPressed() {
        if (bindings.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            handler.post(() -> bindings.drawerLayout.closeDrawer(GravityCompat.START));
        } else if (bindings.drawerLayout.isDrawerOpen(GravityCompat.END)) {
            handler.post(() -> bindings.drawerLayout.closeDrawer(GravityCompat.END));
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onComplete(AuthType authType) {
        super.onComplete(authType);
        switch (authType.type) {
            case SEND_ASSET:
                if (!authType.sendAsset.isValidAmount()) {
                    Log.d(BreadActivity.class.getSimpleName(), "invalid amount");
                    return;
                }
                String message = String.format(getString(R.string.asset_confirm_message),
                        authType.sendAsset.getDestinationAddress(),
                        authType.sendAsset.assetModel.getAssetName(),
                        authType.sendAsset.getDisplayDestinationAmount());
                Gson gson = new Gson();
                final String payload = gson.toJson(authType.sendAsset);
                Log.d(BRActivity.class.getSimpleName(), payload);
                RetrofitManager.instance.sendAsset(payload, new RetrofitManager.SendAssetCallback() {
                    @Override
                    public void success(SendAssetResponse sendAssetResponse) {
                        Log.d(BreadActivity.class.getSimpleName(), sendAssetResponse.toString());
                        AuthManager.getInstance().authPrompt(BreadActivity.this,
                                null,
                                message,
                                new BRAuthCompletion.AuthType(sendAssetResponse, authType.sendAsset));
                    }

                    @Override
                    public void error(String message, Throwable throwable) {
                        BRClipboardManager.putClipboard(BreadActivity.this, "message: " + message + "payload: " + payload);
                        Crashlytics.logException(new Exception("message: " + message + "payload: " + payload));
                        showSendConfirmDialog(1, TextUtils.isEmpty(message) ? getString(R.string.Alerts_sendFailure) : message);
                    }
                });
                break;
            case ASSET_BROADCAST:
                broadcast(authType.sendAssetResponse, authType.sendAsset);
                break;
        }
    }

    private void broadcast(SendAssetResponse sendAssetResponse, SendAsset sendAsset) {
        try {
            byte[] sendAddressHex = Hex.decodeHex(sendAssetResponse.getTxHex().toCharArray());
            byte[] rawSeed = BRKeyStore.getPhrase(NoirWallet.getContext(), BRConstants.ASSETS_REQUEST_CODE);
            byte[] seed = TypesConverter.getNullTerminatedPhrase(rawSeed);
            byte[] transaction = BRWalletManager.parseSignSerialize(sendAddressHex, seed);
            final String txHex = BaseEncoding.base16().encode(transaction);
            Log.d(BRActivity.class.getSimpleName(), "Broadcast Payload: " + txHex);
            RetrofitManager.instance.broadcast(txHex, new RetrofitManager.BroadcastTransaction() {
                @Override
                public void success(String txId) {
                    copyTxToClipboard();
                    if (sendAsset.isCompleteSpend()) {
                        assetAdapter.removeItem(sendAsset.assetModel);
                    }
                    showSendConfirmDialog(0, "");
                    Log.d(BRActivity.class.getSimpleName(), "Broadcast Response: " + txId);
                }

                @Override
                public void onError(String errorMessage) {
                    copyTxToClipboard();
                    Crashlytics.logException(new Exception("tx_hex: " + txHex));
                    showSendConfirmDialog(1, TextUtils.isEmpty(errorMessage) ? getString(R.string.Alerts_sendFailure) : errorMessage);
                }

                private void copyTxToClipboard() {
                    Gson gson = new Gson();
                    final String payload = gson.toJson(sendAsset);
                    BRClipboardManager.putClipboard(BreadActivity.this, payload + ":" + txHex);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showSendConfirmDialog(int error, String message) {
        BRExecutor.getInstance().forMainThreadTasks().execute(() -> {
            BRAnimator.showBreadSignal(BreadActivity.this,
                    error == 0 ? getString(R.string.Alerts_sendSuccess)
                            : getString(R.string.Alert_error),
                    error == 0 ? getString(R.string.Alerts_assetsSendSuccessSubheader)
                            : message, error == 0 ? R.raw.success_check
                            : R.raw.error_check, () -> {
                        try {
                            getSupportFragmentManager().popBackStack();
                        } catch (IllegalStateException e) {
                            e.printStackTrace();
                        }
                    });
        });
    }
}