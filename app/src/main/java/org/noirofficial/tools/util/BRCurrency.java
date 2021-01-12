package org.noirofficial.tools.util;

import static org.noirofficial.tools.util.BRConstants.CURRENT_UNIT_BITS;

import android.content.Context;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Currency;
import java.util.Locale;

import org.noirofficial.tools.manager.BRSharedPrefs;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 6/28/16.
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

public class BRCurrency {
    public static final String TAG = BRCurrency.class.getName();
    private static DecimalFormat currencyFormat;
    private static DecimalFormatSymbols decimalFormatSymbols;

    static {
        currencyFormat = (DecimalFormat) DecimalFormat.getCurrencyInstance(Locale.getDefault());
        decimalFormatSymbols = currencyFormat.getDecimalFormatSymbols();
    }

    // amount is in currency or BTC (bits, mBTC or BTC)
    public static String getFormattedCurrencyString(Context app, String isoCurrencyCode,
            BigDecimal amount) {

        Currency currency;
        String symbol = null;
        int decimalPoints = 0;
        if ("NOR".equals(isoCurrencyCode)) {
            symbol = BRExchange.getBitcoinSymbol(app);
        } else {
            try {
                currency = Currency.getInstance(isoCurrencyCode);
            } catch (IllegalArgumentException e) {
                currency = Currency.getInstance(Locale.getDefault());
            }
            symbol = currency.getSymbol();
            decimalPoints = currency.getDefaultFractionDigits();
        }
        decimalFormatSymbols.setCurrencySymbol(symbol);
        currencyFormat.setGroupingUsed(true);
        if ("NOR".equals(isoCurrencyCode)) {
            currencyFormat.setMinimumFractionDigits(
                    BRSharedPrefs.getCurrencyUnit(app) == BRConstants.CURRENT_UNIT_BITCOINS ? 6
                            : 2);
            currencyFormat.setMaximumFractionDigits(
                    BRSharedPrefs.getCurrencyUnit(app) == BRConstants.CURRENT_UNIT_BITCOINS ? 6
                            : 2);
        } else {
            currencyFormat.setMaximumFractionDigits(decimalPoints);
        }
        currencyFormat.setDecimalFormatSymbols(decimalFormatSymbols);
        currencyFormat.setNegativePrefix(decimalFormatSymbols.getCurrencySymbol() + "-");
        currencyFormat.setNegativeSuffix("");
        return currencyFormat.format(amount.doubleValue());
    }

    public static String getSymbolByIso(Context app, String iso) {
        String symbol;
        if ("NOR".equals(iso)) {
            String currencySymbolString = BRConstants.bitcoinLowercase;
            if (app != null) {
                int unit = BRSharedPrefs.getCurrencyUnit(app);
                switch (unit) {
                    case CURRENT_UNIT_BITS:
                        currencySymbolString = BRConstants.bitcoinLowercase;
                        break;
                    case BRConstants.CURRENT_UNIT_MBITS:
                        currencySymbolString = "m" + BRConstants.bitcoinUppercase;
                        break;
                    case BRConstants.CURRENT_UNIT_BITCOINS:
                        currencySymbolString = BRConstants.bitcoinUppercase;
                        break;
                }
            }
            symbol = currencySymbolString;
        } else {
            Currency currency;
            try {
                currency = Currency.getInstance(iso);
            } catch (IllegalArgumentException e) {
                currency = Currency.getInstance(Locale.getDefault());
            }
            symbol = currency.getSymbol();
        }
        return Utils.isNullOrEmpty(symbol) ? iso : symbol;
    }

    //for now only use for BTC and Bits
    public static String getCurrencyName(Context app, String iso) {
        if ("NOR".equals(iso)) {
            if (app != null) {
                int unit = BRSharedPrefs.getCurrencyUnit(app);
                switch (unit) {
                    case CURRENT_UNIT_BITS:
                        return "µNOR";
                    case BRConstants.CURRENT_UNIT_MBITS:
                        return "mNOR";
                    case BRConstants.CURRENT_UNIT_BITCOINS:
                        return "NOR";
                }
            }
        }
        return iso;
    }

    public static int getMaxDecimalPlaces(String iso) {
        if (Utils.isNullOrEmpty(iso)) return 8;

        if (iso.equalsIgnoreCase("NOR")) {
            return 8;
        } else {
            try {
                Currency currency = Currency.getInstance(iso);
                return currency.getDefaultFractionDigits();
            } catch(IllegalArgumentException e) {
                return 2;
            }
        }

    }
}