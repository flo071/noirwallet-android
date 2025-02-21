package org.noirofficial.tools.security;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import java.math.BigDecimal;
import java.util.Locale;

import org.noirofficial.R;
import org.noirofficial.presenter.entities.PaymentItem;
import org.noirofficial.presenter.activities.callbacks.BRAuthCompletion;
import org.noirofficial.tools.animation.BRDialog;
import org.noirofficial.tools.manager.BRReportsManager;
import org.noirofficial.tools.manager.BRSharedPrefs;
import org.noirofficial.tools.threads.BRExecutor;
import org.noirofficial.tools.util.BRConstants;
import org.noirofficial.tools.util.BRCurrency;
import org.noirofficial.tools.util.BRExchange;
import org.noirofficial.wallet.BRWalletManager;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 4/25/17.
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
public class BRSender {
    private static final String TAG = BRSender.class.getName();

    private static BRSender instance;
    private Handler handler = new Handler(Looper.getMainLooper());
    private final static long FEE_EXPIRATION_MILLIS = 72 * 60 * 60 * 1000L;

    private BRSender() {
    }

    public static BRSender getInstance() {
        if (instance == null) instance = new BRSender();
        return instance;
    }


    public interface UpdateTxFeeCallback {
        void onTxFeeUpdated();
    }

    /**
     * Create tx from the PaymentItem object and try to send it
     */
    public void sendTransaction(final AppCompatActivity app, final PaymentItem request,
            UpdateTxFeeCallback updateTxFeeCallback) {
        //array in order to be able to modify the first element from an inner block (can't be final)
        final String[] errTitle = {null};
        final String[] errMessage = {null};
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(() -> {
            try {
                if (updateTxFeeCallback != null) {
                    handler.post(() -> updateTxFeeCallback.onTxFeeUpdated());
                }
                tryPay(app, request);
                return; //return so no error is shown
            } catch (InsufficientFundsException ignored) {
                errTitle[0] = app.getString(R.string.Alerts_sendFailure);
                errMessage[0] = "Insufficient Funds";
            } catch (AmountSmallerThanMinException e) {
                long minAmount = BRWalletManager.getInstance().getMinOutputAmount();
                errTitle[0] = app.getString(R.string.Alerts_sendFailure);
                errMessage[0] = String.format(Locale.getDefault(),
                        app.getString(R.string.PaymentProtocol_Errors_smallPayment),
                        BRConstants.bitcoinLowercase + new BigDecimal(minAmount).divide(
                                new BigDecimal(100), BRConstants.ROUNDING_MODE));
            } catch (SpendingNotAllowed spendingNotAllowed) {
                showSpendNotAllowed(app);
                return;
            } catch (FeeNeedsAdjust feeNeedsAdjust) {
                //offer to change amount, so it would be enough for fee
//                    showFailed(app); //just show failed for now
                showAdjustFee((Activity) app, request);
                return;
            }
//            catch (FeeOutOfDate ex) {
//                //Fee is out of date, show not connected error
//                Crashlytics.logException(ex);
//                BRExecutor.getInstance().forMainThreadTasks().execute(
//                        () -> BRDialog.showCustomDialog(app,
//                                app.getString(R.string.Alerts_sendFailure),
//                                app.getString(R.string.NodeSelector_notConnected),
//                                app.getString(R.string.Button_ok), null,
//                                brDialogView -> brDialogView.dismiss(), null, null, 0));
//                return;
//            }
            catch (SomethingWentWrong somethingWentWrong) {
                somethingWentWrong.printStackTrace();
                Crashlytics.logException(somethingWentWrong);
                BRExecutor.getInstance().forMainThreadTasks().execute(
                        () -> BRDialog.showCustomDialog(app,
                                app.getString(R.string.Alerts_sendFailure),
                                "Something went wrong", app.getString(R.string.Button_ok), null,
                                brDialogView -> brDialogView.dismiss(), null, null, 0));
                return;
            }

            //show the message if we have one to show
            if (errTitle[0] != null && errMessage[0] != null) {
                BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        BRDialog.showCustomDialog(app, errTitle[0], errMessage[0],
                                app.getString(R.string.Button_ok), null,
                                brDialogView -> brDialogView.dismiss(), null, null, 0);
                    }
                });
            }
        });
    }

    /**
     * Try transaction and throw appropriate exceptions if something was wrong
     * BLOCKS
     */
    public void tryPay(final AppCompatActivity app, final PaymentItem paymentRequest)
            throws InsufficientFundsException,
            AmountSmallerThanMinException, SpendingNotAllowed, FeeNeedsAdjust, SomethingWentWrong {
        if (paymentRequest == null || paymentRequest.addresses == null) {
            Log.e(TAG, "tryPay: ERROR: paymentRequest: " + paymentRequest);
            String message =
                    paymentRequest == null ? "paymentRequest is null" : "addresses is null";
            BRReportsManager.reportBug(
                    new RuntimeException("paymentRequest is malformed: " + message), true);
            throw new SomethingWentWrong("wrong parameters: paymentRequest");
        }
        long amount = paymentRequest.amount;
        long balance = BRWalletManager.getInstance().getBalance(app);
        final BRWalletManager m = BRWalletManager.getInstance();
        long minOutputAmount = BRWalletManager.getInstance().getMinOutputAmount();
        final long maxOutputAmount = BRWalletManager.getInstance().getMaxOutputAmount();

        // check if spending is allowed
        if (!BRSharedPrefs.getAllowSpend(app)) {
            throw new SpendingNotAllowed();
        }

        //check if amount isn't smaller than the min amount
        if (isSmallerThanMin(app, paymentRequest)) {
            throw new AmountSmallerThanMinException(amount, minOutputAmount);
        }

        //amount is larger than balance
        if (isLargerThanBalance(app, paymentRequest)) {
            throw new InsufficientFundsException(amount, balance);
        }

        //not enough for fee
        if (notEnoughForFee(app, paymentRequest)) {
            //weird bug when the core BRWalletManager is NULL
            if (maxOutputAmount == -1) {
                BRReportsManager.reportBug(
                        new RuntimeException("getMaxOutputAmount is -1, meaning _wallet is NULL"),
                        true);
            }
            // max you can spend is smaller than the min you can spend
            if (maxOutputAmount == 0 || maxOutputAmount < minOutputAmount) {
                throw new InsufficientFundsException(amount, balance);
            }

            long feeForTx = m.feeForTransaction(paymentRequest.addresses[0], paymentRequest.amount);
            throw new FeeNeedsAdjust(amount, balance, feeForTx);
        }
        // payment successful
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(() -> {
            byte[] tmpTx = m.tryTransaction(paymentRequest.addresses[0], paymentRequest.amount);
            if (tmpTx == null) {
                //something went wrong, failed to create tx
                ((Activity) app).runOnUiThread(() -> BRDialog.showCustomDialog(app, "",
                        app.getString(R.string.Alerts_sendFailure),
                        app.getString(R.string.AccessibilityLabels_close), null,
                        brDialogView -> brDialogView.dismiss(), null, null, 0));
                return;
            }
            paymentRequest.serializedTx = tmpTx;
            confirmPay(app, paymentRequest);
        });

    }

    private void showAdjustFee(final Activity app, PaymentItem item) {
        BRWalletManager m = BRWalletManager.getInstance();
        long maxAmountDouble = m.getMaxOutputAmount();
        if (maxAmountDouble == -1) {
            BRReportsManager.reportBug(
                    new RuntimeException("getMaxOutputAmount is -1, meaning _wallet is NULL"));
            return;
        }
        if (maxAmountDouble == 0) {
            BRDialog.showCustomDialog(app, app.getString(R.string.Alerts_sendFailure),
                    app.getString(R.string.insufficient_fee), app.getString(R.string.Button_ok),
                    null,
                    brDialogView -> brDialogView.dismissWithAnimation(), null, null, 0);
        } else {
            BRDialog.showCustomDialog(app, app.getString(R.string.Alerts_sendFailure),
                    app.getString(R.string.insufficient_fee), app.getString(R.string.Button_ok),
                    null,
                    brDialogView -> brDialogView.dismissWithAnimation(), null, null, 0);
            //todo fix this fee adjustment
        }
    }

    public void confirmPay(final AppCompatActivity ctx, final PaymentItem request) {
        String message = createConfirmation(ctx, request);

        double minOutput;
        if (request.isAmountRequested) {
            minOutput = BRWalletManager.getInstance().getMinOutputAmountRequested();
        } else {
            minOutput = BRWalletManager.getInstance().getMinOutputAmount();
        }

        //amount can't be less than the min
        if (request.amount < minOutput) {
            final String bitcoinMinMessage = String.format(Locale.getDefault(),
                    ctx.getString(R.string.PaymentProtocol_Errors_smallTransaction),
                    BRConstants.bitcoinLowercase + new BigDecimal(minOutput).divide(
                            new BigDecimal("100")));


            ctx.runOnUiThread(() -> BRDialog.showCustomDialog(ctx, ctx.getString(R.string.Alerts_sendFailure),
                    bitcoinMinMessage, ctx.getString(R.string.AccessibilityLabels_close),
                    null,
                    brDialogView -> brDialogView.dismiss(), null, null, 0));
            return;
        }
        String selectedIso = BRSharedPrefs.getPreferredBTC(ctx) ? "NOR" : BRSharedPrefs.getIso(ctx);
        long limit = BRExchange.getSatoshisFromAmount(ctx, selectedIso, new BigDecimal(BRKeyStore.getSpendLimit(ctx))).longValue();
        long requestAmount = request.amount;

        boolean fingerprintEnabled = (
                AuthManager.isFingerPrintAvailableAndSetup(ctx) && limit == 0) || (AuthManager.isFingerPrintAvailableAndSetup(ctx) && requestAmount < limit);
        //successfully created the transaction, authenticate user
        AuthManager.getInstance().authPrompt(ctx, "", message, fingerprintEnabled, new BRAuthCompletion.AuthType(request));
    }

    public String createConfirmation(Context ctx, PaymentItem request) {
        String receiver = getReceiver(request);

        String iso = BRSharedPrefs.getIso(ctx);

        BRWalletManager m = BRWalletManager.getInstance();
        long feeForTx = m.feeForTransaction(request.addresses[0], request.amount);
        if (feeForTx == 0) {
            long maxAmount = m.getMaxOutputAmount();
            if (maxAmount == -1) {
                BRReportsManager.reportBug(
                        new RuntimeException("getMaxOutputAmount is -1, meaning _wallet is NULL"),
                        true);
            }
            if (maxAmount == 0) {
                BRDialog.showCustomDialog(ctx, "", ctx.getString(R.string.Alerts_sendFailure),
                        ctx.getString(R.string.AccessibilityLabels_close), null,
                        brDialogView -> brDialogView.dismiss(), null, null, 0);

                return null;
            }
            feeForTx = m.feeForTransaction(request.addresses[0], maxAmount);
            feeForTx += (BRWalletManager.getInstance().getBalance(ctx) - request.amount) % 100;
        }
        final long total = request.amount + feeForTx;
        String formattedAmountBTC = BRCurrency.getFormattedCurrencyString(ctx, "NOR",
                BRExchange.getBitcoinForSatoshis(ctx, new BigDecimal(request.amount)));
        String formattedFeeBTC = BRCurrency.getFormattedCurrencyString(ctx, "NOR",
                BRExchange.getBitcoinForSatoshis(ctx, new BigDecimal(feeForTx)));
        String formattedTotalBTC = BRCurrency.getFormattedCurrencyString(ctx, "NOR",
                BRExchange.getBitcoinForSatoshis(ctx, new BigDecimal(total)));

        String formattedAmount = BRCurrency.getFormattedCurrencyString(ctx, iso,
                BRExchange.getAmountFromSatoshis(ctx, iso, new BigDecimal(request.amount)));
        String formattedFee = BRCurrency.getFormattedCurrencyString(ctx, iso,
                BRExchange.getAmountFromSatoshis(ctx, iso, new BigDecimal(feeForTx)));
        String formattedTotal = BRCurrency.getFormattedCurrencyString(ctx, iso,
                BRExchange.getAmountFromSatoshis(ctx, iso, new BigDecimal(total)));

        //formatted text
        return  ctx.getString(R.string.Confirmation_amountLabel) + " " + formattedAmountBTC + " ("
                + formattedAmount + ")"
                + "\n" + ctx.getString(R.string.Confirmation_feeLabel) + " " + formattedFeeBTC
                + " (" + formattedFee + ")"
                + "\n" + ctx.getString(R.string.Confirmation_totalLabel) + " " + formattedTotalBTC
                + " (" + formattedTotal + ")\n"
                + receiver + "\n"
                + (request.comment == null ? "" : "\n" + request.comment);
    }

    public String getReceiver(PaymentItem item) {
        String receiver;
        boolean certified = false;
        if (item.cn != null && item.cn.length() != 0) {
            certified = true;
        }
        StringBuilder allAddresses = new StringBuilder();
        for (String s : item.addresses) {
            allAddresses.append(s + " ");
        }
        receiver = allAddresses.toString();
        allAddresses.delete(allAddresses.length() - 2, allAddresses.length());
        if (certified) {
            receiver = "certified: " + item.cn + "\n";
        }
        return receiver;
    }

    public boolean isSmallerThanMin(Context app, PaymentItem paymentRequest) {
        long minAmount = BRWalletManager.getInstance().getMinOutputAmount();
        return paymentRequest.amount < minAmount;
    }

    public boolean isLargerThanBalance(Context app, PaymentItem paymentRequest) {
        return paymentRequest.amount > BRWalletManager.getInstance().getBalance(app)
                && paymentRequest.amount > 0;
    }

    public boolean notEnoughForFee(Context app, PaymentItem paymentRequest) {
        BRWalletManager m = BRWalletManager.getInstance();
        long feeForTx = m.feeForTransaction(paymentRequest.addresses[0], paymentRequest.amount);
        if (feeForTx == 0) {
            long maxOutput = m.getMaxOutputAmount();
            feeForTx = m.feeForTransaction(paymentRequest.addresses[0], maxOutput);
            return feeForTx != 0;
        }
        return false;
    }

    public static void showSpendNotAllowed(final Context app) {
        Log.d(TAG, "showSpendNotAllowed");
        ((Activity) app).runOnUiThread(
                () -> BRDialog.showCustomDialog(app, app.getString(R.string.Alert_error),
                        app.getString(R.string.Send_isRescanning),
                        app.getString(R.string.Button_ok), null,
                        brDialogView -> brDialogView.dismissWithAnimation(), null, null, 0));
    }

    private class InsufficientFundsException extends Exception {
        public InsufficientFundsException(long amount, long balance) {
            super("Balance: " + balance + " satoshis, amount: " + amount + " satoshis.");
        }
    }

    private class AmountSmallerThanMinException extends Exception {
        public AmountSmallerThanMinException(long amount, long min) {
            super("Min: " + min + " satoshis, amount: " + amount + " satoshis.");
        }
    }

    private class SpendingNotAllowed extends Exception {
        public SpendingNotAllowed() {
            super("spending is not allowed at the moment");
        }
    }

    private class FeeNeedsAdjust extends Exception {
        public FeeNeedsAdjust(long amount, long balance, long fee) {
            super("Balance: " + balance + " satoshis, amount: " + amount + " satoshis, fee: " + fee
                    + " satoshis.");
        }
    }

    private class FeeOutOfDate extends Exception {
        public FeeOutOfDate(long timestamp, long now) {
            super("FeeOutOfDate: timestamp: " + timestamp + ",now: " + now);
        }
    }

    private class SomethingWentWrong extends Exception {
        public SomethingWentWrong(String mess) {
            super(mess);
        }
    }
}