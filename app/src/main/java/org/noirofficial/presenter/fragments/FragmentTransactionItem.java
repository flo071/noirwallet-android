package org.noirofficial.presenter.fragments;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.noirofficial.NoirWallet;
import org.noirofficial.R;
import org.noirofficial.databinding.TransactionDetailsItemBinding;
import org.noirofficial.presenter.entities.TxItem;
import org.noirofficial.presenter.fragments.interfaces.TransactionDetailsCallback;
import org.noirofficial.presenter.fragments.models.TransactionDetailsViewModel;
import org.noirofficial.tools.manager.BRClipboardManager;
import org.noirofficial.tools.manager.BRSharedPrefs;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 6/29/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class FragmentTransactionItem extends Fragment {
    private static final String TRANSACTION_ITEM = "FragmentTransactionItem:TransactionItem";

    private TransactionDetailsViewModel viewModel;

    private TransactionDetailsCallback callback = new TransactionDetailsCallback() {
        @Override
        public void onBackgroundClick() {
            FragmentTransactionDetails fragment =
                    (FragmentTransactionDetails) getActivity().getSupportFragmentManager()
                            .findFragmentByTag(
                                    FragmentTransactionDetails.class.getName());
            fragment.onBackPressed();
        }

        @Override
        public void onAddressClick() {
            //BRClipboardManager.putClipboard(getContext(), viewModel.getAddress());
            //Toast.makeText(getContext(), R.string.Receive_copied, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onTransactionIDClick() {
            BRClipboardManager.putClipboard(getContext(), viewModel.getTransactionID());
            Toast.makeText(getContext(), R.string.Receive_copied, Toast.LENGTH_SHORT).show();
        }
    };

    public static FragmentTransactionItem newInstance(TxItem item) {
        FragmentTransactionItem fragmentTransactionItem = new FragmentTransactionItem();
        Bundle bundle = new Bundle();
        bundle.putParcelable(TRANSACTION_ITEM, item);
        fragmentTransactionItem.setArguments(bundle);
        return fragmentTransactionItem;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        viewModel = new TransactionDetailsViewModel(getArguments().getParcelable(TRANSACTION_ITEM));
        TransactionDetailsItemBinding binding = TransactionDetailsItemBinding.inflate(inflater);
        binding.setData(viewModel);
        binding.setCallback(callback);
        binding.originalFiatAmountText.setSelected(true);
        binding.currentFiatAmountText.setSelected(true);
        binding.cryptoAmountText.setSelected(true);
        binding.amountSwitcher.setDisplayedChild(
                (BRSharedPrefs.getPreferredBTC(NoirWallet.getContext())
                        || viewModel.currentFiatAmountEqualsOriginalFiatAmount() && !BRSharedPrefs.getPreferredBTC(
                        NoirWallet.getContext())) ? 0 : 1);
        return binding.getRoot();
    }
}