package org.noirofficial.presenter.fragments.models;

import android.annotation.SuppressLint;
import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import android.text.TextUtils;

import com.platform.entities.TxMetaData;
import com.platform.tools.KVStoreManager;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import org.noirofficial.NoirWallet;
import org.noirofficial.R;
import org.noirofficial.presenter.entities.TxItem;
import org.noirofficial.tools.database.Database;
import org.noirofficial.tools.database.DigiTransaction;
import org.noirofficial.tools.manager.BRSharedPrefs;
import org.noirofficial.tools.manager.TxManager;
import org.noirofficial.tools.threads.BRExecutor;
import org.noirofficial.tools.util.BRCurrency;
import org.noirofficial.tools.util.BRExchange;

public class TransactionDetailsViewModel extends BaseObservable {

    private TxItem item;

    public TransactionDetailsViewModel(TxItem item) {
        this.item = item;
    }

    @Bindable
    public String getCryptoAmount() {
        if (!BRSharedPrefs.getPreferredBTC(NoirWallet.getContext()) && currentFiatAmountEqualsOriginalFiatAmount()) {
            return getRawFiatAmount(item);
        } else {
            boolean received = item.getSent() == 0;
            long satoshisAmount = received ? item.getReceived() : (item.getSent() - item.getReceived());
            return BRCurrency.getFormattedCurrencyString(NoirWallet.getContext(), "NOR",
                    BRExchange.getAmountFromSatoshis(NoirWallet.getContext(), "NOR",
                            new BigDecimal(satoshisAmount)));
        }
    }

    public static String getRawFiatAmount(TxItem txItem) {
        boolean received = txItem.getSent() == 0;
        long satoshisAmount = received ? txItem.getReceived() : (txItem.getSent() - txItem.getReceived());
        return BRCurrency.getFormattedCurrencyString(NoirWallet.getContext(),
                BRSharedPrefs.getIso(NoirWallet.getContext()),
                BRExchange.getAmountFromSatoshis(NoirWallet.getContext(),
                        BRSharedPrefs.getIso(NoirWallet.getContext()),
                        new BigDecimal(satoshisAmount)));
    }

    @SuppressLint("StringFormatInvalid")
    @Bindable
    public String getFiatAmount() {
        return String.format(NoirWallet.getContext().getString(R.string.current_amount), getRawFiatAmount(item));
    }

    @SuppressLint("StringFormatInvalid")
    public String getOriginalFiatAmount() {
        DigiTransaction transaction = Database.instance.findTransaction(item.getTxHash());
        if (transaction == null) {
            return String.format(NoirWallet.getContext().getString(R.string.original_amount), "");
        } else {
            return String.format(NoirWallet.getContext().getString(R.string.original_amount),
                    transaction.getTxAmount());
        }
    }

    public boolean currentFiatAmountEqualsOriginalFiatAmount() {
        DigiTransaction transaction = Database.instance.findTransaction(item.getTxHash());
        if (transaction == null) {
            return true;
        }
        boolean received = item.getSent() == 0;
        long satoshisAmount = received ? item.getReceived() : (item.getSent() - item.getReceived());
        return transaction.getTxAmount().equals(BRCurrency.getFormattedCurrencyString(NoirWallet.getContext(),
                BRSharedPrefs.getIso(NoirWallet.getContext()),
                BRExchange.getAmountFromSatoshis(NoirWallet.getContext(),
                        BRSharedPrefs.getIso(NoirWallet.getContext()),
                        new BigDecimal(satoshisAmount))));
    }

    @Bindable
    public String getToFrom() {
        return item.getReceived() - item.getSent() < 0 ? NoirWallet.getContext().getString(
                R.string.sent) : NoirWallet.getContext().getString(R.string.received);
    }

    @Bindable
    public boolean getSent() {
        return item.getReceived() - item.getSent() < 0;
    }

    @Bindable
    public String getAddress() {
        try {
            return getSent() ? item.getTo()[0] : item.getFrom()[0];
        } catch(NullPointerException e) {
            return "";
            //Why isn't the address available?
        }
    }

    @Bindable
    public int getSentReceivedIcon() {
        if (getSent()) {
            return R.drawable.transaction_details_sent;
        } else {
            return R.drawable.transaction_details_received;
        }
    }

    @Bindable
    public String getDate() {
        return getFormattedDate(item.getTimeStamp());
    }

    @Bindable
    public String getTime() {
        return getFormattedTime(item.getTimeStamp());
    }

    @Bindable
    public boolean getCompleted() {
        return BRSharedPrefs.getLastBlockHeight(NoirWallet.getContext()) - item.getBlockHeight() + 1
                >= 6;
    }

    @Bindable
    public String getFee() {
        if (item.getSent() == 0) {
            return "";
        }
        String approximateFee = BRCurrency.getFormattedCurrencyString(NoirWallet.getContext(), "NOR",
                BRExchange.getAmountFromSatoshis(NoirWallet.getContext(), "NOR",
                        new BigDecimal(item.getFee())));
        if (TextUtils.isEmpty(approximateFee)) {
            approximateFee = "";
        }
        return NoirWallet.getContext().getString(R.string.Send_fee).replace("%1$s", approximateFee);
    }

    @Bindable
    public String getMemo() {
        return item.metaData != null ? item.metaData.comment : "";
    }

    public void setMemo(String memo) {
        TxMetaData metaData = item.metaData;
        if (metaData == null) {
            item.metaData = new TxMetaData();
        }
        item.metaData.comment = memo;
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(() -> {
            KVStoreManager.getInstance().putTxMetaData(NoirWallet.getContext(), item.metaData, item.getTxHash());
            TxManager.getInstance().updateTxList();
        });
    }

    private String getFormattedDate(long timeStamp) {
        Date currentLocalTime = new Date(
                timeStamp == 0 ? System.currentTimeMillis() : timeStamp * 1000);
        Locale current = NoirWallet.getContext().getResources().getConfiguration().locale;
        return DateFormat.getDateInstance(DateFormat.SHORT, current).format(currentLocalTime);
    }

    private String getFormattedTime(long timeStamp) {
        Date currentLocalTime = new Date(
                timeStamp == 0 ? System.currentTimeMillis() : timeStamp * 1000);
        Locale current = NoirWallet.getContext().getResources().getConfiguration().locale;
        return DateFormat.getTimeInstance(DateFormat.MEDIUM, current).format(currentLocalTime);
    }

    @Bindable
    public String getTransactionID() {
        return item.txReversed;
    }
}
