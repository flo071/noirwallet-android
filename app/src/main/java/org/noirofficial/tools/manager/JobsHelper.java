package org.noirofficial.tools.manager;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.support.PersistableBundleCompat;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.noirofficial.R;
import org.noirofficial.presenter.activities.LoginActivity;
import org.noirofficial.presenter.activities.models.RecurringPayment;
import org.noirofficial.wallet.BRWalletManager;

public class JobsHelper {
    private static final long SYNC_PERIOD = TimeUnit.HOURS.toMillis(24);
    private static Executor executor = Executors.newSingleThreadExecutor();

    public static class NoirWalletJobCreator implements JobCreator {

        @Override
        @Nullable
        public Job create(@NonNull String tag) {
            switch (tag) {
                case SyncBlockchainJob.TAG:
                    return new SyncBlockchainJob();
                case RecurringPaymentJob.TAG:
                    return new RecurringPaymentJob();
                default:
                    return null;
            }
        }
    }

    public static class SyncBlockchainJob extends Job {

        public static final String TAG = "sync_blockchain_job";

        @Override
        @NonNull
        protected Result onRunJob(Params params) {
            BRWalletManager.getInstance().init();
            return Result.SUCCESS;
        }

        public static void scheduleJob() {
            JobManager.instance().cancelAllForTag(SyncBlockchainJob.TAG);
            new JobRequest.Builder(SyncBlockchainJob.TAG)
                    .setPeriodic(SYNC_PERIOD).setRequiredNetworkType(
                    JobRequest.NetworkType.UNMETERED).setRequiresCharging(true)
                    .build()
                    .schedule();
        }
    }

    public static class RecurringPaymentJob extends Job {

        public static final String TAG = "recurring_payment_blockchain_job";

        @Override
        @NonNull
        protected Result onRunJob(Params params) {
            RecurringPayment recurringPayment = RecurringPayment.findById(RecurringPayment.class, params.getExtras().getLong("id", -1));
            recurringPayment.updateNextScheduledRunTime();
            executor.execute(recurringPayment::save);
            NotificationManager notificationManager =
                    (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel channel = new NotificationChannel("recurring", "Recurring Payments", NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription("Recurring Payments Notifications");
                notificationManager.createNotificationChannel(channel);
            }
            NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext(), "recurring");
            builder.setSmallIcon(R.mipmap.ic_launcher);
            builder.setAutoCancel(true);
            builder.setTicker(String.format(getContext().getString(R.string.recurring_payments_title), recurringPayment.label));
            builder.setContentTitle(String.format(getContext().getString(R.string.recurring_payments_title), recurringPayment.label));
            builder.setContentText(recurringPayment.address + ", " + recurringPayment.amount + ", " + recurringPayment.recurrence);
            Intent intent = new Intent(getContext(), LoginActivity.class);
            Uri uri = Uri.parse("noir://" + recurringPayment.address + "?amount=" + recurringPayment.amount);
            intent.setData(uri);
            PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), recurringPayment.getId().intValue(), intent, PendingIntent.FLAG_CANCEL_CURRENT);
            builder.setContentIntent(pendingIntent);
            notificationManager.notify(recurringPayment.getId().intValue(), builder.build());
            return Result.SUCCESS;
        }
    }

    public static void sendRecurringPaymentSampleNotification(Context context, RecurringPayment recurringPayment) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel("recurring", "Recurring Payments", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Recurring Payments Notifications");
            notificationManager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "recurring");
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setAutoCancel(true);
        builder.setTicker(String.format(context.getString(R.string.recurring_payments_sample), recurringPayment.label));
        builder.setContentTitle(String.format(context.getString(R.string.recurring_payments_sample), recurringPayment.label));
        builder.setContentText(recurringPayment.address + ", " + recurringPayment.amount + ", " + recurringPayment.recurrence);
        Intent intent = new Intent(context, LoginActivity.class);
        Uri uri = Uri.parse("noir://" + recurringPayment.address + "?amount=" + recurringPayment.amount);
        intent.setData(uri);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, recurringPayment.getId().intValue(), intent, PendingIntent.FLAG_CANCEL_CURRENT);
        builder.setContentIntent(pendingIntent);
        notificationManager.notify(recurringPayment.getId().intValue(), builder.build());
    }

    public static void cancelRecurringPayment(RecurringPayment recurringPayment) {
        JobManager.instance().cancel(recurringPayment.jobId);
    }

    public static void updateRecurringPaymentJobs() {
        executor.execute(() -> {
            List<RecurringPayment> recurringPaymentList = RecurringPayment.listAll(RecurringPayment.class);
            for (RecurringPayment recurringPayment : recurringPaymentList) {
                scheduleRecurringPayment(recurringPayment);
            }
        });
    }

    public static void scheduleRecurringPayment(RecurringPayment recurringPayment) {
        cancelRecurringPayment(recurringPayment);
        PersistableBundleCompat persistableBundleCompat = new PersistableBundleCompat();
        persistableBundleCompat.putLong("id", recurringPayment.getId());
        recurringPayment.jobId = new JobRequest.Builder(RecurringPaymentJob.TAG)
                .setExtras(persistableBundleCompat)
                .setPeriodic(recurringPayment.nextScheduledRunTime,
                        recurringPayment.nextScheduledRunTime - TimeUnit.HOURS.toMillis(4))
                .build()
                .schedule();
        executor.execute(() -> recurringPayment.save());
        ;
    }
}
