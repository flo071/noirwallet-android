package org.noirofficial.presenter.activities.introactivities;

import android.content.Intent;
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuItem;

import org.noirofficial.R;
import org.noirofficial.databinding.ActivityWriteDownBinding;
import org.noirofficial.presenter.activities.callbacks.ActivityWriteDownCallback;
import org.noirofficial.presenter.activities.base.BRActivity;
import org.noirofficial.tools.animation.BRAnimator;
import org.noirofficial.tools.manager.BRSharedPrefs;
import org.noirofficial.tools.security.AuthManager;
import org.noirofficial.tools.security.PostAuth;

public class WriteDownActivity extends BRActivity {

    public static void open(AppCompatActivity activity) {
        Intent intent = new Intent(activity, WriteDownActivity.class);
        activity.startActivity(intent);
    }

    private ActivityWriteDownCallback callback = () -> AuthManager.getInstance().authPrompt(
            WriteDownActivity.this, null,
            getString(R.string.VerifyPin_continueBody), new AuthType(AuthType.Type.POST_AUTH));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityWriteDownBinding binding = DataBindingUtil.setContentView(this,
                R.layout.activity_write_down);
        binding.setCallback(callback);
        setupToolbar();
        setToolbarTitle(R.string.SecurityCenter_paperKeyTitle);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.home:
            case android.R.id.home:
                BRAnimator.startBreadActivity(WriteDownActivity.this,
                        BRSharedPrefs.digiIDFocus(this));
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        BRAnimator.startBreadActivity(WriteDownActivity.this, BRSharedPrefs.digiIDFocus(this));
    }

    @Override
    public void onComplete(AuthType authType) {
        switch(authType.type) {
            case LOGIN:
                break;
            case DIGI_ID:
                break;
            case POST_AUTH:
                PostAuth.instance.onPhraseCheckAuth(WriteDownActivity.this,false);
                break;
            default:
                super.onComplete(authType);
        }
    }

    @Override
    public void onCancel(AuthType authType) {

    }
}