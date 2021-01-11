package org.noirofficial.presenter.fragments.models;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.core.content.ContextCompat;
import android.text.TextUtils;

import java.math.BigDecimal;

import org.noirofficial.BR;
import org.noirofficial.NoirWallet;
import org.noirofficial.R;
import org.noirofficial.presenter.activities.utils.ActivityUtils;
import org.noirofficial.tools.manager.BRSharedPrefs;
import org.noirofficial.tools.util.BRCurrency;
import org.noirofficial.tools.util.BRExchange;
import org.noirofficial.wallet.BRWalletManager;

public class SendFragmentModel extends BaseObservable implements Parcelable {

    private StringBuilder amountBuilder = new StringBuilder("");
    private String selectedIso = BRSharedPrefs.getPreferredBTC(NoirWallet.getContext()) ? "DGB"
            : BRSharedPrefs.getIso(NoirWallet.getContext());
    private String enteredAddress = "";
    private String memo = "";
    private boolean showSendWaiting = false;
    private boolean dandelionChecked = true;

    public SendFragmentModel(){}

    @Bindable
    public int getBRKeyboardColor() {
        return R.color.keyboard_text_color;
    }

    @Bindable
    public String getIsoText() {
        return BRCurrency.getSymbolByIso(NoirWallet.getContext(), getSelectedIso());
    }

    @Bindable
    public String getIsoButtonText() {
        return String.format("%s(%s)",
                BRCurrency.getCurrencyName(NoirWallet.getContext(), getSelectedIso()),
                BRCurrency.getSymbolByIso(NoirWallet.getContext(), getSelectedIso()));
    }

    @Bindable
    public String getAddress() {
        return enteredAddress;
    }

    public void setAddress(String address) {
        this.enteredAddress = address;
        notifyPropertyChanged(BR.address);
        notifyPropertyChanged(BR.feeText);
    }

    @Bindable
    public int getBalanceTextColor() {
        if (validAmount() && new BigDecimal(getAmount()).doubleValue()
                > getBalanceForISO().doubleValue()) {
            return ContextCompat.getColor(NoirWallet.getContext(), R.color.warning_color);
        } else {
            return ContextCompat.getColor(NoirWallet.getContext(),R.color.white);
        }
    }

    @Bindable
    public int getFeeTextColor() {
        if (validAmount() && new BigDecimal(getAmount()).doubleValue()
                > getBalanceForISO().doubleValue()) {
            return ContextCompat.getColor(NoirWallet.getContext(),R.color.warning_color);
        } else {
            return ContextCompat.getColor(NoirWallet.getContext(),R.color.white);
        }
    }

    @Bindable
    public int getAmountEditTextColor() {
        if (validAmount() && new BigDecimal(getAmount()).doubleValue()
                > getBalanceForISO().doubleValue()) {
            return ContextCompat.getColor(NoirWallet.getContext(),R.color.warning_color);
        } else {
            return ContextCompat.getColor(NoirWallet.getContext(),R.color.white);
        }
    }

    @Bindable
    public int getISOTextColor() {
        if (validAmount() && new BigDecimal(getAmount()).doubleValue()
                > getBalanceForISO().doubleValue()) {
            return ContextCompat.getColor(NoirWallet.getContext(),R.color.warning_color);
        } else {
            return ContextCompat.getColor(NoirWallet.getContext(),R.color.white);
        }
    }

    @Bindable
    public String getBalanceText() {
        return String.format("%s", getFormattedBalance());
    }

    @Bindable
    public String getFeeText() {
        String approximateFee = getApproximateFee();
        if (TextUtils.isEmpty(approximateFee)) {
            approximateFee = "";
        }
        return NoirWallet.getContext().getString(R.string.Send_fee).replace("%1$s", approximateFee);
    }

    public String getAmount() {
        return amountBuilder.toString().replace("'", "");
    }

    @Bindable
    public String getDisplayAmount() {
        return amountBuilder.toString().replace(".",
                Character.toString(ActivityUtils.getDecimalSeparator())).replace("'", "");
    }

    @Bindable
    public String getMemo() {
        return memo;
    }

    @Bindable
    public int getShowSendWaiting() {
        return showSendWaiting ? 1 : 0;
    }

    @Bindable
    public boolean getMaxSendVisibility() {
        return BRSharedPrefs.getGenericSettingsSwitch(NoirWallet.getContext(), "max_send_enabled");
    }

    @Bindable
    public boolean getDandelionChecked() {
        return dandelionChecked;
    }

    public void setDandelionChecked(boolean dandelionChecked) {
        this.dandelionChecked = dandelionChecked;
        notifyPropertyChanged(BR.dandelionText);
    }

    @Bindable
    public String getDandelionText() {
        return dandelionChecked ? NoirWallet.getContext().getString(R.string.dandelion_on) :
                NoirWallet.getContext().getString(R.string.dandelion_off);
    }

    public void showSendWaiting(boolean show) {
        showSendWaiting = show;
        notifyPropertyChanged(BR.showSendWaiting);
    }

    public void setMemo(String memo) {
        this.memo = memo;
        notifyPropertyChanged(BR.memo);
    }

    public void setSelectedIso(String iso) {
        selectedIso = iso;
        updateText();
    }

    public String getSelectedIso() {
        return selectedIso;
    }

    private boolean validAmount() {
        try {

            new BigDecimal(getAmount());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void appendAmount(Integer append) {
        amountBuilder.append(append);
        notifyPropertyChanged(BR.amountEditTextColor);
    }

    public void appendAmount(String append) {
        amountBuilder.append(append);
        notifyPropertyChanged(BR.amountEditTextColor);
    }

    public void setAmount(String amount) {
        amountBuilder = new StringBuilder(amount);
        notifyPropertyChanged(BR.amountEditTextColor);
    }

    public void handleDeleteClick() {
        if (TextUtils.isEmpty(getAmount())) {
            return;
        }
        amountBuilder.deleteCharAt(getAmount().length() - 1);
    }

    private long getFee() {
        if (getSatoshis() == 0 || TextUtils.isEmpty(getAddress())) {
            return 0;
        }
        if (TextUtils.isEmpty(getAddress()) || !BRWalletManager.validateAddress(getAddress())) {
            return BRWalletManager.getInstance().feeForTransactionAmount(getSatoshis());
        } else {
            return BRWalletManager.getInstance().feeForTransaction(getAddress(), getSatoshis());
        }
    }

    private BigDecimal getFeeForISO() {
        return BRExchange.getAmountFromSatoshis(NoirWallet.getContext(), getSelectedIso(),
                new BigDecimal(getFee()));
    }

    private String getApproximateFee() {
        return BRCurrency.getFormattedCurrencyString(NoirWallet.getContext(), getSelectedIso(),
                getFeeForISO());
    }

    private long getSatoshis() {
        if (!validAmount()) {
            return 0;
        }
        long satoshis = selectedIso.equalsIgnoreCase("dgb") ? BRExchange.getSatoshisForBitcoin(
                NoirWallet.getContext(), new BigDecimal(getAmount())).longValue()
                : BRExchange.getSatoshisFromAmount(NoirWallet.getContext(), getSelectedIso(),
                        new BigDecimal(getAmount())).longValue();
        return satoshis;
    }

    public void populateMaxAmount() {
        BigDecimal maxAvailable = getBalanceForISO().multiply(new BigDecimal(100000000));
        if (maxAvailable.equals(BigDecimal.ZERO)) {
            return;
        }
        BigDecimal fee = new BigDecimal(BRWalletManager.getInstance().feeForTransactionAmount(maxAvailable.intValue()));
        BigDecimal availableConsideringFee = maxAvailable.subtract(fee).divide(
                new BigDecimal(100000000));
        setAmount(availableConsideringFee.toString());
        notifyPropertyChanged(BR.displayAmount);
        notifyPropertyChanged(BR.feeText);
    }

    private BigDecimal getBalanceForISO() {
        return BRExchange.getAmountFromSatoshis(NoirWallet.getContext(), getSelectedIso(),
                new BigDecimal(BRWalletManager.getInstance().getBalance(NoirWallet.getContext())));
    }

    private String getFormattedBalance() {
        return BRCurrency.getFormattedCurrencyString(NoirWallet.getContext(), getSelectedIso(),
                getBalanceForISO());
    }

    public void updateText() {
        notifyPropertyChanged(BR.isoButtonText);
        notifyPropertyChanged(BR.displayAmount);
        notifyPropertyChanged(BR.isoText);
        notifyPropertyChanged(BR.balanceText);
        notifyPropertyChanged(BR.feeText);
        notifyPropertyChanged(BR.balanceTextColor);
        notifyPropertyChanged(BR.feeTextColor);
        notifyPropertyChanged(BR.amountEditTextColor);
        notifyPropertyChanged(BR.iSOTextColor);
    }

    protected SendFragmentModel(Parcel in) {
        amountBuilder = new StringBuilder(in.readString());
        selectedIso = in.readString();
        enteredAddress = in.readString();
        memo = in.readString();
        showSendWaiting = in.readByte() != 0x00;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(amountBuilder.toString());
        dest.writeString(selectedIso);
        dest.writeString(enteredAddress);
        dest.writeString(memo);
        dest.writeByte((byte) (showSendWaiting ? 0x01 : 0x00));
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<SendFragmentModel> CREATOR = new Parcelable.Creator<SendFragmentModel>() {
        @Override
        public SendFragmentModel createFromParcel(Parcel in) {
            return new SendFragmentModel(in);
        }

        @Override
        public SendFragmentModel[] newArray(int size) {
            return new SendFragmentModel[size];
        }
    };
}