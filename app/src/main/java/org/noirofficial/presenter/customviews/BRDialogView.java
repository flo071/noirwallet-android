package org.noirofficial.presenter.customviews;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;

import org.noirofficial.R;
import org.noirofficial.tools.manager.BRReportsManager;
import org.noirofficial.tools.util.Utils;

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
public class BRDialogView extends DialogFragment {

    private static final String TAG = BRDialogView.class.getName();

    private String title = "";
    private String message = "";
    private String posButton = "";
    private String negButton = "";
    private BRDialogView.BROnClickListener posListener;
    private BRDialogView.BROnClickListener negListener;
    private DialogInterface.OnDismissListener dismissListener;
    private Button negativeButton;
    private LinearLayout buttonsLayout;
    private SpannableString spanMessage;


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.bread_alert_dialog, null);
        TextView titleText = view.findViewById(R.id.dialog_title);
        TextView messageText = view.findViewById(R.id.dialog_text);
        Button positiveButton = view.findViewById(R.id.pos_button);
        negativeButton = view.findViewById(R.id.neg_button);
        buttonsLayout = view.findViewById(R.id.linearLayout3);

        titleText.setText(title);
        messageText.setText(message);
        if (spanMessage != null) {
            messageText.setText(spanMessage);
            messageText.setMovementMethod(LinkMovementMethod.getInstance());
        }
        positiveButton.setText(posButton);
        positiveButton.setOnClickListener(v -> {
            if (posListener != null)
                posListener.onClick(BRDialogView.this);
        });
        if (Utils.isNullOrEmpty(negButton)) {
            Log.e(TAG, "onCreateDialog: removing negative button");
            buttonsLayout.removeView(negativeButton);
            buttonsLayout.requestLayout();
        }
        negativeButton.setText(negButton);
        negativeButton.setOnClickListener(v -> {
            if (negListener != null)
                negListener.onClick(BRDialogView.this);
        });
        builder.setView(view);
        Dialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        return dialog;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //Because the callback is passed un-optimally, it's not view recreation safe especially
        //after activity recreation restoring from an activity saved state. Dismiss the dialog and
        //the user can just reinvoke it again by initiating the desired action
        try {
            dismissAllowingStateLoss();
        } catch (IllegalStateException e) {
            Crashlytics.logException(e);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (dismissListener != null)
            dismissListener.onDismiss(dialog);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setSpan(SpannableString message) {
        if (message == null) {
            BRReportsManager.reportBug(new NullPointerException("setSpan with null message"));
            return;
        }
        this.spanMessage = message;
    }

    public void setPosButton(@NonNull String posButton) {
        this.posButton = posButton;
    }

    public void setNegButton(String negButton) {
        this.negButton = negButton;
    }

    public void setPosListener(BRDialogView.BROnClickListener posListener) {
        this.posListener = posListener;
    }

    public void setNegListener(BRDialogView.BROnClickListener negListener) {
        this.negListener = negListener;
    }

    public void setDismissListener(DialogInterface.OnDismissListener dismissListener) {
        this.dismissListener = dismissListener;
    }

    public interface BROnClickListener {
        void onClick(BRDialogView brDialogView);
    }

    public void dismissWithAnimation() {
        try {
            dismissAllowingStateLoss();
        } catch(IllegalArgumentException e) {
            Crashlytics.logException(e);
        }
    }
}
