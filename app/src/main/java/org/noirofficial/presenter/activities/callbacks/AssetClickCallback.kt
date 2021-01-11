package org.noirofficial.presenter.activities.callbacks

import android.view.View
import org.noirofficial.presenter.activities.models.AssetModel
import org.noirofficial.presenter.adapter.MultiTypeDataBoundAdapter

interface AssetClickCallback : MultiTypeDataBoundAdapter.ActionCallback {
    fun onAssetClick(v: View, assetModel: AssetModel)
}