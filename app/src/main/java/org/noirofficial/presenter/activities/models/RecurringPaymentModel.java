package org.noirofficial.presenter.activities.models;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

public class RecurringPaymentModel extends BaseObservable {

    public String recipientAddress;
    public String amount;
    public String label;

    public RecurringPaymentModel() {
    }

    RecurringPaymentModel(String recipientAddress, String amount, String label) {
        this.recipientAddress = recipientAddress;
        this.amount = amount;
        this.label = label;
    }

    @Bindable
    public String getRecipientAddress() {
        return recipientAddress;
    }

    public void setRecipientAddress(String recipientAddress) {
        this.recipientAddress = recipientAddress;
    }

    @Bindable
    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }
}
