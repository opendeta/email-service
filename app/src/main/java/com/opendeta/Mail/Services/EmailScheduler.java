package com.opendeta.Mail.Services;

import android.content.Context;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/**
 * Background automated email scheduler.
 *
 * Schedules an email to be delivered later by the WorkManager based
 * {@link SendEmailWorker}. WorkManager persists the request, so the email
 * will still be sent even if the app process is killed or the device reboots.
 */
public class EmailScheduler {

    private static final String WORK_NAME_PREFIX = "scheduled_email_";

    public static void schedule(Context ctx, String to, String subject, String html, long delayMillis) {
        Data data = new Data.Builder()
                .putString(SendEmailWorker.KEY_TO, to)
                .putString(SendEmailWorker.KEY_SUBJECT, subject)
                .putString(SendEmailWorker.KEY_HTML, html)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SendEmailWorker.class)
                .setInputData(data)
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .addTag("opendeta_email")
                .build();

        WorkManager.getInstance(ctx).enqueueUniqueWork(
                WORK_NAME_PREFIX + System.currentTimeMillis(),
                ExistingWorkPolicy.REPLACE,
                request);
    }

    /** Schedules an email using a delay expressed in minutes. */
    public static void scheduleInMinutes(Context ctx, String to, String subject, String html, int minutes) {
        long delay = Math.max(1, minutes) * 60_000L;
        schedule(ctx, to, subject, html, delay);
    }
}
