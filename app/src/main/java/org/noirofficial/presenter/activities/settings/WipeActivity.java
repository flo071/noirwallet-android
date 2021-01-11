package org.noirofficial.presenter.activities.settings;

import android.content.Intent;
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import org.noirofficial.R;
import org.noirofficial.databinding.ActivityRestoreBinding;
import org.noirofficial.presenter.activities.InputWordsActivity;
import org.noirofficial.presenter.activities.callbacks.ActivityWipeCallback;
import org.noirofficial.presenter.activities.base.BRActivity;


public class WipeActivity extends BRActivity {

    private ActivityWipeCallback callback = () -> InputWordsActivity.open(WipeActivity.this,
            InputWordsActivity.Type.WIPE);

    public static void show(AppCompatActivity activity) {
        Intent intent = new Intent(activity, WipeActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityRestoreBinding binding = DataBindingUtil.setContentView(this,
                R.layout.activity_restore);
        binding.setCallback(callback);
        setupToolbar();
        setToolbarTitle(R.string.Settings_wipe);
    }
}