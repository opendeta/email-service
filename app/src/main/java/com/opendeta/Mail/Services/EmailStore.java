package com.opendeta.Mail.Services;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persistent cache of the inbox inside SharedPreferences.
 * Once emails are loaded they are stored here so the app never has to
 * re-download the same messages again on the next launch.
 */
public class EmailStore {

    private static final String PREF = "MailCache";
    private static final String KEY = "inbox_json";

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static void save(Context ctx, List<EmailItem> items) {
        JSONArray arr = new JSONArray();
        try {
            for (EmailItem e : items) {
                arr.put(e.toJson());
            }
            prefs(ctx).edit().putString(KEY, arr.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public static List<EmailItem> load(Context ctx) {
        List<EmailItem> out = new ArrayList<>();
        String raw = prefs(ctx).getString(KEY, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                out.add(EmailItem.fromJson(arr.getJSONObject(i)));
            }
        } catch (Exception ignored) {
        }
        sortNewestFirst(out);
        return out;
    }

    public static boolean isEmpty(Context ctx) {
        return load(ctx).isEmpty();
    }

    /**
     * Merge freshly fetched emails with the cached ones (de-duplicated by uid),
     * persist the result and return the merged, newest-first list.
     */
    public static List<EmailItem> mergeAndSave(Context ctx, List<EmailItem> fetched) {
        Map<String, EmailItem> map = new LinkedHashMap<>();
        for (EmailItem e : load(ctx)) {
            map.put(e.uid, e);
        }
        for (EmailItem e : fetched) {
            map.put(e.uid, e);
        }
        List<EmailItem> merged = new ArrayList<>(map.values());
        sortNewestFirst(merged);
        save(ctx, merged);
        return merged;
    }

    private static void sortNewestFirst(List<EmailItem> list) {
        Collections.sort(list, new Comparator<EmailItem>() {
            @Override
            public int compare(EmailItem a, EmailItem b) {
                return Long.compare(b.timestamp, a.timestamp);
            }
        });
    }
}
