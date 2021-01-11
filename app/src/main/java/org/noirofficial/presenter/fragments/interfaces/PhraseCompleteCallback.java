package org.noirofficial.presenter.fragments.interfaces;

import org.noirofficial.presenter.adapter.MultiTypeDataBoundAdapter;

public interface PhraseCompleteCallback extends MultiTypeDataBoundAdapter.ActionCallback {
    void phrase(String string);
}
