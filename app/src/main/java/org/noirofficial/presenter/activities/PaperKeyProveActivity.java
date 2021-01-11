package org.noirofficial.presenter.activities;

import android.content.Intent;
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Random;

import org.noirofficial.R;
import org.noirofficial.databinding.ActivityPaperKeyProveBinding;
import org.noirofficial.presenter.activities.callbacks.ActivityPaperKeyProveCallback;
import org.noirofficial.presenter.activities.base.BRActivity;
import org.noirofficial.tools.animation.BRAnimator;
import org.noirofficial.tools.animation.BRDialog;
import org.noirofficial.tools.animation.SpringAnimator;
import org.noirofficial.tools.manager.BRReportsManager;
import org.noirofficial.tools.manager.BRSharedPrefs;
import org.noirofficial.tools.security.SmartValidator;
import org.noirofficial.tools.util.Bip39Reader;
import org.noirofficial.tools.util.Utils;


public class PaperKeyProveActivity extends BRActivity implements TextView.OnEditorActionListener {
    private static final String CLEAN_PHRASE = "PaperKeyProveActivity:CleanPhrase";
    private ActivityPaperKeyProveBinding binding;
    private String[] wordArray;
    private int firstIndex;
    private int secondIndex;

    private ActivityPaperKeyProveCallback callback = new ActivityPaperKeyProveCallback() {
        @Override
        public void onSubmitClick() {
            if (isWordCorrect(true) && isWordCorrect(false)) {
                Utils.hideKeyboard(PaperKeyProveActivity.this);
                BRSharedPrefs.putPhraseWroteDown(PaperKeyProveActivity.this, true);
                BRAnimator.showBreadSignal(PaperKeyProveActivity.this,
                        getString(R.string.Alerts_paperKeySet),
                        getString(R.string.Alerts_paperKeySetSubheader),
                        R.raw.success_check, () -> {
                            if (BRSharedPrefs.digiIDFocus(PaperKeyProveActivity.this)) {
                                BRAnimator.startBreadActivity(PaperKeyProveActivity.this, true);
                            } else {
                                BRAnimator.startBreadActivity(PaperKeyProveActivity.this, false);
                                overridePendingTransition(R.anim.enter_from_right,
                                        R.anim.exit_to_left);
                                finishAffinity();
                            }
                        });
            } else {
                if (!isWordCorrect(true)) {
                    binding.wordEdittextFirst.setTextColor(ContextCompat.getColor(PaperKeyProveActivity.this,  R.color.red_text));
                    SpringAnimator.failShakeAnimation(PaperKeyProveActivity.this,
                            binding.wordEdittextFirst);
                }
                if (!isWordCorrect(false)) {
                    binding.wordEdittextSecond.setTextColor(ContextCompat.getColor(PaperKeyProveActivity.this,R.color.red_text));
                    SpringAnimator.failShakeAnimation(PaperKeyProveActivity.this,
                            binding.wordEdittextSecond);
                }
            }
        }
    };

    public static void show(AppCompatActivity activity, String cleanPhrase) {
        Intent intent = new Intent(activity, PaperKeyProveActivity.class);
        intent.putExtra(CLEAN_PHRASE, cleanPhrase);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_paper_key_prove);
        binding.setCallback(callback);
        binding.setEditorActionListener(this);
        binding.setTextWatcher(new BRTextWatcher());
        setupToolbar();
        setToolbarTitle(R.string.SecurityCenter_paperKeyTitle);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        String cleanPhrase = getCleanPhrase();
        wordArray = cleanPhrase.split(" ");
        if (wordArray.length == 12 && cleanPhrase.charAt(cleanPhrase.length() - 1) == '\0') {
            BRDialog.showCustomDialog(this, getString(R.string.JailbreakWarnings_title),
                    getString(R.string.Alert_keystore_generic_android),
                    getString(R.string.Button_ok), null,
                    brDialogView -> brDialogView.dismissWithAnimation(), null, null, 0);
            BRReportsManager.reportBug(new IllegalArgumentException(
                    "Paper Key error, please contact support at breadwallet.com"), false);
        } else {
            randomWordsSetUp(wordArray);
        }
        new Handler().postDelayed(() -> {
            binding.wordContainerFirst.setHint(
                    String.format(Locale.getDefault(), getString(R.string.ConfirmPaperPhrase_word),
                            (firstIndex)));
            binding.wordContainerFirst.bringToFront();
            binding.wordContainerSecond.setHint(
                    String.format(Locale.getDefault(), getString(R.string.ConfirmPaperPhrase_word),
                            (secondIndex)));
            binding.wordContainerSecond.bringToFront();
        }, 10);
    }

    private String getCleanPhrase() {
        return getIntent().getStringExtra(CLEAN_PHRASE);
    }
    private void randomWordsSetUp(String[] words) {
        final Random random = new SecureRandom();
        //Get first and second indexes
        firstIndex = random.nextInt(10) + 1;
        secondIndex = random.nextInt(10) + 1;
        //If the second index is the same as the first, find another
        while (secondIndex == firstIndex) {
            secondIndex = random.nextInt(10) + 1;
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_NULL) {
            binding.buttonSubmit.performClick();
            return true;
        }
        return false;
    }


    public class BRTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            validateWord(binding.wordEdittextFirst);
            validateWord(binding.wordEdittextSecond);
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }

    private void validateWord(EditText view) {
        String word = view.getText().toString();
        boolean valid = SmartValidator.isWordValid(PaperKeyProveActivity.this, word);
        view.setTextColor(ContextCompat.getColor(PaperKeyProveActivity.this,valid ? R.color.light_gray : R.color.red_text));
        if (isWordCorrect(true)) {
            view.setTextColor(ContextCompat.getColor(PaperKeyProveActivity.this,R.color.green_text));
        }
    }

    private boolean isWordCorrect(boolean first) {
        if (first) {
            String edit = Bip39Reader.cleanWord(binding.wordEdittextFirst.getText().toString());
            return SmartValidator.isWordValid(PaperKeyProveActivity.this, edit)
                    && edit.equalsIgnoreCase(wordArray[firstIndex - 1]);
        } else {
            String edit = Bip39Reader.cleanWord(binding.wordEdittextSecond.getText().toString());
            return SmartValidator.isWordValid(PaperKeyProveActivity.this, edit)
                    && edit.equalsIgnoreCase(wordArray[secondIndex - 1]);
        }
    }
}
