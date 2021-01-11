package org.noirofficial.presenter.activities.callbacks;

import org.noirofficial.presenter.activities.models.RecurringPayment;
import org.noirofficial.presenter.adapter.MultiTypeDataBoundAdapter;

public interface ActivityRecurringPaymentCallback extends MultiTypeDataBoundAdapter.ActionCallback {
    void onSetTimeClick();

    void onAddClick();

    void onRecurringPaymentClick(RecurringPayment recurringPayment);
}
