package org.noirofficial.tools.manager;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.noirofficial.NoirWallet;
import org.noirofficial.wallet.BRPeerManager;
import org.noirofficial.wallet.BRWalletManager;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 9/19/17.
 * Copyright (c) 2017 breadwallet LLC
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
public class SyncManager {
    public interface onStatusListener {

        void onSyncManagerStarted();

        void onSyncManagerUpdate();

        void onSyncManagerFinished();

        void onSyncFailed();
    }

    private static final String TAG = SyncManager.class.getName();
    private static SyncManager instance;

    private double theProgress;
    private long theLastBlockTimestamp;
    public boolean enabled;

    public double getProgress() {
        return theProgress;
    }

    public long getLastBlockTimestamp() {
        return theLastBlockTimestamp;
    }

    private static Executor executorService = Executors.newSingleThreadScheduledExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());
    private ArrayList<onStatusListener> theListeners = new ArrayList<>();

    public void addListener(onStatusListener aListener) {
        theListeners.add(aListener);
    }

    public void removeListener(onStatusListener aListener) {
        theListeners.remove(aListener);
    }

    public static SyncManager getInstance() {
        if (instance == null) {
            instance = new SyncManager();
        }
        return instance;
    }

    public void startSyncingProgressThread() {
        Log.d(TAG, "startSyncingProgressThread:" + Thread.currentThread().getName());
        if (enabled) {
            return;
        }
        enabled = true;
        handler.postDelayed(startSyncRunnable, 2500);
        executorService.execute(syncRunnable);
        BRWalletManager.getInstance().init();
    }

    private Runnable startSyncRunnable = () -> {
        for (onStatusListener listener : theListeners) {
            listener.onSyncManagerStarted();
        }
    };

    public void stopSyncingProgressThread() {
        Log.d(TAG, "stopSyncingProgressThread");
        if (!enabled) {
            return;
        }
        enabled = false;
        handler.removeCallbacks(startSyncRunnable);
        handler.post(() -> {
            for (onStatusListener listener : theListeners) {
                listener.onSyncManagerFinished();
            }
        });
    }

    public void syncFailed() {
        handler.post(() -> {
            for (onStatusListener listener : theListeners) {
                listener.onSyncFailed();
            }
        });
    }

    private Runnable syncRunnable = new Runnable() {
        @Override
        public void run() {
            SystemClock.sleep(1000);
            theProgress = BRPeerManager.syncProgress(
                    BRSharedPrefs.getStartHeight(NoirWallet.getContext()));
            theLastBlockTimestamp = BRPeerManager.getInstance().getLastBlockTimestamp();
            handler.post(() -> {
                for (onStatusListener listener : theListeners) {
                    listener.onSyncManagerUpdate();
                }
            });
            if (Double.compare(theProgress, 1.0d) != 0 && enabled) {
                executorService.execute(syncRunnable);
            } else {
                stopSyncingProgressThread();
            }
        }
    };

    public boolean isSyncing() {
        return enabled;
    }
}