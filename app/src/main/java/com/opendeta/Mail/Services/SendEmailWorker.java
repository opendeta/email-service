package com.opendeta.Mail.Services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/** Background worker that delivers a scheduled email through Gmail SMTP. */
public class SendEmailWorker extends Worker {

    public static final String KEY_TO = "to";
    public static final String KEY_SUBJECT = "subject";
    public static final String KEY_HTML = "html";

    private static final String CHANNEL_ID = "opendeta_mail_scheduler";

    public SendEmailWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();
        SharedPreferences sp = ctx.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);

        String from = sp.getString("saved_gmail", "");
        String password = sp.getString("saved_password", "");

        String to = getInputData().getString(KEY_TO);
        String subject = getInputData().getString(KEY_SUBJECT);
        String html = getInputData().getString(KEY_HTML);

        if (from.isEmpty() || password.isEmpty() || to == null || to.isEmpty()) {
            return Result.failure();
        }

        try {
            MailSender.send(from, password, to, subject == null ? "" : subject,
                    html == null ? "" : html);
            notify(ctx, subject, to, true);
            return Result.success();
        } catch (Exception e) {
            notify(ctx, subject, to, false);
            return Result.retry();
        }
    }

    private void notify(Context ctx, String subject, String to, boolean ok) {
        try {
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID, "Scheduled Emails", NotificationManager.IMPORTANCE_DEFAULT);
                nm.createNotificationChannel(channel);
            }

            String text = (ok ? "Scheduled email sent to " : "Failed to send email to ") + to;
            NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_email)
                    .setContentTitle(subject == null || subject.isEmpty() ? "OpenDeta Mail" : subject)
                    .setContentText(text)
                    .setAutoCancel(true);

            nm.notify((int) (System.currentTimeMillis() % 100000), builder.build());
        } catch (Exception ignored) {
        }
    }
}
