package org.noirofficial.presenter.activities.models;

import java.io.Serializable;

public class SendAssetResponse implements Serializable {
    String txHex;

    public String getTxHex() {
        return txHex;
    }

    @Override
    public String toString() {
        return "SendAssetResponse{" +
                "txHex='" + txHex + '\'' +
                '}';
    }
}
