package com.opendeta.Mail.Services;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.opendeta.Mail.Services.databinding.MainBinding;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private MainBinding binding;
    private SharedPreferences sp;

    private final List<EmailItem> emails = new ArrayList<>();
    private EmailAdapter adapter;
    private volatile boolean loading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = MainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        sp = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        initializeLogic();
    }

    private void initializeLogic() {
        // Gradient buttons (brand look)
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF1A73E8, 0xFF0052D4});
        gradient.setCornerRadius(26f);
        binding.btnSignin.setBackground(gradient);

        // Inbox list
        adapter = new EmailAdapter();
        binding.lvEmails.setAdapter(adapter);
        binding.lvEmails.setOnItemClickListener((parent, view, position, id) -> openEmail(emails.get(position)));

        // Password hide / show
        binding.cbHidePassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.etLoginPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            } else {
                binding.etLoginPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            }
            binding.etLoginPassword.setSelection(binding.etLoginPassword.getText().length());
        });

        // User guide
        binding.tvLearnMore.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://opendeta.github.io/userguide/")));
            } catch (Exception ignored) {
            }
        });

        binding.btnSignin.setOnClickListener(v -> doSignin());

        // Compose button (right side)
        binding.fabCompose.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ComposeActivity.class)));

        // Pull-down to refresh
        binding.swRefresh.setOnRefreshListener(this::fetchInbox);
        binding.swRefresh.setColorSchemeColors(0xFF1A73E8);

        // Splash then decide which screen to show
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            binding.layoutSplash.setVisibility(View.GONE);
            String savedGmail = sp.getString("saved_gmail", "");
            String savedPassword = sp.getString("saved_password", "");

            if (!savedGmail.isEmpty() && !savedPassword.isEmpty()) {
                showInbox(savedGmail);
                loadCachedInbox();
            } else {
                binding.layoutSignin.setVisibility(View.VISIBLE);
            }
        }, 1500);
    }

    // ---------------------------------------------------------------- sign-in

    private void doSignin() {
        final String gmail = binding.etLoginGmail.getText().toString().trim();
        final String password = binding.etLoginPassword.getText().toString().trim();

        if (gmail.isEmpty() || password.isEmpty()) {
            Toast.makeText(getApplicationContext(),
                    "Please Enter a Gmail & App Password First.", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSignin.setText("");
        binding.btnSignin.setEnabled(false);
        binding.pbSignin.setVisibility(View.VISIBLE);

        new Thread(() -> {
            final boolean ok = MailSender.verify(gmail, password);

            runOnUiThread(() -> {
                binding.pbSignin.setVisibility(View.GONE);
                binding.btnSignin.setText("Sign-in");
                binding.btnSignin.setEnabled(true);

                if (ok) {
                    sp.edit()
                            .putString("saved_gmail", gmail)
                            .putString("saved_password", password)
                            .apply();
                    binding.layoutSignin.setVisibility(View.GONE);
                    showInbox(gmail);
                    loadCachedInbox();
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Please Enter a valid Gmail and its password or Learn how to Create App Password",
                            Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    // ------------------------------------------------------------------ inbox

    private void showInbox(String gmail) {
        binding.tvUserEmail.setText(gmail);
        binding.layoutInbox.setVisibility(View.VISIBLE);
        updateEmptyState();
    }

    /** Load emails from the SharedPreferences cache (no network). */
    private void loadCachedInbox() {
        List<EmailItem> cached = EmailStore.load(this);
        emails.clear();
        emails.addAll(cached);
        adapter.notifyDataSetChanged();
        updateEmptyState();

        // Very first launch for this account: nothing cached yet -> fetch once.
        if (emails.isEmpty()) {
            fetchInbox();
        }
    }

    /** Fetch the newest emails from Gmail over IMAP and merge them into the cache. */
    private void fetchInbox() {
        if (loading) {
            binding.swRefresh.setRefreshing(false);
            return;
        }
        final String gmail = sp.getString("saved_gmail", "");
        final String password = sp.getString("saved_password", "");

        if (gmail.isEmpty() || password.isEmpty()) {
            binding.swRefresh.setRefreshing(false);
            return;
        }

        loading = true;
        binding.swRefresh.setRefreshing(true);

        new Thread(() -> {
            try {
                List<EmailItem> fetched = ImapFetcher.fetch(gmail, password, 50);
                final List<EmailItem> merged = EmailStore.mergeAndSave(MainActivity.this, fetched);

                runOnUiThread(() -> {
                    emails.clear();
                    emails.addAll(merged);
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                    binding.swRefresh.setRefreshing(false);
                    loading = false;
                    Toast.makeText(getApplicationContext(),
                            "Inbox updated (" + fetched.size() + " messages)", Toast.LENGTH_SHORT).show();
                });
            } catch (final Exception e) {
                runOnUiThread(() -> {
                    binding.swRefresh.setRefreshing(false);
                    loading = false;
                    Toast.makeText(getApplicationContext(),
                            "Could not load the inbox. Check your connection / App Password.",
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void updateEmptyState() {
        binding.tvEmpty.setVisibility(emails.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openEmail(EmailItem item) {
        Intent i = new Intent(MainActivity.this, EmailDetailActivity.class);
        i.putExtra("subject", item.subject);
        i.putExtra("from", item.from);
        i.putExtra("date", item.date);
        i.putExtra("body", item.body);
        startActivity(i);
    }

    // ---------------------------------------------------------------- adapter

    private class EmailAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return emails.size();
        }

        @Override
        public Object getItem(int position) {
            return emails.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this)
                        .inflate(R.layout.row_email, parent, false);
            }
            EmailItem item = emails.get(position);
            TextView from = convertView.findViewById(R.id.tv_row_from);
            TextView date = convertView.findViewById(R.id.tv_row_date);
            TextView subject = convertView.findViewById(R.id.tv_row_subject);
            TextView snippet = convertView.findViewById(R.id.tv_row_snippet);

            from.setText(item.from);
            date.setText(item.date);
            subject.setText(item.subject);
            snippet.setText(item.getSnippet());
            return convertView;
        }
    }
}
