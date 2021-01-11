package org.noirofficial.tools.crypto

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.noirofficial.R
import org.noirofficial.presenter.activities.callbacks.BRAuthCompletion
import org.noirofficial.presenter.activities.models.AssetModel
import org.noirofficial.presenter.activities.models.FinanceUTXO
import org.noirofficial.presenter.activities.models.SendAsset
import org.noirofficial.presenter.activities.utils.RetrofitManager
import org.noirofficial.presenter.fragments.FragmentNumberPicker
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class AssetsHelper {

    private external fun getNeededUTXOTxid(amount: Int): FinanceUTXO
    @kotlin.jvm.JvmField
    var pendingAssetTx: AssetTx? = null

    companion object {
        val instance: AssetsHelper = AssetsHelper()
    }

    data class AssetTx(var changeAddress: String, var destinationAddress: CharSequence, val utxoTxids: Array<String>, val assetQuantity: Int, val assetId: String, val divisibility: Int, val assetModel: AssetModel)

    fun sendPendingAssetTx(context: Context) {
        pendingAssetTx?.let {
            processAssetTx(context, it)
            pendingAssetTx = null
        }
    }

    @SuppressLint("CheckResult")
    fun processAssetTx(context: Context, assetTx: AssetTx) {
        try {
            Log.d(AssetModel::class.java.simpleName, "Clipped Address: ${assetTx.destinationAddress}")

            Observable.fromCallable {
                val fee = 360 * assetTx.utxoTxids.size + 214
                val financeUTXO = getNeededUTXOTxid(fee)
                if (financeUTXO.txid.isNullOrEmpty()) {
                    Toast.makeText(context, R.string.not_enough_digi, Toast.LENGTH_SHORT).show()
                    throw IllegalStateException()
                }
                RetrofitManager.instance.clearCache(assetTx.changeAddress)
                RetrofitManager.instance.clearCache(assetTx.destinationAddress.toString())
                SendAsset(
                        Integer.toString(fee),
                        assetTx.changeAddress,
                        assetTx.utxoTxids,
                        financeUTXO.vout,
                        financeUTXO.txid,
                        assetTx.destinationAddress.toString(),
                        assetTx.assetQuantity,
                        assetTx.assetId,
                        assetTx.divisibility,
                        assetTx.assetModel
                )
            }.subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread()).subscribe { sendAsset ->
                FragmentNumberPicker.show(
                        context as AppCompatActivity,
                        BRAuthCompletion.AuthType(sendAsset)
                )
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }
}