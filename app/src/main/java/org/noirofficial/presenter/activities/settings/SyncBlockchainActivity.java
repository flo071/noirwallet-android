package org.noirofficial.presenter.activities.settings;

import androidx.databinding.DataBindingUtil;
import android.os.Bundle;

import org.noirofficial.R;
import org.noirofficial.databinding.ActivitySyncBlockchainBinding;
import org.noirofficial.presenter.activities.callbacks.ActivitySyncCallback;
import org.noirofficial.presenter.activities.base.BRActivity;
import org.noirofficial.tools.animation.BRAnimator;
import org.noirofficial.tools.animation.BRDialog;
import org.noirofficial.wallet.BRWalletManager;


public class SyncBlockchainActivity extends BRActivity {

    private ActivitySyncCallback callback = () -> BRDialog.showCustomDialog(SyncBlockchainActivity.this,
            getString(R.string.ReScan_alertTitle),
            getString(R.string.ReScan_footer), getString(R.string.ReScan_alertAction),
            getString(R.string.Button_cancel),
            brDialogView -> {
                brDialogView.dismissWithAnimation();
                BRWalletManager.getInstance().wipeBlockAndTrans(SyncBlockchainActivity.this,
                        () -> BRAnimator.startBreadActivity(SyncBlockchainActivity.this,
                                false));
            }, brDialogView -> brDialogView.dismissWithAnimation(), null, 0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivitySyncBlockchainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_sync_blockchain);
        setupToolbar();
        setToolbarTitle(R.string.Settings_sync);
        binding.setCallback(callback);
    }
}