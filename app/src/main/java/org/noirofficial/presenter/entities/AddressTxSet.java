package org.noirofficial.presenter.entities;

import javax.annotation.Nullable;

import org.noirofficial.tools.list.items.ListItemTransactionData;

public class AddressTxSet {
    public String address;
    public ListItemTransactionData listItemTransactionData;

    public AddressTxSet(String address, ListItemTransactionData listItemTransactionData) {
        this.address = address;
        this.listItemTransactionData = listItemTransactionData;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        AddressTxSet addressTxSet = (AddressTxSet) obj;
        return address.equals(addressTxSet.address);
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }
}