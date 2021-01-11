package org.noirofficial.presenter.fragments.interfaces;

import org.noirofficial.presenter.customviews.BRKeyboard;

public interface PinFragmentCallback extends BRKeyboard.OnInsertListener {
    void onCancelClick();
}
