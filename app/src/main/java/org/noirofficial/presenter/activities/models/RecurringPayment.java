package org.noirofficial.presenter.activities.models;

import com.orm.SugarRecord;

import java.util.Calendar;
import java.util.Date;

import org.noirofficial.R;
import org.noirofficial.presenter.activities.RecurringPaymentsActivity;
import org.noirofficial.presenter.adapter.LayoutBinding;

public class RecurringPayment extends SugarRecord implements LayoutBinding {

    public String address;
    public String amount;
    public String recurrence;
    public long nextScheduledRunTime;
    public String label;
    public int jobId;

    public RecurringPayment() {
    }

    public RecurringPayment(String address, String amount, String recurrence, String label) {
        this.address = address;
        this.amount = amount;
        this.recurrence = recurrence;
        this.label = label;
    }

    @Override
    public int getLayoutId() {
        return R.layout.recurring_payment;
    }

    public void updateNextScheduledRunTime() {
        switch (RecurringPaymentsActivity.Schedule.valueOf(recurrence)) {
            default:
            case DAILY: {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                Date date = calendar.getTime();
                nextScheduledRunTime = date.getTime();
            }
            case WEEKLY: {
                Calendar now = Calendar.getInstance();
                int weekday = now.get(Calendar.DAY_OF_WEEK);
                if (weekday != Calendar.MONDAY) {
                    int days = (Calendar.SATURDAY - weekday + 2) % 7;
                    now.add(Calendar.DAY_OF_YEAR, days);
                }
                Date date = now.getTime();
                nextScheduledRunTime = date.getTime();
            }
            case MONTHLY: {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.MONTH, 1);
                cal.set(Calendar.DAY_OF_MONTH, 1);
                Date date = cal.getTime();
                nextScheduledRunTime = date.getTime();
            }
        }
    }
}
