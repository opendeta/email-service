package com.opendeta.Mail.Services;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/** Sends HTML emails through Gmail SMTP using a Gmail App Password. */
public class MailSender {

    public static final String HOST = "smtp.gmail.com";
    public static final String PORT = "587";

    public static Properties baseProps() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", HOST);
        props.put("mail.smtp.port", PORT);
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.ssl.trust", HOST);
        props.put("mail.smtp.connectiontimeout", "20000");
        props.put("mail.smtp.timeout", "30000");
        return props;
    }

    /** Verifies that the Gmail address + App Password can authenticate. */
    public static boolean verify(String gmail, String appPassword) {
        try {
            Session session = Session.getInstance(baseProps(), null);
            Transport transport = session.getTransport("smtp");
            transport.connect(HOST, gmail, appPassword);
            transport.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void send(String from, String appPassword, String to, String subject, String html)
            throws Exception {
        send(from, appPassword, new String[]{to}, subject, html);
    }

    public static void send(String from, String appPassword, String[] to, String subject, String html)
            throws Exception {
        Session session = Session.getInstance(baseProps(), new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, appPassword);
            }
        });

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(String.join(",", to)));
        message.setSubject(subject);
        message.setContent(html, "text/html; charset=utf-8");
        Transport.send(message);
    }
}
