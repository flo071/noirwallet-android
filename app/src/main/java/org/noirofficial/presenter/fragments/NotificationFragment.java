package org.noirofficial.presenter.fragments;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.noirofficial.presenter.fragments.interfaces.DialogCompleteCallback;
import org.noirofficial.presenter.fragments.interfaces.OnBackPressListener;
import org.noirofficial.tools.animation.BRAnimator;

public abstract class NotificationFragment extends Fragment implements OnBackPressListener {
    protected ViewGroup background;
    private DialogCompleteCallback dialogCompleteCallback;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (background == null) {
            throw new IllegalArgumentException(
                    "Set the background ViewGroup in implementing class");
        }
        ObjectAnimator colorFade = BRAnimator.animateBackgroundDim(background, false, null);
        colorFade.setStartDelay(350);
        colorFade.setDuration(500);
        colorFade.start();
    }

    protected void setCompletion(DialogCompleteCallback dialogCompleteCallback) {
        this.dialogCompleteCallback = dialogCompleteCallback;
    }

    protected void fadeOutRemove() {
        ObjectAnimator colorFade = BRAnimator.animateBackgroundDim(background, true, () -> {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (dialogCompleteCallback != null) {
                    dialogCompleteCallback.onComplete();
                    dialogCompleteCallback = null;
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