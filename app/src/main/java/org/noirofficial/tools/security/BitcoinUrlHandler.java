package org.noirofficial.tools.security;

import android.text.TextUtils;
import android.util.Log;

import java.math.BigDecimal;
import java.net.URI;

import org.noirofficial.presenter.entities.PaymentRequestWrapper;
import org.noirofficial.presenter.entities.RequestObject;
import org.noirofficial.wallet.BRWalletManager;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 10/19/15.
 * Copyright (c) 2016 breadwallet LLC
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

public class BitcoinUrlHandler {
    private static final String TAG = BitcoinUrlHandler.class.getName();
    private static final Object lockObject = new Object();

    public static boolean isBitcoinUrl(String url) {
        RequestObject requestObject = getRequestFromString(url);
        // return true if the request is valid url and has param: r or param: address
        // return true if it is a valid bitcoinPrivKey
        return (requestObject != null && (requestObject.r != null || requestObject.address != null)
                || BRWalletManager.getInstance().isValidBitcoinBIP38Key(url)
                || BRWalletManager.getInstance().isValidBitcoinPrivateKey(url));
    }

    public static RequestObject getScannedQRRequest(String str) {
        RequestObject obj = new RequestObject();
        String tmp = str.trim().replaceAll("\n", "").replaceAll(" ", "%20");
        if (!tmp.startsWith("digibyte://")) {
            if (!tmp.startsWith("digibyte:")) {
                tmp = "digibyte://".concat(tmp);
            } else {
                tmp = tmp.replace("digibyte:", "digibyte://");
            }
        }
        URI uri = URI.create(tmp);
        String host = uri.getHost();
        if (!TextUtils.isEmpty(host)) {
            String addrs = host.trim();
            if (BRWalletManager.validateAddress(addrs)) {
                obj.address = addrs;
            }
        }
        String query = uri.getQuery();
        if (query == null) return obj;
        String[] params = query.split("&");
        for (String s : params) {
            String[] keyValue = s.split("=", 2);
            if (keyValue.length != 2)
                continue;
            if (keyValue[0].trim().equals("amount")) {
                obj.amount = keyValue[1].trim();
            }
        }
        return obj;
    }

    public static RequestObject getRequestFromString(String str) {
        if (str == null || str.isEmpty()) return null;
        RequestObject obj = new RequestObject();

        String tmp = str.trim().replaceAll("\n", "").replaceAll(" ", "%20");

        if (!tmp.startsWith("digibyte://")) {
            if (!tmp.startsWith("digibyte:"))
                tmp = "digibyte://".concat(tmp);
            else
                tmp = tmp.replace("digibyte:", "digibyte://");
        }
        URI uri;
        try {
            uri = URI.create(tmp);
        } catch (IllegalArgumentException ex) {
            Log.e(TAG, "getRequestFromString: ", ex);
            return null;
        }

        String host = uri.getHost();
        if (host != null) {
            String addrs = host.trim();
            if (BRWalletManager.validateAddress(addrs)) {
                obj.address = addrs;
            }
        }
        String query = uri.getQuery();
        if (query == null) return obj;
        String[] params = query.split("&");
        for (String s : params) {
            String[] keyValue = s.split("=", 2);
            if (keyValue.length != 2)
                continue;
            if (keyValue[0].trim().equals("amount")) {
                try {
                    BigDecimal bigDecimal = new BigDecimal(keyValue[1].trim());
                    obj.amount = bigDecimal.multiply(new BigDecimal("100000000")).toString();
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            } else if (keyValue[0].trim().equals("label")) {
                obj.label = keyValue[1].trim();
            } else if (keyValue[0].trim().equals("message")) {
                obj.message = keyValue[1].trim();
            } else if (keyValue[0].trim().startsWith("req")) {
                obj.req = keyValue[1].trim();
            } else if (keyValue[0].trim().startsWith("r")) {
                obj.r = keyValue[1].trim();
            }
        }
        return obj;
    }

    public static native PaymentRequestWrapper parsePaymentRequest(byte[] req);

    public static native String parsePaymentACK(byte[] req);

    public static native byte[] getCertificatesFromPaymentRequest(byte[] req, int index);

}
