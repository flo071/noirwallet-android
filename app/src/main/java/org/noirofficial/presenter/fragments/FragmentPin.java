package org.noirofficial.presenter.fragments;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.BindingAdapter;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import org.noirofficial.NoirWallet;
import org.noirofficial.R;
import org.noirofficial.databinding.FragmentBreadPinBinding;
import org.noirofficial.presenter.fragments.interfaces.OnBackPressListener;
import org.noirofficial.presenter.fragments.interfaces.PinFragmentCallback;
import org.noirofficial.presenter.fragments.models.PinFragmentViewModel;
import org.noirofficial.presenter.activities.callbacks.BRAuthCompletion;
import org.noirofficial.tools.animation.BRAnimator;
import org.noirofficial.tools.animation.SpringAnimator;
import org.noirofficial.tools.security.AuthManager;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 6/29/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class FragmentPin extends Fragment implements OnBackPressListener {
    private static final String TAG = FragmentPin.class.getName();

    private BRAuthCompletion completion;
    private FragmentBreadPinBinding binding;
    protected StringBuilder pin = new StringBuilder();
    private boolean authComplete = false;
    protected BRAuthCompletion.AuthType authType;

    private PinFragmentCallback mPinFragmentCallback = new PinFragmentCallback() {
        @Override
        public void onClick(String key) {
            handleClick(key);
        }

        @Override
        public void onCancelClick() {
            fadeOutRemove(false);
        }
    };

    public static void show(AppCompatActivity activity, String title, String message, BRAuthCompletion.AuthType type) {
        FragmentPin fragmentPin = new FragmentPin();
        Bundle args = new Bundle();
        args.putString("title", title);
        if (TextUtils.isEmpty(message)) {
            message = activity.getString(R.string.VerifyPin_continueBody);
        }
        args.putString("message", message);

        //We cannot use fragment arguments for AuthType as it's potentially too large
        fragmentPin.setAuthType(type);

        fragmentPin.setArguments(args);
        FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.animator.from_bottom, R.animator.to_bottom, R.animator.from_bottom, R.animator.to_bottom);
        transaction.add(android.R.id.content, fragmentPin, FragmentPin.class.getName());
        transaction.addToBackStack(FragmentPin.class.getName());
        transaction.commitAllowingStateLoss();
    }

    private void setAuthType(BRAuthCompletion.AuthType authType) {
        this.authType = authType;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof BRAuthCompletion) {
            completion = (BRAuthCompletion) context;
        } else {
            throw new RuntimeException("Failed to have the containing activity implement BRAuthCompletion");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        binding = FragmentBreadPinBinding.inflate(inflater);
        binding.setCallback(mPinFragmentCallback);
        binding.mainLayout.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
        Bundle bundle = getArguments();
        PinFragmentViewModel viewModel = new PinFragmentViewModel();
        if (bundle != null) {
            viewModel.setTitle(bundle.getString("title"));
            viewModel.setMessage(bundle.getString("message"));
        }
        binding.setData(viewModel);
        binding.executePendingBindings();
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (authType == null) {
            getFragmentManager().popBackStack();
            return;
        }
        ObjectAnimator colorFade = BRAnimator.animateBackgroundDim(binding.mainLayout, false, null);
        colorFade.setStartDelay(350);
        colorFade.setDuration(500);
        colorFade.start();
        Animator upFromBottom = AnimatorInflater.loadAnimator(getContext(), R.animator.from_bottom);
        upFromBottom.setTarget(binding.brkeyboard);
        upFromBottom.setDuration(1000);
        upFromBottom.setInterpolator(new DecelerateInterpolator());
        upFromBottom.start();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateDots();
    }

    private void handleClick(@Nullable final String key) {
        if (key == null) {
            Log.e(TAG, "handleClick: key is null! ");
            return;
        }
        if (key.isEmpty()) {
            handleDeleteClick();
        } else if (!onlyDigits() || Character.isDigit(key.charAt(0))) {
            handleDigitClick(key.substring(0, 1));
        } else {
            Log.e(TAG, "handleClick: oops: " + key);
        }
    }

    protected boolean onlyDigits() {
        return true;
    }

    protected void handleDigitClick(@NonNull final String dig) {
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

    protected void updateDots() {
        if (authComplete) {
            return;
        }
        AuthManager.getInstance().updateDots(pin.toString(), binding.dot1,
                binding.dot2, binding.dot3, binding.dot4, binding.dot5, binding.dot6, () -> {
                    if (AuthManager.getInstance().checkAuth(pin.toString(), getContext())) {
                        authComplete = true;
                        AuthManager.getInstance().authSuccess(getContext());
                        fadeOutRemove(true);
                    } else {
                        SpringAnimator.failShakeAnimation(getActivity(), binding.pinLayout);
                        pin = new StringBuilder("");
                        new Handler().postDelayed(() -> updateDots(), 250);
                        AuthManager.getInstance().authFail(getContext());
                    }
                });
    }

    protected BRAuthCompletion.AuthType getType() {
        return authType;
    }

    protected void fadeOutRemove(boolean authenticated) {
        ObjectAnimator colorFade = BRAnimator.animateBackgroundDim(binding.mainLayout, true, null);
        colorFade.setDuration(500);
        Animator downToBottom = AnimatorInflater.loadAnimator(NoirWallet.getContext(), R.animator.to_bottom);
        downToBottom.setTarget(binding.brkeyboard);
        downToBottom.setDuration(500);
        downToBottom.setInterpolator(new DecelerateInterpolator());
        AnimatorSet set = new AnimatorSet();
        set.playSequentially(colorFade, downToBottom);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                remove();
                if (authenticated) {
                    Handler handler = new Handler(Looper.myLooper());
                    handler.postDelayed(() -> {
                        if (completion != null) completion.onComplete(getType());
                    }, 350);
                }
            }
        });
        set.start();
    }

    private void remove() {
        if (getFragmentManager() == null) {
            return;
        }
        try {
            getFragmentManager().popBackStack();
        } catch(IllegalStateException e) {
            //Race condition
        }
    }

    @Override
    public void onBackPressed() {
        fadeOutRemove(false);
    }

    @BindingAdapter("setMovementMethod")
    public static void setMovementMethod(TextView textview, boolean set) {
        textview.setMovementMethod(ScrollingMovementMethod.getInstance());
    }
}