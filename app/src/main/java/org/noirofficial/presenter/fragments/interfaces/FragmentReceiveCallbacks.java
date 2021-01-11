package org.noirofficial.presenter.fragments.interfaces;

import org.noirofficial.presenter.customviews.BRKeyboard;

public interface FragmentReceiveCallbacks extends BRKeyboard.OnInsertListener {

    void shareEmailClick();
    void shareTextClick();

    void shareCopyClick();
    void shareButtonClick();

    void shareExternalClick();
    void addressClick();
    void requestButtonClick();
    void backgroundClick();
    void qrImageClick();
    void closeClick();

    void onAmountEditClick();
    void onIsoButtonClick();
}
