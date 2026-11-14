package vn.edu.utc.hotel_booking.modules.ai.ocr.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/** Reads bilingual labels and wrapped values without treating captions or footers as identity data. */
final class IdentityTextLabels {
    private IdentityTextLabels() { }
    static String fold(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT);
    }
    private static final Pattern BOUNDARY = Pattern.compile(
            "ho\\h*(?:va\\h*)?ten|full\\h*name|ngay\\h*sinh|date\\h*of\\h*birth|gioi\\h*tinh|sex|gender|quoc\\h*tich|nationality|que\\h*quan|place\\h*of\\h*origin|noi\\h*thuong\\h*tru|place\\h*of\\h*residence|co\\h*gia\\h*tri|date\\h*of\\h*expiry|khong\\h*co\\h*gia\\h*tri|du\\h*lieu\\h*gia|so\\h*(?:cccd|cmnd|/)|passport|document\\h*number");
    private static final String ADDRESS_LABEL = "(?:n[od0][il1]\\h*thuong\\h*tru|noi\\h*dktt|dia\\h*chi|permanent\\h*address|place\\h*of\\h*residence)(?:\\h*/\\h*place\\h*of\\h*residence)?\\h*[:.]?\\h*";
    static Matcher addressLabel(String text) { return Pattern.compile(ADDRESS_LABEL).matcher(fold(text)); }
    static String read(String text, String label, int maxValueLines) {
        String[] lines = Normalizer.normalize(text, Normalizer.Form.NFC).split("\\R");
        Pattern pattern = Pattern.compile(label);
        for (int i = 0; i < lines.length; i++) {
            var match = pattern.matcher(fold(lines[i]));
            if (!match.find()) continue;
            StringBuilder value = new StringBuilder();
            int accepted = 0;
            for (int j = i; j < lines.length && j <= i + maxValueLines + 2; j++) {
                String part = j == i ? lines[j].substring(match.end()) : lines[j];
                var boundary = BOUNDARY.matcher(fold(part));
                boolean stop = boundary.find();
                if (stop) part = part.substring(0, boundary.start());
                part = part.replaceAll("^[\\h/:.]+|[\\h]+$", "").trim();
                if (part.length() > 1 && !part.matches("[\\p{Punct}\\d\\h]{1,4}")) {
                    if (!value.isEmpty()) value.append(' ');
                    value.append(part);
                    accepted++;
                }
                if (stop || accepted >= maxValueLines) break;
            }
            if (!value.isEmpty()) return value.toString();
        }
        return null;
    }
    static String name(String text) {
        String value = read(text, "(?:ho\\h*(?:va\\h*|,\\h*)?ten|full\\h*name)(?:\\h*/\\h*full\\h*name)?\\h*[:.]?\\h*", 1);
        if (value == null || !value.matches("[\\p{L}][\\p{L} '\\-]{2,99}")) return null;
        return value.trim().replaceAll("\\h+", " ").toUpperCase(Locale.ROOT);
    }
    static String birth(String text) {
        return read(text, "(?:ngay\\h*sinh|date\\h*of\\h*birth)(?:\\h*/\\h*date\\h*of\\h*birth)?\\h*[:.]?\\h*", 1);
    }
    static String gender(String text) {
        return read(text, "(?:gioi\\h*tinh|sex|gender)(?:\\h*/\\h*sex)?\\h*[:.]?\\h*", 1);
    }
    static String nationality(String text) {
        String value = read(text, "(?:quoc\\h*tich|nationality)(?:\\h*/\\h*nationality)?\\h*[:.]?\\h*", 1);
        if (value == null) return null;
        value = stripCaption(value, "nationality");
        // A recognized country is normalized; citizenship is never inferred from the document type.
        if (fold(value).replaceAll("[^a-z]", "").equals("vietnam")) return "Việt Nam";
        return value.matches("[\\p{L}][\\p{L} '\\-]{1,59}") ? value : null;
    }
    static String address(String text) {
        String value = read(text, ADDRESS_LABEL, 3);
        if (value == null) return null;
        value = stripCaption(value, "placeofresidence");
        // A damaged expiry caption is evidence of mixed columns, not an address component.
        String[] tokens = value.split("\\h+");
        for (int i = 0; i < tokens.length; i++) {
            StringBuilder caption = new StringBuilder();
            for (int j = i; j < Math.min(tokens.length, i + 4); j++) {
                caption.append(fold(tokens[j]).replaceAll("[^a-z]", ""));
                if (caption.toString().startsWith("date") && distance(caption.toString(), "dateofexpiry") <= 3) return null;
            }
        }
        return value;
    }

    private static String stripCaption(String value, String expected) {
        value = value.replaceAll("^[\\h/:.]+", "");
        int colon = value.indexOf(':');
        if (colon >= 0 && colon < 32 && distance(fold(value.substring(0, colon)).replaceAll("[^a-z]", ""), expected) <= 3)
            return value.substring(colon + 1).replaceAll("^[\\h/:.]+", "").trim();
        int space = value.indexOf(' ');
        if (space > 0 && distance(fold(value.substring(0, space)).replaceAll("[^a-z]", ""), expected) <= 3)
            return value.substring(space + 1).replaceAll("^[\\h/:.]+", "").trim();
        return value.trim();
    }
    private static int distance(String a, String b) {
        int[] row = new int[b.length() + 1];
        for (int j = 0; j < row.length; j++) row[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            int previous = row[0]; row[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int old = row[j];
                row[j] = Math.min(Math.min(row[j] + 1, row[j - 1] + 1), previous + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1));
                previous = old;
            }
        }
        return row[b.length()];
    }
}
