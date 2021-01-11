package org.noirofficial.presenter.activities.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.noirofficial.presenter.entities.TxItem;
import org.noirofficial.tools.list.items.ListItemTransactionData;

public class TransactionUtils {
    public static ArrayList<ListItemTransactionData> convertNewTransactionsForAdapter(Adapter adapter,
                                                                                      ArrayList<ListItemTransactionData> transactions) {
        ArrayList<ListItemTransactionData> transactionList = new ArrayList<>();
        for (int index = 0; index < transactions.size(); index++) {
            ListItemTransactionData transactionData = transactions.get(index);
            TxItem item = transactions.get(index).transactionItem;
            if (adapter == Adapter.RECEIVED && item.getSent() == 0) {
                transactionList.add(transactionData);
            } else if (adapter == Adapter.SENT && item.getSent() > 0) {
                transactionList.add(transactionData);
            }
        }
        return transactionList;
    }

    public static ArrayList<ListItemTransactionData> getNewTransactionsData(TxItem[] newTxItems) {
        ArrayList<ListItemTransactionData> newTransactionsData = new ArrayList<>();
        ArrayList<TxItem> newTransactions = new ArrayList<>(Arrays.asList(newTxItems));
        Collections.sort(newTransactions,
                (t1, t2) -> Long.compare(t1.getTimeStamp(), t2.getTimeStamp()));
        for (TxItem tx : newTransactions) {
            newTransactionsData.add(new ListItemTransactionData(newTransactions.indexOf(tx),
                    newTransactions.size(), tx));
        }
        return newTransactionsData;
    }

    public enum Adapter {
        SENT, RECEIVED
    }
}
