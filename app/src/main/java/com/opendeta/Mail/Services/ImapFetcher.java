package com.opendeta.Mail.Services;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;

/**
 * Streams the newest emails of the signed-in Gmail account over IMAP.
 * Every message is decoded and handed to the listener immediately, so the
 * inbox fills up quickly, one email after another.
 */
public class ImapFetcher {

    public static final String HOST = "imap.gmail.com";
    public static final int PORT = 993;

    public interface Listener {
        void onEmail(EmailItem item);

        void onComplete(int count);

        void onError(Exception e);
    }

    public static void fetchStream(String user, String appPassword, int max, Listener listener) {
        Store store = null;
        Folder inbox = null;
        int count = 0;

        try {
            Properties props = new Properties();
            props.put("mail.store.protocol", "imaps");
            props.put("mail.imaps.host", HOST);
            props.put("mail.imaps.port", String.valueOf(PORT));
            props.put("mail.imaps.ssl.enable", "true");
            props.put("mail.imaps.ssl.protocols", "TLSv1.2");
            props.put("mail.imaps.connectiontimeout", "20000");
            props.put("mail.imaps.timeout", "30000");

            Session session = Session.getInstance(props, null);
            store = session.getStore("imaps");
            store.connect(HOST, user, appPassword);

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            int total = inbox.getMessageCount();
            if (total > 0) {
                int start = Math.max(1, total - max + 1);
                Message[] messages = inbox.getMessages(start, total);

                UIDFolder uidFolder = (inbox instanceof UIDFolder) ? (UIDFolder) inbox : null;
                SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy", Locale.US);

                // newest first
                for (int i = messages.length - 1; i >= 0; i--) {
                    Message msg = messages[i];
                    EmailItem item = new EmailItem();

                    item.uid = uidFolder != null
                            ? String.valueOf(uidFolder.getUID(msg))
                            : String.valueOf(msg.getMessageNumber());

                    item.to = user;
                    try {
                        Address[] to = msg.getRecipients(Message.RecipientType.TO);
                        if (to != null && to.length > 0) {
                            item.to = to[0].toString();
                        }
                    } catch (Exception ignored) {
                    }

                    String subject = msg.getSubject();
                    item.subject = (subject == null || subject.trim().isEmpty())
                            ? "(No Subject)" : subject;

                    Date date = null;
                    try {
                        date = msg.getSentDate();
                    } catch (Exception ignored) {
                    }
                    item.timestamp = date != null ? date.getTime() : 0L;
                    item.date = date != null ? fmt.format(date) : "00/00/0000";

                    item.body = EmailDecoder.decode(msg);

                    listener.onEmail(item);
                    count++;
                }
            }

            listener.onComplete(count);

        } catch (Exception e) {
            listener.onError(e);
        } finally {
            try {
                if (inbox != null && inbox.isOpen()) inbox.close(false);
            } catch (Exception ignored) {
            }
            try {
                if (store != null && store.isConnected()) store.close();
            } catch (Exception ignored) {
            }
        }
    }
}
