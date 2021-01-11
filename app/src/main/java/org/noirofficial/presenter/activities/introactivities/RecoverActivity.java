package org.noirofficial.presenter.activities.introactivities;

import androidx.databinding.DataBindingUtil;
import android.os.Bundle;

import org.noirofficial.R;
import org.noirofficial.databinding.ActivityIntroRecoverBinding;
import org.noirofficial.presenter.activities.InputWordsActivity;
import org.noirofficial.presenter.activities.callbacks.ActivityRecoverCallback;
import org.noirofficial.presenter.activities.base.BRActivity;

public class RecoverActivity extends BRActivity {

    ActivityRecoverCallback callback = () ->
            InputWordsActivity.open(RecoverActivity.this, InputWordsActivity.Type.RESTORE);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityIntroRecoverBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_intro_recover);
        binding.setCallback(callback);
        setupToolbar();
        setToolbarTitle(R.string.RecoverWallet_header);
    }
}