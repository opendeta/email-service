package com.opendeta.Mail.Services;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.mail.BodyPart;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

/**
 * Android/JavaMail safe email body decoder.
 *
 * Some Android JavaMail builds return an {@link InputStream} from
 * {@code Part.getContent()} instead of a {@link Multipart} object. A naive
 * cast therefore throws and breaks the whole fetch. This decoder converts the
 * stream back into a MimeMultipart (the fix), and reads both String and
 * InputStream based content.
 */
public class EmailDecoder {

    public static String decode(Part part) {
        try {
            return decodePart(part);
        } catch (Exception e) {
            return "";
        }
    }

    private static String decodePart(Part p) throws Exception {
        if (p.isMimeType("text/html")) {
            return readStream(p.getContent());
        }

        if (p.isMimeType("multipart/*")) {
            Object content = p.getContent();
            Multipart mp;

            // -------- the important fix --------
            if (content instanceof Multipart) {
                mp = (Multipart) content;
            } else if (content instanceof InputStream) {
                mp = new MimeMultipart(new ByteArrayDataSource((InputStream) content, p.getContentType()));
            } else {
                return "";
            }

            String textFound = "";
            for (int i = 0; i < mp.getCount(); i++) {
                BodyPart bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/html")) {
                    return readStream(bp.getContent());
                } else if (bp.isMimeType("text/plain")) {
                    if (textFound.isEmpty()) {
                        textFound = readStream(bp.getContent());
                    }
                } else {
                    String nested = decodePart(bp);
                    if (nested != null && !nested.isEmpty()) {
                        return nested;
                    }
                }
            }
            return textFound;
        }

        if (p.isMimeType("text/plain")) {
            return readStream(p.getContent());
        }
        return "";
    }

    private static String readStream(Object content) throws Exception {
        if (content instanceof String) {
            return (String) content;
        }
        if (content instanceof InputStream) {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader((InputStream) content, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString();
        }
        return String.valueOf(content);
    }
}
