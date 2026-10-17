package com.opendeta.Mail.Services;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.opendeta.Mail.Services.databinding.ActivityEmailDetailBinding;

/**
 * Reads one email like a normal mail app (HTML rendered in a WebView).
 * The overflow menu (three dots drawn with code) offers "Show Code".
 */
public class EmailDetailActivity extends AppCompatActivity {

    private ActivityEmailDetailBinding binding;
    private String rawCode = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEmailDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String subject = getIntent().getStringExtra("subject");
        String from = getIntent().getStringExtra("from");
        String date = getIntent().getStringExtra("date");
        rawCode = getIntent().getStringExtra("body");

        binding.tvDetailSubject.setText(subject == null ? "(No Subject)" : subject);
        binding.tvDetailFrom.setText(from == null ? "" : from);
        binding.tvDetailDate.setText(date == null ? "" : date);

        binding.btnBackDetail.setOnClickListener(v -> finish());

        // ---- three-dot overflow menu -> "Show Code" ----
        binding.btnOverflow.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(EmailDetailActivity.this, v);
            popup.getMenu().add("Show Code");
            popup.setOnMenuItemClickListener(item -> {
                showCodeDialog();
                return true;
            });
            popup.show();
        });

        // ---- render the email like a standard mail app ----
        WebSettings settings = binding.wvDetail.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        String display = rawCode;
        if (display == null || display.trim().isEmpty()) {
            display = "<html><body style='font-family:sans-serif;padding:16px;color:#5F6368'>"
                    + "<p>This message has no displayable content.</p></body></html>";
        } else if (display.indexOf('<') < 0) {
            // plain text message -> wrap it so it renders nicely
            display = "<html><body><pre style=\"white-space:pre-wrap;word-wrap:break-word;"
                    + "font-family:sans-serif;font-size:15px;padding:12px;color:#202124\">"
                    + escapeHtml(display) + "</pre></body></html>";
        }
        binding.wvDetail.loadDataWithBaseURL(null, display, "text/html", "utf-8", null);
    }

    /** Dialog showing the raw email HTML code: Copy on the left, "x" on the right. */
    private void showCodeDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_email_code);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.CENTER);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.94);
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(lp);
        }

        final String code = rawCode == null ? "" : rawCode;
        android.widget.EditText et = dialog.findViewById(R.id.et_email_code);
        et.setText(code);

        dialog.findViewById(R.id.btn_copy_code).setOnClickListener(v -> {
            ClipboardManager clipboard =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("EmailHTML", code));
                Toast.makeText(getApplicationContext(), "Code Copied to Clipboard! 📋",
                        Toast.LENGTH_SHORT).show();
            }
        });

        dialog.findViewById(R.id.btn_close_code).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
