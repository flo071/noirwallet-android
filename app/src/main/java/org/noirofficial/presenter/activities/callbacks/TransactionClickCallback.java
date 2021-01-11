package org.noirofficial.presenter.activities.callbacks;

import org.noirofficial.presenter.adapter.MultiTypeDataBoundAdapter;
import org.noirofficial.tools.list.items.ListItemTransactionData;

public interface TransactionClickCallback extends MultiTypeDataBoundAdapter.ActionCallback {

    void onTransactionClick(ListItemTransactionData listItemTransactionData);
}
