package vn.edu.utc.hotel_booking.modules.ai.advisor.llm;

import java.util.regex.Pattern;

public final class PiiRedactionUtil {

    private PiiRedactionUtil() {}

    // Vietnamese Citizen ID (CCCD): 9 to 12 digits
    private static final Pattern CCCD_PATTERN = Pattern.compile("\\b\\d{9,12}\\b");

    // Passport: 1 uppercase letter followed by 7-8 digits (e.g. B1234567)
    private static final Pattern PASSPORT_PATTERN = Pattern.compile("(?i)\\b[A-Z]\\d{7,8}\\b");

    // Vietnamese Phone Numbers: (0 or +84) followed by 9 digits
    private static final Pattern PHONE_PATTERN = Pattern.compile("(\\+84|0)(3|5|7|8|9)\\d{8}\\b");

    // Email address
    private static final Pattern EMAIL_PATTERN = Pattern.compile("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b");

    // Payment card: 13-19 digits, possibly separated by spaces or dashes
    private static final Pattern CARD_PATTERN = Pattern.compile("\\b(?:\\d[ -]?){13,19}\\b");

    /**
     * Masks PII information in untrusted user text before logging or feeding to LLMs.
     */
    public static String redact(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        String result = input;
        result = EMAIL_PATTERN.matcher(result).replaceAll("[REDACTED_EMAIL]");
        result = PHONE_PATTERN.matcher(result).replaceAll("[REDACTED_PHONE]");
        result = CARD_PATTERN.matcher(result).replaceAll("[REDACTED_CARD]");
        result = CCCD_PATTERN.matcher(result).replaceAll("[REDACTED_CCCD]");
        result = PASSPORT_PATTERN.matcher(result).replaceAll("[REDACTED_PASSPORT]");

        return result;
    }
}
