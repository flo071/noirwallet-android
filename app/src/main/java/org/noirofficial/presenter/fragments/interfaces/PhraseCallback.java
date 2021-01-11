package org.noirofficial.presenter.fragments.interfaces;

import org.noirofficial.presenter.adapter.MultiTypeDataBoundAdapter;

public interface PhraseCallback extends MultiTypeDataBoundAdapter.ActionCallback {
    void onClick(String string);
}
