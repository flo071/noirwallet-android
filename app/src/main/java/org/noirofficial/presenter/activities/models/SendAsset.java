package org.noirofficial.presenter.activities.models;

import java.io.Serializable;

public class SendAsset implements Serializable {
    private String fee;
    private String[] sendutxo;
    private FinanceUTXO.Vout financeOutput;
    private String financeOutputTxid;
    private To[] to;
    public transient int divisibility;
    public transient AssetModel assetModel;

    private static final int TO_DESTINATION = 0;
    private static final int TO_CHANGE = 1;

    public static final int INVALID_AMOUNT = -1;

    public SendAsset(String fee, String changeAddress, String[] sendutxo, FinanceUTXO.Vout financeOutput, String financeOutputTxid, String destinationAddress,
                     int totalAssetQuantity, String assetId, int divisibility, AssetModel assetModel) {
        this.fee = fee;

        this.sendutxo = sendutxo;
        this.financeOutput = financeOutput;
        this.financeOutputTxid = financeOutputTxid;

        to = new To[2];
        to[TO_DESTINATION] = new To();
        to[TO_DESTINATION].address = destinationAddress;
        to[TO_DESTINATION].assetId = assetId;

        to[TO_CHANGE] = new To();
        to[TO_CHANGE].address = changeAddress;
        to[TO_CHANGE].assetId = assetId;
        to[TO_CHANGE].amount = totalAssetQuantity;

        this.divisibility = divisibility;
        this.assetModel = assetModel;
    }

    public String getDestinationAddress() {
        return to[TO_DESTINATION].address;
    }

    public String getDestinationAssetId() {
        return to[TO_DESTINATION].assetId;
    }

    public int getDestinationAmount() {
        return to[TO_DESTINATION].amount;
    }

    public int getDisplayDestinationAmount() {
        return Double.valueOf((double) getDestinationAmount() / Math.pow(10, divisibility)).intValue();
    }

    public void setQuantity(int amount) {
        to[TO_DESTINATION].amount = amount;
        to[TO_CHANGE].amount -= amount;
        if (to[TO_CHANGE].amount < 0) {
            //Too much selected, invalid amount
            to[TO_DESTINATION].amount = INVALID_AMOUNT;
        } else if (to[TO_CHANGE].amount == 0) {
            //Total amount consumed, remove the change output
            To[] single = new To[1];
            single[TO_DESTINATION] = to[TO_DESTINATION];
            to = single;
        }
    }

    public boolean isValidAmount() {
        return to[TO_DESTINATION].amount != INVALID_AMOUNT;
    }

    public boolean isCompleteSpend() {
        return to.length == 1;
    }

    public class To implements Serializable {
        String address;
        int amount;
        String assetId;
    }
}