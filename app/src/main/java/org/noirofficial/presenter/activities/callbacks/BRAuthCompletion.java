package org.noirofficial.presenter.activities.callbacks;

import java.io.Serializable;

import org.noirofficial.presenter.activities.models.SendAsset;
import org.noirofficial.presenter.activities.models.SendAssetResponse;
import org.noirofficial.presenter.entities.PaymentItem;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 3/15/17.
 * Copyright (c) 2017 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
public interface BRAuthCompletion {
    class AuthType implements Serializable {

        public enum Type {
            DIGI_ID, LOGIN, POST_AUTH, SPENDING_LIMIT, SEND, SEND_ASSET, ASSET_BROADCAST
        }

        public Type type;
        public String bitId;
        public boolean deepLink;
        public String callbackUrl;
        public PaymentItem paymentItem;
        public SendAsset sendAsset;
        public SendAssetResponse sendAssetResponse;

        public AuthType(Type type) {
            this.type = type;
            if (type == Type.DIGI_ID || type == Type.SEND) {
                throw new RuntimeException("Wrong constructor, use the one with the corresponding params");
            }
        }

        public AuthType(SendAsset sendAsset) {
            this.type = Type.SEND_ASSET;
            this.sendAsset = sendAsset;
        }

        /**
         * This constructor is associated with send functionality, see the input param type
         * @param paymentRequest
         */
        public AuthType(PaymentItem paymentRequest) {
            this.type = Type.SEND;
            this.paymentItem = paymentRequest;
        }

        /**
         * This constructor is associated with Digi-ID inherently considering the types of the params
         * @param bitId
         * @param deepLink
         * @param callbackUrl
         */
        public AuthType(String bitId, boolean deepLink, String callbackUrl) {
            this.type = Type.DIGI_ID;
            this.bitId = bitId;
            this.deepLink = deepLink;
            this.callbackUrl = callbackUrl;
        }

        public AuthType(SendAssetResponse sendAssetResponse, SendAsset sendAsset) {
            this.type = Type.ASSET_BROADCAST;
            this.sendAssetResponse = sendAssetResponse;
            this.sendAsset = sendAsset;

        }
    }
    void onComplete(AuthType authType);
    void onCancel(AuthType type);
}