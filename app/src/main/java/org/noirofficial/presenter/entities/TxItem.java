package org.noirofficial.presenter.entities;


import android.os.Parcel;
import android.os.Parcelable;

import com.platform.entities.TxMetaData;
import com.platform.tools.KVStoreManager;

import java.util.Arrays;

import org.noirofficial.NoirWallet;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 1/13/16.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class TxItem implements Parcelable {
    public static final String TAG = TxItem.class.getName();
    private long timeStamp;
    private int blockHeight;
    private byte[] txHash;
    private long sent;
    private long received;
    private long fee;
    private String[] to;
    private String[] from;
    public String txReversed;
    private long balanceAfterTx;
    private long[] outAmounts;
    private boolean isValid;
    private int txSize;
    public TxMetaData metaData;
    public boolean isAsset;

    private TxItem() {
    }

    public TxItem(long timeStamp, int blockHeight, byte[] hash, String txReversed, long sent,
                  long received, long fee, String[] to, String[] from,
                  long balanceAfterTx, int txSize, long[] outAmounts, boolean isValid, boolean isAsset) {
        this.timeStamp = timeStamp;
        this.blockHeight = blockHeight;
        this.txReversed = txReversed;
        this.txHash = hash;
        this.sent = sent;
        this.received = received;
        this.fee = fee;
        this.to = to;
        this.from = from;
        this.balanceAfterTx = balanceAfterTx;
        this.outAmounts = outAmounts;
        this.isValid = isValid;
        this.txSize = txSize;
        this.metaData = KVStoreManager.getInstance().getTxMetaData(NoirWallet.getContext(), txHash);
        this.isAsset = isAsset;

    }

    public int getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(int blockHeight) {
        this.blockHeight = blockHeight;
    }

    public long getFee() {
        return fee;
    }

    public int getTxSize() {
        return txSize;
    }

    public String[] getFrom() {
        return from;
    }

    public byte[] getTxHash() {
        return txHash;
    }

    public String getTxHashHexReversed() {
        return txReversed;
    }

    public long getReceived() {
        return received;
    }

    public long getSent() {
        return sent;
    }

    public static String getTAG() {
        return TAG;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public String[] getTo() {
        return to;
    }

    public long getBalanceAfterTx() {
        return balanceAfterTx;
    }

    public long[] getOutAmounts() {
        return outAmounts;
    }

    public boolean isValid() {
        return isValid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TxItem txItem = (TxItem) o;
        return txReversed.equals(txItem.txReversed);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(txHash);
    }

    protected TxItem(Parcel in) {
        timeStamp = in.readLong();
        blockHeight = in.readInt();
        sent = in.readLong();
        received = in.readLong();
        fee = in.readLong();
        txReversed = in.readString();
        balanceAfterTx = in.readLong();
        isValid = in.readByte() != 0x00;
        txSize = in.readInt();
        metaData = (TxMetaData) in.readValue(TxMetaData.class.getClassLoader());
        isAsset = in.readInt() == 1;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(timeStamp);
        dest.writeInt(blockHeight);
        dest.writeLong(sent);
        dest.writeLong(received);
        dest.writeLong(fee);
        dest.writeString(txReversed);
        dest.writeLong(balanceAfterTx);
        dest.writeByte((byte) (isValid ? 0x01 : 0x00));
        dest.writeInt(txSize);
        dest.writeValue(metaData);
        dest.writeInt(isAsset ? 1 : 0);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<TxItem> CREATOR = new Parcelable.Creator<TxItem>() {
        @Override
        public TxItem createFromParcel(Parcel in) {
            return new TxItem(in);
        }

        @Override
        public TxItem[] newArray(int size) {
            return new TxItem[size];
        }
    };
}