package org.noirofficial.tools.adapter

import java.util.ArrayList
import java.util.Date

import org.noirofficial.NoirWallet
import org.noirofficial.R
import org.noirofficial.presenter.activities.BreadActivity
import org.noirofficial.presenter.activities.callbacks.TransactionClickCallback
import org.noirofficial.presenter.adapter.MultiTypeDataBoundAdapter
import org.noirofficial.presenter.entities.TxItem
import org.noirofficial.presenter.fragments.models.TransactionDetailsViewModel
import org.noirofficial.tools.animation.BRAnimator
import org.noirofficial.tools.database.Database
import org.noirofficial.tools.list.items.ListItemTransactionData
import org.noirofficial.tools.manager.BRSharedPrefs
import org.noirofficial.wallet.BRWalletManager


/**
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

class TransactionListAdapter(
        private val activity: BreadActivity
) : MultiTypeDataBoundAdapter(null, null as Any?), TransactionClickCallback {

    val transactions = ArrayList<ListItemTransactionData>()

    init {
        setActionCallback(this)
    }

    fun updateTransactions(transactions: ArrayList<ListItemTransactionData>) {
        for (listItemTransactionData in this.transactions) {
            val findTransaction = transactions.filter { i -> i == listItemTransactionData }.firstOrNull()
            findTransaction?.let { updatedTransaction ->
                listItemTransactionData.update(updatedTransaction)
                val confirms = BRSharedPrefs.getLastBlockHeight(NoirWallet.getContext()) -
                        listItemTransactionData.getTransactionItem().blockHeight + 1
                if (confirms <= 8) {
                    BRWalletManager.getInstance().refreshBalance(NoirWallet.getContext())
                }
            }
        }
    }

    fun addTransactions(transactions: ArrayList<ListItemTransactionData>) {
        transactions.sortWith(Comparator { o1, o2 ->
            Date(o1.transactionItem.timeStamp).compareTo(
                    Date(o2.transactionItem.timeStamp))
        })
        for (transaction in transactions) {
            saveFiatValue(transaction.transactionItem)
            this.transactions.add(0, transaction)
            addItem(0, transaction)
        }
    }

    override fun getItemCount(): Int {
        return transactions.size
    }

    override fun getItemId(position: Int): Long {
        return Integer.valueOf(transactions[position].getTransactionItem().hashCode()).toLong()
    }

    private fun saveFiatValue(txItem: TxItem) {
        //Save the transaction text in fiat mode, the first time the transaction is displayed in the app
        if (!Database.instance.containsTransaction(txItem.txHash)) {
            Database.instance.saveTransaction(txItem.txHash,
                    TransactionDetailsViewModel.getRawFiatAmount(txItem))
        }
    }

    override fun onTransactionClick(listItemTransactionData: ListItemTransactionData) {
        /*if (listItemTransactionData.transactionItem.isAsset) {
            activity.onAssetsButtonClick(null)
        } else {*/
            val adapterPosition = transactions.indexOf(listItemTransactionData)
            BRAnimator.showTransactionPager(activity, transactions, adapterPosition)
        /*}*/
    }
}