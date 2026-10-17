package com.opendeta.Mail.Services;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.opendeta.Mail.Services.databinding.ActivityComposeBinding;

/**
 * Compose screen: HTML email "code editor" with a Run button that renders the
 * code in a WebView dialog, plus immediate send and a background scheduler.
 */
public class ComposeActivity extends AppCompatActivity {

    private ActivityComposeBinding binding;
    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityComposeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        sp = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        styleButtons();

        binding.btnBack.setOnClickListener(v -> finish());

        // "Run" -> preview the HTML code inside a WebView dialog (above the editor)
        binding.btnRun.setOnClickListener(v -> showPreviewDialog());

        binding.btnSend.setOnClickListener(v -> sendNow());

        binding.btnSchedule.setOnClickListener(v -> scheduleSend());
    }

    /** Run / Send / Schedule buttons styled with code-built gradient drawables. */
    private void styleButtons() {
        GradientDrawable runBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF1A73E8, 0xFF0052D4});
        runBg.setCornerRadius(22f);
        binding.btnRun.setBackground(runBg);
        binding.btnRun.setTextColor(0xFFFFFFFF);

        GradientDrawable sendBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF1A73E8, 0xFF0052D4});
        sendBg.setCornerRadius(28f);
        binding.btnSend.setBackground(sendBg);
        binding.btnSend.setTextColor(0xFFFFFFFF);

        GradientDrawable scheduleBg = new GradientDrawable();
        scheduleBg.setColor(0xFF0B57D0);
        scheduleBg.setCornerRadius(22f);
        binding.btnSchedule.setBackground(scheduleBg);
        binding.btnSchedule.setTextColor(0xFFFFFFFF);
    }

    /** Renders the editor content in a WebView dialog with an "x" close button. */
    private void showPreviewDialog() {
        String html = binding.etHtml.getText().toString();
        if (html.trim().isEmpty()) {
            html = "<html><body style='font-family:sans-serif;padding:16px;color:#5F6368'>"
                    + "<h3>Nothing to preview</h3>"
                    + "<p>Type some HTML code in the editor first.</p></body></html>";
        }

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_preview);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.TOP);

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.85);
            window.setAttributes(lp);
        }

        WebView webView = dialog.findViewById(R.id.wv_preview);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);

        // "x" button closes the dialog
        dialog.findViewById(R.id.btn_close_preview).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private boolean validate(String receiver, String html) {
        if (binding.etSubject.getText().toString().trim().isEmpty()
                || receiver.isEmpty()
                || html.trim().isEmpty()) {
            Toast.makeText(getApplicationContext(), "Please fill all fields!", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void sendNow() {
        final String subject = binding.etSubject.getText().toString().trim();
        final String receiver = binding.etReceiver.getText().toString().trim();
        final String html = binding.etHtml.getText().toString();

        if (!validate(receiver, html)) {
            return;
        }

        final String from = sp.getString("saved_gmail", "");
        final String password = sp.getString("saved_password", "");

        if (from.isEmpty() || password.isEmpty()) {
            Toast.makeText(getApplicationContext(), "Please sign in first.", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSend.setEnabled(false);
        binding.btnSend.setText("Sending...");

        new Thread(() -> {
            try {
                MailSender.send(from, password, receiver, subject, html);
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), "Email Successfully Sent", Toast.LENGTH_SHORT).show();
                    binding.btnSend.setEnabled(true);
                    binding.btnSend.setText("Send");
                    binding.etSubject.setText("");
                    binding.etReceiver.setText("");
                    binding.etHtml.setText("");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(),
                            "Oops, An Error Occured! Try again.", Toast.LENGTH_LONG).show();
                    binding.btnSend.setEnabled(true);
                    binding.btnSend.setText("Send");
                });
            }
        }).start();
    }

    /** Hands the email over to the background scheduler (WorkManager). */
    private void scheduleSend() {
        final String subject = binding.etSubject.getText().toString().trim();
        final String receiver = binding.etReceiver.getText().toString().trim();
        final String html = binding.etHtml.getText().toString();

        if (!validate(receiver, html)) {
            return;
        }

        if (sp.getString("saved_gmail", "").isEmpty()) {
            Toast.makeText(getApplicationContext(), "Please sign in first.", Toast.LENGTH_SHORT).show();
            return;
        }

        int minutes = 5;
        String delayText = binding.etDelayMinutes.getText().toString().trim();
        if (!delayText.isEmpty()) {
            try {
                minutes = Integer.parseInt(delayText);
            } catch (NumberFormatException ignored) {
                minutes = 5;
            }
        }

        EmailScheduler.scheduleInMinutes(getApplicationContext(), receiver, subject, html, minutes);
        Toast.makeText(getApplicationContext(),
                "Scheduled in " + Math.max(1, minutes) + " minute(s). Will send in the background.",
                Toast.LENGTH_LONG).show();
    }
}
