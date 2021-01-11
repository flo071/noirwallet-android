package org.noirofficial.presenter.activities.introactivities;

import android.os.Bundle;
import androidx.annotation.Nullable;

import org.noirofficial.presenter.activities.base.BRActivity;
import org.noirofficial.tools.animation.BRAnimator;
import org.noirofficial.wallet.BRWalletManager;

public class LauncherActivity extends BRActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
        BRWalletManager wallet = BRWalletManager.getInstance();
        if (wallet.noWallet(this)) {
            IntroActivity.open(this);
        } else {
            BRAnimator.startBreadActivity(this, true);
        }
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }
}
