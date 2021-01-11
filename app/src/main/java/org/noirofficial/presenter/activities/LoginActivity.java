package org.noirofficial.presenter.activities;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import androidx.databinding.DataBindingUtil;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;

import android.os.Vibrator;
import android.util.Log;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.RotateAnimation;

import com.platform.tools.BRBitId;

import org.noirofficial.R;
import org.noirofficial.databinding.ActivityPinBinding;
import org.noirofficial.presenter.activities.callbacks.LoginActivityCallback;
import org.noirofficial.presenter.activities.models.PinActivityModel;
import org.noirofficial.presenter.activities.utils.ActivityUtils;
import org.noirofficial.presenter.activities.base.BRActivity;
import org.noirofficial.presenter.fragments.FragmentFingerprint;
import org.noirofficial.tools.animation.BRAnimator;
import org.noirofficial.tools.animation.SpringAnimator;
import org.noirofficial.tools.security.AuthManager;
import org.noirofficial.tools.security.BitcoinUrlHandler;
import org.noirofficial.tools.threads.BRExecutor;
import org.noirofficial.wallet.BRWalletManager;

public class LoginActivity extends BRActivity implements BRWalletManager.OnBalanceChanged {
    private static final String TAG = LoginActivity.class.getName();
    ActivityPinBinding binding;
    private StringBuilder pin = new StringBuilder();
    private boolean inputAllowed = true;
    private Handler handler = new Handler(Looper.getMainLooper());
    private NfcAdapter nfcAdapter;
    private PendingIntent pendingNfcIntent;
    private boolean paused;
    private Vibrator vibrator;


    private LoginActivityCallback callback = () -> {
        if (!paused) {
            BRAnimator.openScanner(this);
        }
    };

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_pin);
        binding.setData(new PinActivityModel());
        binding.setCallback(callback);
        binding.brkeyboard.addOnInsertListener(this::handleClick);
        binding.brkeyboard.setShowDot(false);
        binding.brkeyboard.setDeleteImage(R.drawable.ic_delete_white);
        if (!processDeepLink(getIntent()) &&
                AuthManager.isFingerPrintAvailableAndSetup(this)) {
            showFingerprintDialog();
        }
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter != null) {
            pendingNfcIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                    getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        }
        binding.touchImage.post(() -> {
            final RotateAnimation rotateAnim = new RotateAnimation(0.0f, 1080.0f,
                    RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                    RotateAnimation.RELATIVE_TO_SELF, 0.5f);
            rotateAnim.setDuration(1000);
            rotateAnim.setStartOffset(500);
            rotateAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            rotateAnim.setFillAfter(true);
            binding.touchImage.setAnimation(rotateAnim);
            rotateAnim.start();
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        processDeepLink(intent);
        processNFC(intent);
        setIntent(new Intent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        paused = false;
        updateDots();
        inputAllowed = true;
        ActivityUtils.enableNFC(this);
        BRWalletManager.getInstance().init();
        BRWalletManager.getInstance().addBalanceChangedListener(this);
        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(this, pendingNfcIntent, null, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
        ActivityUtils.disableNFC(this);
        BRWalletManager.getInstance().removeListener(this);
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    private boolean processDeepLink(@Nullable final Intent intent) {
        if (intent == null) {
            return false;
        }
        Uri data = intent.getData();
        if (data != null && BRBitId.isBitId(data.toString())) {
            BRBitId.digiIDAuthPrompt(this, data.toString(), true);
            return true;
        } else if (data != null && BitcoinUrlHandler.isBitcoinUrl(data.toString())) {
            BRAnimator.showOrUpdateSendFragment(this, data.toString());
            return true;
        }
        return false;
    }

    private void processNFC(@Nullable final Intent intent) {
        if (intent == null) {
            return;
        }
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                return;
            }
            NdefMessage ndefMessage = ndef.getCachedNdefMessage();
            if (ndefMessage == null) {
                return;
            }
            NdefRecord[] records = ndefMessage.getRecords();
            for (NdefRecord ndefRecord : records) {
                try {
                    String record = new String(ndefRecord.getPayload());
                    if (record.contains("digiid")) {
                        Log.d(LoginActivity.class.getSimpleName(),
                                record.substring(record.indexOf("digiid")));
                        BRBitId.digiIDAuthPrompt(this, record.substring(record.indexOf("digiid")), false);
                    } else if (record.contains("digibyte")) {
                        BRAnimator.showOrUpdateSendFragment(this,
                                record.substring(record.indexOf("digibyte")));
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handleClick(String key) {
        if (vibrator != null && vibrator.hasVibrator()) {
            try {
                //Fatal Exception: java.lang.NullPointerException
                //Attempt to read from field 'android.os.VibrationEffect com.android.server.VibratorService$Vibration.mEffect' on a null object reference
                vibrator.vibrate(150);
            } catch (NullPointerException e) {
                //Platform malfunction
            }
        }
        if (!inputAllowed) {
            Log.e(TAG, "handleClick: input not allowed");
            return;
        }
        if (key == null) {
            Log.e(TAG, "handleClick: key is null! ");
            return;
        }

        if (key.isEmpty()) {
            handleDeleteClick();
        } else if (Character.isDigit(key.charAt(0))) {
            handleDigitClick(Integer.parseInt(key.substring(0, 1)));
        } else {
            Log.e(TAG, "handleClick: oops: " + key);
        }
    }

    private void showFingerprintDialog() {
        if (getSupportFragmentManager().findFragmentByTag(FragmentFingerprint.class.getName())
                != null) {
            return;
        }
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            AuthManager.getInstance().authPrompt(LoginActivity.this, "", "", new AuthType(
                    AuthType.Type.LOGIN));
        }, 500);
    }


    private void handleDigitClick(Integer dig) {
        if (pin.length() < 6) {
            pin.append(dig);
        }
        updateDots();
    }

    private void handleDeleteClick() {
        if (pin.length() > 0) {
            pin.deleteCharAt(pin.length() - 1);
        }
        updateDots();
    }

    private void unlockWallet() {
        BRAnimator.startBreadActivity(this, false);
    }

    private void showFailedToUnlock() {
        SpringAnimator.failShakeAnimation(LoginActivity.this, binding.pinLayout);
        pin = new StringBuilder("");
        new Handler().postDelayed(() -> {
            inputAllowed = true;
            updateDots();
        }, 1000);
    }

    private void updateDots() {
        AuthManager.getInstance().updateDots(pin.toString(), binding.dot1,
                binding.dot2, binding.dot3, binding.dot4,
                binding.dot5, binding.dot6, () -> {
                    inputAllowed = false;
                    if (AuthManager.getInstance().checkAuth(pin.toString(), LoginActivity.this)) {
                        handler.postDelayed(() -> {
                            AuthManager.getInstance().authSuccess(LoginActivity.this);
                            unlockWallet();
                        }, 350);
                    } else {
                        AuthManager.getInstance().authFail(LoginActivity.this);
                        showFailedToUnlock();
                    }
                });
    }

    @Override
    public void onComplete(AuthType authType) {
        switch (authType.type) {
            case LOGIN:
                unlockWallet();
                break;
            default:
                super.onComplete(authType);
        }
    }

    @Override
    public void onCancel(AuthType authType) {

    }

    @Override
    public void onBalanceChanged(long balance) {

    }

    @Override
    public void showSendConfirmDialog(String message, int error, byte[] txHash) {
        BRExecutor.getInstance().forMainThreadTasks().execute(() -> {
            BRAnimator.showBreadSignal(LoginActivity.this,
                    error == 0 ? getString(R.string.Alerts_sendSuccess)
                            : getString(R.string.Alert_error),
                    error == 0 ? getString(R.string.Alerts_sendSuccessSubheader)
                            : message, error == 0 ? R.raw.success_check
                            : R.raw.error_check, () -> {
                        try {
                            getSupportFragmentManager().popBackStack();
                        } catch (IllegalStateException e) {
                        }
                    });
        });
    }
}