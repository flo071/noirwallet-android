package org.noirofficial.presenter.fragments;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.noirofficial.databinding.FragmentNotificationBinding;
import org.noirofficial.presenter.fragments.interfaces.OnBackPressListener;
import org.noirofficial.presenter.fragments.models.FragmentSignalViewModel;
import org.noirofficial.presenter.activities.callbacks.BROnSignalCompletion;
import org.noirofficial.tools.animation.BRAnimator;


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

public class FragmentSignal extends Fragment implements OnBackPressListener {
    private static final String TAG = FragmentSignal.class.getName();

    public static final String TITLE = "title";
    public static final String ICON_DESCRIPTION = "iconDescription";
    public static final String RES_ID = "resId";
    private BROnSignalCompletion completion;
    private FragmentNotificationBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
        binding = FragmentNotificationBinding.inflate(inflater);
        FragmentSignalViewModel viewModel = new FragmentSignalViewModel();
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            String title = bundle.getString(TITLE, "");
            String description = bundle.getString(ICON_DESCRIPTION, "");
            int resId = bundle.getInt(RES_ID, 0);
            viewModel.setTitle(title);
            viewModel.setDescription(description);
            viewModel.setIcon(resId);
        } else {
            Log.e(TAG, "onCreateView: bundle is null!");
        }
        binding.setData(viewModel);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ObjectAnimator colorFade = BRAnimator.animateBackgroundDim(binding.background, false, null);
        colorFade.setStartDelay(350);
        colorFade.setDuration(500);
        colorFade.start();
    }

    public void setCompletion(BROnSignalCompletion completion) {
        this.completion = completion;
    }

    @Override
    public void onResume() {
        super.onResume();
        new Handler().postDelayed(() -> {
            try {
                if (getActivity() != null)
                    getActivity().getFragmentManager().popBackStack();
            } catch (Exception ignored) {

            }
            new Handler().postDelayed(() -> {
                fadeOutRemove();
            }, 300);
        }, 2000);
    }

    private void fadeOutRemove() {
        ObjectAnimator colorFade = BRAnimator.animateBackgroundDim(binding.background, true,
                () -> {
                    new Handler().postDelayed(() -> {
                        if (completion != null) {
                            completion.onComplete();
                            completion = null;
                        }
                    }, 300);
                    remove();
                });
        colorFade.start();
    }

    private void remove() {
        if (getFragmentManager() == null) {
            return;
        }
        try {
            getFragmentManager().popBackStack();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        fadeOutRemove();
    }

}