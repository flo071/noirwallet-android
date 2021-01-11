package org.noirofficial.presenter.activities.base;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.common.io.ByteStreams;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.platform.tools.BRBitId;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.noirofficial.R;
import org.noirofficial.presenter.activities.BreadActivity;
import org.noirofficial.presenter.activities.utils.ActivityUtils;
import org.noirofficial.presenter.fragments.FragmentFingerprint;
import org.noirofficial.presenter.fragments.FragmentNumberPicker;
import org.noirofficial.presenter.fragments.FragmentPin;
import org.noirofficial.presenter.fragments.interfaces.OnBackPressListener;
import org.noirofficial.presenter.activities.callbacks.BRAuthCompletion;
import org.noirofficial.tools.animation.BRAnimator;
import org.noirofficial.tools.crypto.AssetsHelper;
import org.noirofficial.tools.security.AuthManager;
import org.noirofficial.tools.security.BRKeyStore;
import org.noirofficial.tools.security.BitcoinUrlHandler;
import org.noirofficial.tools.security.PostAuth;
import org.noirofficial.tools.threads.BRExecutor;
import org.noirofficial.tools.util.BRConstants;
import org.noirofficial.wallet.BRWalletManager;
import spencerstudios.com.bungeelib.Bungee;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 5/23/17.
 * Copyright (c) 2017 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
public abstract class BRActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener,
        BRAuthCompletion {
    private final String TAG = this.getClass().getName();
    private CopyOnWriteArrayList<OnBackPressListener> backClickListeners = new CopyOnWriteArrayList<>();
    public static final int QR_IMAGE_PROCESS = 244;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bungee.slideRight(this);
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        if (!BRKeyStore.initCrypto(this)) {
            ActivityUtils.showCryptoFailureDialog(this);
        }
    }

    protected void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    protected void setToolbarTitle(int resId) {
        setToolbarTitle(getString(resId));
    }

    protected void setToolbarTitle(String text) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        TextView title = toolbar.findViewById(R.id.toolbar_title);
        title.setText(text);
    }

    @Override
    protected void onResume() {
        if (!ActivityUtils.isAppSafe(this)) {
            if (AuthManager.getInstance().isWalletDisabled(this)) {
                AuthManager.getInstance().setWalletDisabled(this);
            }
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        for (Fragment fragment : fragments) {
            if (FragmentFingerprint.class.getSimpleName().equals(fragment.getTag()) ||
                    FragmentPin.class.getSimpleName().equals(fragment.getTag()) ||
                    FragmentNumberPicker.class.getSimpleName().equals(fragment.getTag())) {
                transaction.remove(fragment);
            }
        }
        transaction.commitAllowingStateLoss();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        switch (requestCode) {
            case BRConstants.ASSETS_SCANNER_REQUEST: {
                if (data == null) {
                    return;
                }
                String result = data.getStringExtra("SCAN_RESULT");
                if (TextUtils.isEmpty(result)) {
                    return;
                }
                if (AssetsHelper.Companion.getInstance().pendingAssetTx != null) {
                    result = result.replace("digibyte://", "");
                    result = result.replace("digibyte:", "");
                    if (BRWalletManager.validateAddress(result)) {
                        AssetsHelper.Companion.getInstance().pendingAssetTx.setDestinationAddress(result);
                        AssetsHelper.Companion.getInstance().sendPendingAssetTx(this);
                    } else {
                        Toast.makeText(this, R.string.Send_invalidAddressTitle, Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            }
            case BRConstants.SCANNER_REQUEST: {
                if (resultCode == Activity.RESULT_OK) {
                    new Handler().postDelayed(() -> {
                        String result = data.getStringExtra("SCAN_RESULT");
                        if (BitcoinUrlHandler.isBitcoinUrl(result)) {
                            BRAnimator.showOrUpdateSendFragment(BRActivity.this, result);
                        } else if (BRBitId.isBitId(result)) {
                            BRBitId.digiIDAuthPrompt(BRActivity.this, result, false);
                        } else {
                            Log.e(TAG, "onActivityResult: not bitcoin address NOR bitID");
                        }
                    }, 500);
                }
                break;
            }
            case QR_IMAGE_PROCESS: {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(data.getData());
                    byte[] rawBytes = ByteStreams.toByteArray(inputStream);
                    Bitmap bMap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.length);
                    String contents = null;
                    int[] intArray = new int[bMap.getWidth() * bMap.getHeight()];
                    //copy pixel data from the Bitmap into the 'intArray' array
                    bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(),
                            bMap.getHeight());

                    LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(),
                            bMap.getHeight(), intArray);
                    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                    Reader reader = new MultiFormatReader();
                    Result result = reader.decode(bitmap);
                    contents = result.getText();
                    if (BitcoinUrlHandler.isBitcoinUrl(contents)) {
                        BRAnimator.showOrUpdateSendFragment(this, contents);
                    } else if (BRBitId.isBitId(contents)) {
                        BRBitId.digiIDAuthPrompt(this, contents, false);
                    } else {
                        Log.e(BreadActivity.class.getSimpleName(),
                                "onActivityResult: not bitcoin address NOR bitID");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case BRConstants.CAMERA_REQUEST_ID: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    BRAnimator.openScanner(this);
                }
                break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (backClickListeners.size() == 0) {
            super.onBackPressed();
            Bungee.slideLeft(this);
        } else {
            for (OnBackPressListener onBackPressListener : backClickListeners) {
                onBackPressListener.onBackPressed();
            }
        }
    }

    @Override
    public void onBackStackChanged() {
        //Add back press listeners
        backClickListeners.clear();
        for (int i = 0; i < getSupportFragmentManager().getBackStackEntryCount(); i++) {
            FragmentManager.BackStackEntry backStackEntry = getSupportFragmentManager().getBackStackEntryAt(i);
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(backStackEntry.getName());
            if (fragment instanceof OnBackPressListener) {
                OnBackPressListener onBackPressListener = (OnBackPressListener) fragment;
                if (!backClickListeners.contains(onBackPressListener)) {
                    backClickListeners.add(onBackPressListener);
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home || item.getItemId() == R.id.home) {
            finish();
            Bungee.slideLeft(this);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onComplete(AuthType authType) {
        //These two switch cases are in the base activity because they can both be invoked
        //from the Login screen and from the main transaction list screen
        switch(authType.type) {
            case SEND:
                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(() -> {
                    PostAuth.instance.onPublishTxAuth(BRActivity.this, authType.paymentItem);
                });
                break;
            case DIGI_ID:
                BRBitId.digiIDSignAndRespond(BRActivity.this, authType.bitId, authType.deepLink,
                        authType.callbackUrl);
                break;
        }
    }

    @Override
    public void onCancel(AuthType type) {

    }
}