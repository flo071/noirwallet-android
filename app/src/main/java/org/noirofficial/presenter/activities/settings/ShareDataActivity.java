package org.noirofficial.presenter.activities.settings;

import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import org.noirofficial.R;
import org.noirofficial.presenter.activities.base.BRActivity;
import org.noirofficial.tools.manager.BRSharedPrefs;

public class ShareDataActivity extends BRActivity {
    private static final String TAG = ShareDataActivity.class.getName();
    private ToggleButton toggleButton;
    public static boolean appVisible = false;
    private static ShareDataActivity app;

    public static ShareDataActivity getApp() {
        return app;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
        toggleButton.setChecked(BRSharedPrefs.getShareData(this));
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                BRSharedPrefs.putShareData(ShareDataActivity.this, isChecked);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        appVisible = true;
        app = this;
    }

    @Override
    protected void onPause() {
        super.onPause();
        appVisible = false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

}
