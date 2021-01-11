package org.noirofficial.presenter.fragments.interfaces;

import android.view.View;
import android.widget.TextView;

import org.noirofficial.presenter.customviews.BRKeyboard;

public interface FragmentSendCallbacks extends View.OnKeyListener, BRKeyboard.OnInsertListener,
        TextView.OnEditorActionListener {
    void onAmountClickListener();

    void onPasteClickListener();

    void onIsoButtonClickListener();

    void onScanClickListener();

    void onSendClickListener();

    void onCloseClickListener();

    void onMaxSendButtonClickListener();
}