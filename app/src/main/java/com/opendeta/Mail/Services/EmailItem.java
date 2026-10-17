package com.opendeta.Mail.Services;

import org.json.JSONException;
import org.json.JSONObject;

/** Simple model for one email message shown in the inbox. */
public class EmailItem {
    public String uid = "";
    public String from = "";
    public String subject = "";
    public String date = "";
    public String body = "";
    public long timestamp = 0L;

    public EmailItem() {
    }

    public EmailItem(String uid, String from, String subject, String date, String body, long timestamp) {
        this.uid = uid;
        this.from = from;
        this.subject = subject;
        this.date = date;
        this.body = body;
        this.timestamp = timestamp;
    }

    public String getSnippet() {
        if (body == null) return "";
        String text = body.replaceAll("(?s)<style.*?</style>", " ")
                .replaceAll("(?s)<script.*?</script>", " ")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("\\s+", " ").trim();
        return text.length() > 90 ? text.substring(0, 90) + "..." : text;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("uid", uid);
        o.put("from", from);
        o.put("subject", subject);
        o.put("date", date);
        o.put("body", body);
        o.put("timestamp", timestamp);
        return o;
    }

    public static EmailItem fromJson(JSONObject o) {
        EmailItem e = new EmailItem();
        e.uid = o.optString("uid", "");
        e.from = o.optString("from", "");
        e.subject = o.optString("subject", "");
        e.date = o.optString("date", "");
        e.body = o.optString("body", "");
        e.timestamp = o.optLong("timestamp", 0L);
        return e;
    }
}
