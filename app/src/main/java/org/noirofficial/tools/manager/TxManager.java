package org.noirofficial.tools.manager;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;

import org.noirofficial.presenter.entities.TxItem;
import org.noirofficial.tools.threads.BRExecutor;
import org.noirofficial.wallet.BRWalletManager;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 7/19/17.
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
public class TxManager {
    public interface onStatusListener {
        void onTxManagerUpdate(TxItem[] aTransactionList);
    }

    private static final String TAG = TxManager.class.getName();
    private static TxManager instance;
    private Handler handler = new Handler(Looper.getMainLooper());

    private ArrayList<onStatusListener> theListeners;

    public void addListener(onStatusListener aListener) {
        theListeners.add(aListener);
    }

    public void removeListener(onStatusListener aListener) {
        theListeners.remove(aListener);
    }

    public static TxManager getInstance() {
        if (instance == null) {
            instance = new TxManager();
        }
        return instance;
    }

    private TxManager() {
        theListeners = new ArrayList<>();
    }

    public void updateTxList() {
        //This callback from the native layer gets hammered.
        //Post delay a callback and remove callbacks to prevent excessive invokation
        handler.removeCallbacks(updateTxRunnable);
        handler.postDelayed(updateTxRunnable, 1000);
    }

    private Runnable updateTxRunnable = new Runnable() {
        @Override
        public void run() {
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(() -> {
                final TxItem[] transactions = BRWalletManager.getInstance().getTransactions();
                handler.post(() -> {
                    for (onStatusListener listener : theListeners) {
                        listener.onTxManagerUpdate(transactions);
                    }
                });
            });
        }
    };
}