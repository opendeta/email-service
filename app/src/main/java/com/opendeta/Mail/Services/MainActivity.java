package com.opendeta.Mail.Services;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.opendeta.Mail.Services.databinding.MainBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

public class MainActivity extends AppCompatActivity {

    /** How many newest messages to pull from Gmail per load. */
    private static final int MAX_EMAILS = 50;

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
        // ---- styled sign-in button (drawn with code) ----
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF1A73E8, 0xFF0052D4});
        gradient.setCornerRadius(26f);
        binding.btnSignin.setBackground(gradient);

        // ---- inbox list ----
        adapter = new EmailAdapter();
        binding.lvEmails.setAdapter(adapter);
        binding.lvEmails.setOnItemClickListener((parent, view, position, id) ->
                openEmail(emails.get(position)));

        // ---- password hide / show ----
        binding.cbHidePassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.etLoginPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            } else {
                binding.etLoginPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            }
            binding.etLoginPassword.setSelection(binding.etLoginPassword.getText().length());
        });

        // ---- learn more ----
        binding.tvLearnMore.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://myaccount.google.com/apppasswords")));
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Oops, An Error Occured", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnSignin.setOnClickListener(v -> doSignin());

        // ---- Compose (right side) ----
        GradientDrawable fabBg = new GradientDrawable();
        fabBg.setShape(GradientDrawable.OVAL);
        fabBg.setColor(0xFF1A73E8);
        binding.fabCompose.setBackground(fabBg);

        binding.fabCompose.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ComposeActivity.class)));

        // ---- pull down to refresh ----
        binding.swRefresh.setOnRefreshListener(this::fetchInbox);
        binding.swRefresh.setColorSchemeColors(0xFF1A73E8);

        // ---- splash, then choose screen ----
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

    // ------------------------------------------------------------ sign in

    private void doSignin() {
        final String gmail = binding.etLoginGmail.getText().toString().trim();
        final String password = binding.etLoginPassword.getText().toString().trim();

        if (gmail.isEmpty() || password.isEmpty()) {
            Toast.makeText(getApplicationContext(), "Please fill all fields!", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSignin.setText("");
        binding.btnSignin.setEnabled(false);
        binding.pbSignin.setVisibility(View.VISIBLE);

        new Thread(() -> {
            String error = null;
            try {
                Properties props = new Properties();
                props.put("mail.store.protocol", "imaps");
                props.put("mail.imaps.host", ImapFetcher.HOST);
                props.put("mail.imaps.port", String.valueOf(ImapFetcher.PORT));
                props.put("mail.imaps.ssl.enable", "true");

                javax.mail.Session session = javax.mail.Session.getInstance(props, null);
                javax.mail.Store store = session.getStore("imaps");
                store.connect(ImapFetcher.HOST, gmail, password);
                store.close();
            } catch (Exception e) {
                error = e.getMessage() != null ? e.getMessage() : "Authentication / Network Failed";
            }

            final String actualError = error;
            runOnUiThread(() -> {
                binding.pbSignin.setVisibility(View.GONE);
                if (actualError == null) {
                    sp.edit()
                            .putString("saved_gmail", gmail)
                            .putString("saved_password", password)
                            .apply();
                    binding.layoutSignin.setVisibility(View.GONE);
                    showInbox(gmail);
                    loadCachedInbox();
                } else {
                    binding.btnSignin.setText("Sign-in");
                    binding.btnSignin.setEnabled(true);
                    Toast.makeText(getApplicationContext(),
                            "Login Error: " + actualError, Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    // -------------------------------------------------------------- inbox

    private void showInbox(String gmail) {
        binding.tvUserEmail.setText("To: " + gmail);
        binding.layoutInbox.setVisibility(View.VISIBLE);
        updateEmptyState();
    }

    /** Shows the SharedPreferences cache instantly (no network). */
    private void loadCachedInbox() {
        List<EmailItem> cached = EmailStore.load(this);
        emails.clear();
        emails.addAll(cached);
        adapter.notifyDataSetChanged();
        updateEmptyState();

        // nothing cached yet -> fetch once
        if (emails.isEmpty()) {
            fetchInbox();
        }
    }

    /** Pulls the newest emails over IMAP and streams them into the list one by one. */
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
        binding.pbLoading.setVisibility(View.VISIBLE);

        new Thread(() -> ImapFetcher.fetchStream(gmail, password, MAX_EMAILS, new ImapFetcher.Listener() {

            @Override
            public void onEmail(final EmailItem item) {
                runOnUiThread(() -> {
                    binding.pbLoading.setVisibility(View.GONE);
                    mergeEmail(item);
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                });
            }

            @Override
            public void onComplete(int count) {
                // persist the merged inbox so it never has to be downloaded again
                EmailStore.save(MainActivity.this, emails);
                runOnUiThread(() -> {
                    binding.pbLoading.setVisibility(View.GONE);
                    binding.swRefresh.setRefreshing(false);
                    loading = false;
                });
            }

            @Override
            public void onError(final Exception e) {
                runOnUiThread(() -> {
                    binding.pbLoading.setVisibility(View.GONE);
                    binding.swRefresh.setRefreshing(false);
                    loading = false;
                    String msg = e.getMessage() != null ? e.getMessage() : "Unknown Stream Error";
                    Toast.makeText(getApplicationContext(), "Stream Error: " + msg, Toast.LENGTH_LONG).show();
                });
            }
        })).start();
    }

    private void mergeEmail(EmailItem item) {
        for (int i = 0; i < emails.size(); i++) {
            if (emails.get(i).uid.equals(item.uid)) {
                emails.set(i, item);
                sortNewestFirst();
                return;
            }
        }
        emails.add(item);
        sortNewestFirst();
    }

    private void sortNewestFirst() {
        Collections.sort(emails, new Comparator<EmailItem>() {
            @Override
            public int compare(EmailItem a, EmailItem b) {
                return Long.compare(b.timestamp, a.timestamp);
            }
        });
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

    // ------------------------------------------------------------ adapter

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
            TextView to = convertView.findViewById(R.id.tv_row_to);
            TextView subject = convertView.findViewById(R.id.tv_row_subject);
            TextView date = convertView.findViewById(R.id.tv_row_date);

            to.setText("To: " + (item.to == null || item.to.isEmpty() ? "" : item.to));
            subject.setText(item.getShortSubject());
            date.setText(item.date);
            return convertView;
        }
    }
}
