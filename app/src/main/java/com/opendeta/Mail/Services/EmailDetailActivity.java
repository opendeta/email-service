package com.opendeta.Mail.Services;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;

import com.opendeta.Mail.Services.databinding.ActivityEmailDetailBinding;

/** Shows a single email rendered inside a WebView, just like a normal mail app. */
public class EmailDetailActivity extends AppCompatActivity {

    private ActivityEmailDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEmailDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String subject = getIntent().getStringExtra("subject");
        String from = getIntent().getStringExtra("from");
        String date = getIntent().getStringExtra("date");
        String body = getIntent().getStringExtra("body");

        binding.tvDetailSubject.setText(subject == null ? "(no subject)" : subject);
        binding.tvDetailFrom.setText(from == null ? "" : from);
        binding.tvDetailDate.setText(date == null ? "" : date);

        binding.btnBackDetail.setOnClickListener(v -> finish());

        WebSettings settings = binding.wvDetail.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        if (body == null || body.trim().isEmpty()) {
            body = "<html><body style='font-family:sans-serif;padding:16px;color:#5F6368'>"
                    + "<p>This message has no displayable content.</p></body></html>";
        }
        binding.wvDetail.loadDataWithBaseURL(null, body, "text/html", "utf-8", null);
    }
}
