package org.noirofficial.tools.util;

import java.math.RoundingMode;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 2/16/16.
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

public class BRConstants {

    /**
     * Native library name
     */
    public static final String NATIVE_LIB_NAME =  "core-lib";


    /**
     * Permissions
     */
    public static final int CAMERA_REQUEST_ID = 34;

    /**
     * Request codes for auth
     */
    public static final int ASSETS_REQUEST_CODE = 110;
    public static final int SHOW_PHRASE_REQUEST_CODE = 111;
    public static final int PAY_REQUEST_CODE = 112;
    public static final int CANARY_REQUEST_CODE = 113;
    public static final int PUT_PHRASE_NEW_WALLET_REQUEST_CODE = 114;
    public static final int PUT_PHRASE_RECOVERY_WALLET_REQUEST_CODE = 115;
    public static final int REQUEST_PHRASE_BITID = 117;
    public static final int PROVE_PHRASE_REQUEST = 119;

    /**
     * Request codes for take picture
     */
    public static final int SCANNER_REQUEST = 201;
    public static final int ASSETS_SCANNER_REQUEST = 202;

    public static final String CANARY_STRING = "canary";
    public static final String FIRST_ADDRESS = "firstAddress";
    public static final String SECURE_TIME_PREFS = "secureTime";
    public static final String FEE_KB_PREFS = "feeKb";
    public static final String ECONOMY_FEE_KB_PREFS = "EconomyFeeKb";

    /**
     * BRSharedPrefs
     */
    public static final String PREFS_NAME = "MyPrefsFile";
    public static final String RECEIVE_ADDRESS = "receive_address";
    public static final String WALLET_NAME = "wallet_name";
    public static final String START_HEIGHT = "startHeight";
    public static final String LAST_BLOCK_HEIGHT = "lastBlockHeight";
    public static final String CURRENT_UNIT = "currencyUnit";
    public static final String CURRENT_CURRENCY = "currentCurrency";
    public static final String POSITION = "position";
    public static final String PHRASE_WRITTEN = "phraseWritten";
    public static final String ALLOW_SPEND = "allowSpend";
    public static final String USER_ID = "userId";
    public static final String GEO_PERMISSIONS_REQUESTED = "geoPermissionsRequested";

    /**
     * Currency units
     */
    public static final int CURRENT_UNIT_BITS = 0;
    public static final int CURRENT_UNIT_MBITS = 1;
    public static final int CURRENT_UNIT_BITCOINS = 2;

    public static final String bitcoinLowercase = "\u018A";
    public static final String bitcoinUppercase = "\u018A";

    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;
    public static final boolean WAL = true;

    public static final String receive = "receive-bitcoin";
    public static final String send = "send-bitcoin";

    private BRConstants() {
    }

}
