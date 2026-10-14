package vn.edu.utc.hotel_booking.modules.ai.advisor.llm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PiiRedactionUtilTest {

    @Test
    @DisplayName("Should mask CCCD numbers (9-12 digits)")
    void shouldMaskCccd() {
        String input = "Số CCCD của tôi là 001234567890 hãy lưu lại";
        String redacted = PiiRedactionUtil.redact(input);

        assertThat(redacted).doesNotContain("001234567890");
        assertThat(redacted).contains("[REDACTED_CCCD]");
    }

    @Test
    @DisplayName("Should mask phone numbers and email addresses")
    void shouldMaskPhoneAndEmail() {
        String input = "Liên hệ qua email test.guest@example.com hoặc SĐT 0912345678 nhé";
        String redacted = PiiRedactionUtil.redact(input);

        assertThat(redacted).doesNotContain("test.guest@example.com");
        assertThat(redacted).doesNotContain("0912345678");
        assertThat(redacted).contains("[REDACTED_EMAIL]");
        assertThat(redacted).contains("[REDACTED_PHONE]");
    }

    @Test
    @DisplayName("Should mask passport and payment card numbers")
    void shouldMaskPassportAndCard() {
        String input = "Hộ chiếu B1234567 và thẻ 4111 2222 3333 4444";
        String redacted = PiiRedactionUtil.redact(input);

        assertThat(redacted).doesNotContain("B1234567");
        assertThat(redacted).doesNotContain("4111 2222 3333 4444");
        assertThat(redacted).contains("[REDACTED_PASSPORT]");
        assertThat(redacted).contains("[REDACTED_CARD]");
    }
}
