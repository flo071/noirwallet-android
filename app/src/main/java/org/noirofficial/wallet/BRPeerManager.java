package org.noirofficial.wallet;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import org.noirofficial.NoirWallet;
import org.noirofficial.presenter.entities.BlockEntity;
import org.noirofficial.presenter.entities.PeerEntity;
import org.noirofficial.tools.animation.BRAnimator;
import org.noirofficial.tools.manager.BRSharedPrefs;
import org.noirofficial.tools.manager.SyncManager;
import org.noirofficial.tools.sqlite.MerkleBlockDataSource;
import org.noirofficial.tools.sqlite.PeerDataSource;
import org.noirofficial.tools.threads.BRExecutor;
import org.noirofficial.tools.util.TrustedNode;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 12/10/15.
 * Copyright (c) 2016 breadwallet LLC
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

public class BRPeerManager {
    public static final String TAG = BRPeerManager.class.getName();
    private static BRPeerManager instance;

    private static List<OnTxStatusUpdate> statusUpdateListeners;

    private BRPeerManager() {
        statusUpdateListeners = new ArrayList<>();
    }

    public static BRPeerManager getInstance() {
        if (instance == null) {
            instance = new BRPeerManager();
        }
        return instance;
    }

    /**
     * void BRPeerManagerSetCallbacks(BRPeerManager *manager, void *info,
     * void (*syncStarted)(void *info),
     * void (*syncSucceeded)(void *info),
     * void (*syncFailed)(void *info, BRPeerManagerError error),
     * void (*txStatusUpdate)(void *info),
     * void (*saveBlocks)(void *info, const BRMerkleBlock blocks[], size_t count),
     * void (*savePeers)(void *info, const BRPeer peers[], size_t count),
     * int (*networkIsReachable)(void *info))
     */

    public static void syncStarted() {
        Log.d(TAG, "syncStarted: " + Thread.currentThread().getName());
        Context ctx = NoirWallet.getContext();
        int startHeight = BRSharedPrefs.getStartHeight(ctx);
        int lastHeight = BRSharedPrefs.getLastBlockHeight(ctx);
        if (startHeight > lastHeight) BRSharedPrefs.putStartHeight(ctx, lastHeight);
    }

    public static void syncSucceeded() {
        Log.d(TAG, "syncSucceeded");
        final Context app = NoirWallet.getContext();
        if (app == null) return;
        BRSharedPrefs.putLastSyncTime(app, System.currentTimeMillis());
        BRSharedPrefs.putAllowSpend(app, true);
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(
                () -> BRSharedPrefs.putStartHeight(app, getCurrentBlockHeight()));
    }

    public static void syncFailed() {
        Log.d(TAG, "syncFailed");
        SyncManager.getInstance().syncFailed();
        Context ctx = NoirWallet.getContext();
        if (ctx == null) return;
    }

    public static void txStatusUpdate() {
        Log.d(TAG, "txStatusUpdate");

        for (OnTxStatusUpdate listener : statusUpdateListeners) {
            if (listener != null) listener.onStatusUpdate();
        }
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(
                () -> updateLastBlockHeight(getCurrentBlockHeight()));
    }

    public static void corruptBlocks() {
        BRWalletManager.getInstance().wipeBlockAndTrans(NoirWallet.getContext(),
                () -> BRAnimator.startBreadActivity(NoirWallet.getContext(),
                        false));
    }

    public static void saveBlocks(final BlockEntity[] blockEntities, final boolean replace) {
        Log.d(TAG, "saveBlocks: " + blockEntities.length);

        final Context ctx = NoirWallet.getContext();
        if (ctx == null) return;
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(() -> {
            if (replace) MerkleBlockDataSource.getInstance(ctx).deleteAllBlocks();
            MerkleBlockDataSource.getInstance(ctx).putMerkleBlocks(blockEntities);
        });
    }

    public static void savePeers(final PeerEntity[] peerEntities, final boolean replace) {
        Log.d(TAG, "savePeers: " + peerEntities.length);
        final Context ctx = NoirWallet.getContext();
        if (ctx == null) return;
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(() -> {
            if (replace) PeerDataSource.getInstance(ctx).deleteAllPeers();
            PeerDataSource.getInstance(ctx).putPeers(peerEntities);
        });
    }

    public static boolean networkIsReachable() {
        Log.d(TAG, "networkIsReachable");
        return BRWalletManager.getInstance().isNetworkAvailable(NoirWallet.getContext());
    }

    public static void deleteBlocks() {
        Log.d(TAG, "deleteBlocks");
        final Context ctx = NoirWallet.getContext();
        if (ctx == null) return;
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(
                () -> MerkleBlockDataSource.getInstance(ctx).deleteAllBlocks());
    }

    public static void deletePeers() {
        Log.d(TAG, "deletePeers");
        final Context ctx = NoirWallet.getContext();
        if (ctx == null) return;
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(
                () -> PeerDataSource.getInstance(ctx).deleteAllPeers());
    }


    public void updateFixedPeer(Context ctx) {
        String node = BRSharedPrefs.getTrustNode(ctx);
        String host = TrustedNode.getNodeHost(node);
        int port = TrustedNode.getNodePort(node);
        boolean success = setFixedPeer(host, port);
        if (!success) {
            Log.e(TAG, "updateFixedPeer: Failed to updateFixedPeer with input: " + node);
        } else {
            Log.d(TAG, "updateFixedPeer: succeeded");
        }
        connect();
    }

    public void addStatusUpdateListener(OnTxStatusUpdate listener) {
        if (statusUpdateListeners == null) {
            return;
        }
        if (!statusUpdateListeners.contains(listener)) {
            statusUpdateListeners.add(listener);
        }
    }

    public void removeListener(OnTxStatusUpdate listener) {
        if (statusUpdateListeners == null) {
            return;
        }
        statusUpdateListeners.remove(listener);

    }

    public interface OnTxStatusUpdate {
        void onStatusUpdate();
    }

    public interface OnSyncSucceeded {
        void onFinished();
    }

    public static void updateLastBlockHeight(int blockHeight) {
        final Context ctx = NoirWallet.getContext();
        if (ctx == null) return;
        BRSharedPrefs.putLastBlockHeight(ctx, blockHeight);
    }

    public native String getCurrentPeerName();

    public native void create(int earliestKeyTime, int blockCount, int peerCount);

    public native void createNew(int earliestKeyTime, int blockCount, int peerCount, String hash, int height, long timestamp, int target);

    public native void connect();

    public native void putPeer(byte[] peerAddress, byte[] peerPort, byte[] peerTimeStamp);

    public native void createPeerArrayWithCount(int count);

    public native void putBlock(byte[] block, int blockHeight);

    public native void createBlockArrayWithCount(int count);

    public native static double syncProgress(int startHeight);

    public native static int getCurrentBlockHeight();

    public native static int getRelayCount(byte[] hash);

    public native boolean setFixedPeer(String node, int port);

    public native static int getEstimatedBlockHeight();

    public native boolean isCreated();

    //0 = disconnected, 1 = connecting, 2 = connected, -1 = _peerManager is null, -2 = something
    // went wrong.
    public static native int connectionStatus();

    public native void peerManagerFreeEverything();

    public native long getLastBlockTimestamp();

    public native void rescan();
}