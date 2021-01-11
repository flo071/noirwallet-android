package org.noirofficial.presenter.activities.models

import java.util.*

class AddressInfo {
    lateinit var address: String
    lateinit var utxos: Array<UTXO>
    var empty: Boolean = false

    companion object {
        fun empty(): AddressInfo {
            val addressInfo = AddressInfo()
            addressInfo.utxos = arrayOf()
            return addressInfo
        }
    }


    val assets: List<Asset>
        get() {
            val assets = LinkedList<Asset>()
            for (utxo in utxos) {
                if (utxo.used) {
                    continue
                }
                for (asset in utxo.assets) {
                    asset.address = utxo.address
                    asset.txid = utxo.txid
                    asset.index = utxo.index
                    assets.add(asset)
                }
            }
            return assets
        }

    fun getAssets(txid: String): LinkedList<Asset> {
        val assets = LinkedList<Asset>()
        for (utxo in utxos) {
            if (utxo.used or (utxo.txid.toLowerCase() != txid.toLowerCase())) {
                continue
            }
            for (asset in utxo.assets) {
                asset.address = utxo.address
                asset.txid = utxo.txid
                asset.index = utxo.index
                assets.add(asset)
            }
        }
        return assets
    }

    inner class UTXO {
        lateinit var address: String
        var index: Int = 0
        var used: Boolean = false
        var value: Long = 0
        lateinit var txid: String
        lateinit var assets: Array<Asset>
    }

    inner class Asset {
        lateinit var assetId: String
        var amount: Float = 0f
        lateinit var issueTxid: String
        var divisibility: Int = 0
        var lockStatus: Boolean = false
        lateinit var aggregationPolicy: String
        lateinit var address: String
        lateinit var txid: String
        var index: Int = 0
    }
}