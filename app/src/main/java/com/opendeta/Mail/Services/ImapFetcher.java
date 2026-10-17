package com.opendeta.Mail.Services;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;

/**
 * Fetches the newest emails of the signed-in Gmail account over IMAP
 * using the same Gmail App Password that is already stored for SMTP.
 */
public class ImapFetcher {

    public static final String HOST = "imap.gmail.com";
    public static final int PORT = 993;

    public static List<EmailItem> fetch(String user, String appPassword, int max) throws Exception {
        List<EmailItem> result = new ArrayList<>();

        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", HOST);
        props.put("mail.imaps.port", String.valueOf(PORT));
        props.put("mail.imaps.ssl.enable", "true");
        props.put("mail.imaps.ssl.protocols", "TLSv1.2");
        props.put("mail.imaps.connectiontimeout", "20000");
        props.put("mail.imaps.timeout", "30000");

        Session session = Session.getInstance(props, null);
        Store store = session.getStore("imaps");
        store.connect(HOST, user, appPassword);

        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);
        try {
            int total = inbox.getMessageCount();
            if (total <= 0) {
                return result;
            }
            int start = Math.max(1, total - max + 1);
            Message[] messages = inbox.getMessages(start, total);

            UIDFolder uidFolder = null;
            if (inbox instanceof UIDFolder) {
                uidFolder = (UIDFolder) inbox;
            }

            SimpleDateFormat fmt = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US);

            // newest first
            for (int i = messages.length - 1; i >= 0; i--) {
                Message msg = messages[i];
                EmailItem item = new EmailItem();

                String uid;
                if (uidFolder != null) {
                    uid = String.valueOf(uidFolder.getUID(msg));
                } else {
                    uid = String.valueOf(msg.getMessageNumber());
                }
                item.uid = uid;

                String from = "";
                try {
                    if (msg.getFrom() != null && msg.getFrom().length > 0) {
                        from = msg.getFrom()[0].toString();
                    }
                } catch (Exception ignored) {
                }
                item.from = from;

                String subject = msg.getSubject();
                item.subject = (subject == null || subject.trim().isEmpty()) ? "(no subject)" : subject;

                Date sent = null;
                try {
                    sent = msg.getSentDate();
                } catch (Exception ignored) {
                }
                if (sent == null) {
                    try {
                        sent = msg.getReceivedDate();
                    } catch (Exception ignored) {
                    }
                }
                item.timestamp = sent != null ? sent.getTime() : 0L;
                item.date = sent != null ? fmt.format(sent) : "";

                String html = extractHtml(msg);
                item.body = html != null ? html : "";

                result.add(item);
            }
        } finally {
            try {
                inbox.close(false);
            } catch (Exception ignored) {
            }
            try {
                store.close();
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    /** Prefer the HTML part, fall back to a wrapped plain-text body. */
    private static String extractHtml(Part part) throws Exception {
        if (part.isMimeType("text/html")) {
            Object content = part.getContent();
            if (content instanceof String) {
                return (String) content;
            }
        }
        if (part.isMimeType("text/plain")) {
            Object content = part.getContent();
            if (content instanceof String) {
                return "<html><body><pre style=\"white-space:pre-wrap;word-wrap:break-word;"
                        + "font-family:sans-serif;font-size:15px;padding:12px\">"
                        + escapeHtml((String) content) + "</pre></body></html>";
            }
        }
        if (part.isMimeType("multipart/*")) {
            Object content = part.getContent();
            if (content instanceof Multipart) {
                Multipart mp = (Multipart) content;
                String best = null;
                for (int i = 0; i < mp.getCount(); i++) {
                    Part bp = mp.getBodyPart(i);
                    String r = extractHtml(bp);
                    if (r != null) {
                        best = r;
                        if (bp.isMimeType("text/html")) {
                            break;
                        }
                    }
                }
                return best;
            }
        }
        return null;
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
