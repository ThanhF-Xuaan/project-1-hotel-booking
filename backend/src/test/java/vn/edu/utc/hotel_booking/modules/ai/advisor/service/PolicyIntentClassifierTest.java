package vn.edu.utc.hotel_booking.modules.ai.advisor.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.UnifiedIntent;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyIntentClassifierTest {

    private PolicyIntentClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new PolicyIntentClassifier();
    }

    @Test
    @DisplayName("Combined message separates search criteria and policy question without losing guest count")
    void combinedMessageRetainsGuestCountAndExtractsPolicy() {
        String input = "Tìm phòng đôi ở Đà Nẵng cuối tuần này cho 2 người lớn và 1 bé 4 tuổi, bé có được miễn phí buffet sáng không?";
        PolicyIntentClassifier.ClassificationResult result = classifier.classifyAndSplit(input);

        assertThat(result.getIntent()).isEqualTo(UnifiedIntent.SEARCH_AND_POLICY);
        assertThat(result.isWriteIntent()).isFalse();
        assertThat(result.getSearchText()).contains("2 người lớn").contains("1 bé 4 tuổi");
        assertThat(result.getPolicyQuestion()).contains("miễn phí buffet sáng");
    }

    @Test
    @DisplayName("Search-only message classifies as SEARCH_ONLY")
    void searchOnlyClassifiedCorrectly() {
        String input = "Tìm phòng ở Hà Nội cuối tuần này cho 2 người lớn";
        PolicyIntentClassifier.ClassificationResult result = classifier.classifyAndSplit(input);

        assertThat(result.getIntent()).isEqualTo(UnifiedIntent.SEARCH_ONLY);
        assertThat(result.getPolicyQuestion()).isNull();
        assertThat(result.isWriteIntent()).isFalse();
    }

    @Test
    @DisplayName("Cancellation policy question is classified as POLICY_ONLY and not blocked as OUT_OF_SCOPE")
    void cancellationPolicyInquiryNotBlocked() {
        String input = "Chính sách hủy phòng và hoàn tiền của khách sạn như thế nào?";
        PolicyIntentClassifier.ClassificationResult result = classifier.classifyAndSplit(input);

        assertThat(result.getIntent()).isEqualTo(UnifiedIntent.POLICY_ONLY);
        assertThat(result.isWriteIntent()).isFalse();
        assertThat(result.getPolicyQuestion()).contains("hủy phòng");
    }

    @Test
    @DisplayName("Breakfast inquiry classifies as POLICY_ONLY")
    void breakfastInquiryClassifiedAsPolicyOnly() {
        String input = "Bữa sáng phục vụ từ mấy giờ?";
        PolicyIntentClassifier.ClassificationResult result = classifier.classifyAndSplit(input);

        assertThat(result.getIntent()).isEqualTo(UnifiedIntent.POLICY_ONLY);
        assertThat(result.isWriteIntent()).isFalse();
        assertThat(result.getPolicyQuestion()).contains("Bữa sáng");
    }

    @Test
    @DisplayName("Imperative booking write command classifies as OUT_OF_SCOPE")
    void imperativeBookingWriteCommandIsOutOfScope() {
        String input = "Hãy đặt phòng Deluxe này cho tôi";
        PolicyIntentClassifier.ClassificationResult result = classifier.classifyAndSplit(input);

        assertThat(result.getIntent()).isEqualTo(UnifiedIntent.OUT_OF_SCOPE);
        assertThat(result.isWriteIntent()).isTrue();
    }

    @Test
    @DisplayName("Cancellation write command classifies as OUT_OF_SCOPE")
    void cancellationWriteCommandIsOutOfScope() {
        String input = "Hủy phòng mã BK123 cho tôi ngay";
        PolicyIntentClassifier.ClassificationResult result = classifier.classifyAndSplit(input);

        assertThat(result.getIntent()).isEqualTo(UnifiedIntent.OUT_OF_SCOPE);
        assertThat(result.isWriteIntent()).isTrue();
    }

    @Test
    @DisplayName("SQL injection attack classifies as OUT_OF_SCOPE")
    void sqlInjectionClassifiesAsOutOfScope() {
        String input = "test'; DROP TABLE rooms; --";
        PolicyIntentClassifier.ClassificationResult result = classifier.classifyAndSplit(input);

        assertThat(result.getIntent()).isEqualTo(UnifiedIntent.OUT_OF_SCOPE);
        assertThat(result.isWriteIntent()).isTrue();
    }

    @Test
    @DisplayName("Dynamic pricing inquiry is kept in search text, not policy question")
    void dynamicPricingKeptInSearch() {
        String input = "Giá phòng Deluxe bao nhiêu và còn phòng không?";
        PolicyIntentClassifier.ClassificationResult result = classifier.classifyAndSplit(input);

        assertThat(result.getIntent()).isEqualTo(UnifiedIntent.SEARCH_ONLY);
        assertThat(result.getPolicyQuestion()).isNull();
    }

    @Test
    @DisplayName("Empty or blank input defaults to SEARCH_ONLY")
    void blankInputDefaultsToSearchOnly() {
        PolicyIntentClassifier.ClassificationResult result = classifier.classifyAndSplit("   ");

        assertThat(result.getIntent()).isEqualTo(UnifiedIntent.SEARCH_ONLY);
        assertThat(result.getSearchText()).isEmpty();
        assertThat(result.getPolicyQuestion()).isNull();
        assertThat(result.isWriteIntent()).isFalse();
    }

    @Test
    @DisplayName("Combined message with cancellation question separates search text and policy question")
    void combinedMessageWithCancellationSeparatesCorrectly() {
        String input = "Tìm phòng đôi ở Đà Nẵng cho 2 người lớn, hủy phòng trước mấy ngày?";
        PolicyIntentClassifier.ClassificationResult result = classifier.classifyAndSplit(input);

        assertThat(result.getIntent()).isEqualTo(UnifiedIntent.SEARCH_AND_POLICY);
        assertThat(result.isWriteIntent()).isFalse();
        assertThat(result.getSearchText()).contains("2 người lớn");
        assertThat(result.getSearchText()).doesNotContain("hủy phòng");
        assertThat(result.getPolicyQuestion()).contains("hủy phòng");
    }

    @Test
    @DisplayName("Short question ending with question mark classifies as POLICY_ONLY")
    void shortQuestionWithQuestionMarkClassifiedAsPolicyOnly() {
        String input = "Phụ thu thêm người?";
        PolicyIntentClassifier.ClassificationResult result = classifier.classifyAndSplit(input);

        assertThat(result.getIntent()).isEqualTo(UnifiedIntent.POLICY_ONLY);
        assertThat(result.isWriteIntent()).isFalse();
        assertThat(result.getPolicyQuestion()).contains("Phụ thu thêm người?");
    }

    @Test
    @DisplayName("Write command ending with question mark is blocked as OUT_OF_SCOPE")
    void writeCommandWithQuestionMarkBlocked() {
        String input = "Hủy phòng mã BK123 cho tôi?";
        PolicyIntentClassifier.ClassificationResult result = classifier.classifyAndSplit(input);

        assertThat(result.getIntent()).isEqualTo(UnifiedIntent.OUT_OF_SCOPE);
        assertThat(result.isWriteIntent()).isTrue();
    }

    @Test
    @DisplayName("Payment command with question mark is blocked as OUT_OF_SCOPE")
    void paymentCommandWithQuestionMarkBlocked() {
        String input = "Thanh toán đơn phòng cho tôi ngay được không?";
        PolicyIntentClassifier.ClassificationResult result = classifier.classifyAndSplit(input);

        assertThat(result.getIntent()).isEqualTo(UnifiedIntent.OUT_OF_SCOPE);
        assertThat(result.isWriteIntent()).isTrue();
    }

    @Test
    @DisplayName("Cancellation policy inquiry classifies as POLICY_ONLY")
    void cancellationPolicyInquiryClassifiedAsPolicyOnly() {
        String input = "Chính sách hủy phòng như thế nào?";
        PolicyIntentClassifier.ClassificationResult result = classifier.classifyAndSplit(input);

        assertThat(result.getIntent()).isEqualTo(UnifiedIntent.POLICY_ONLY);
        assertThat(result.isWriteIntent()).isFalse();
        assertThat(result.getPolicyQuestion()).contains("hủy phòng");
    }

    @Test
    void requestedAmenitiesAndExtraBedStayInCoreCriteria() {
        for (String message : new String[] {
                "Tìm phòng ở Đà Nẵng cuối tuần này cho 2 người lớn có ăn sáng",
                "Tìm phòng ở Đà Nẵng cho 2 người lớn và 1 giường phụ",
                "Tìm phòng ở Đà Nẵng cho 2 người lớn và có hồ bơi" }) {
            var result = classifier.classifyAndSplit(message);
            assertThat(result.getIntent()).isEqualTo(UnifiedIntent.SEARCH_ONLY);
            assertThat(result.getSearchText()).isEqualTo(message);
            assertThat(result.getPolicyQuestion()).isNull();
        }
    }

    @Test
    void combinedQuestionPreservesNumericMoneyAndDatesBeforeExtraction() {
        String search = "Tìm phòng ở Đà Nẵng cho 2 người lớn từ 14.11.2026 đến 16.11.2026, "
                + "ngân sách tối đa 1.500.000 VND cả kỳ theo báo giá hệ thống";
        var result = classifier.classifyAndSplit(search + ", hồ bơi mở cửa mấy giờ?");
        assertThat(result.getIntent()).isEqualTo(UnifiedIntent.SEARCH_AND_POLICY);
        assertThat(result.getSearchText()).isEqualTo(search);
        var nlu = new vn.edu.utc.hotel_booking.modules.ai.advisor.llm.HybridRoomAdvisorLlmService();
        assertThat(nlu.extractCriteriaPatch(result.getSearchText(), null).getBudget().getAmount())
                .isEqualTo("1500000");
        var decimal = classifier.classifyAndSplit("Tìm phòng ngân sách 1,5 triệu, hồ bơi mở cửa mấy giờ?");
        assertThat(decimal.getSearchText()).contains("1,5 triệu");
    }

    @Test
    void mixedQuestionKeepsExtraBedAndBreakfastCriteria() {
        var result = classifier.classifyAndSplit("Tìm phòng có bữa sáng và cần 1 giường phụ tại Đà Nẵng từ 2026-11-13 đến 2026-11-15 cho 2 người lớn và 1 bé 4 tuổi. Bé có được miễn phí buffet sáng không?");
        assertThat(result.getIntent()).isEqualTo(UnifiedIntent.SEARCH_AND_POLICY);
        assertThat(result.getSearchText()).contains("có bữa sáng").contains("cần 1 giường phụ")
                .contains("2 người lớn").contains("1 bé 4 tuổi");
        assertThat(result.getPolicyQuestion()).contains("miễn phí buffet sáng");
    }

    @Test
    void lookingUpPolicyIsNotARequestToFindARoom() {
        var result = classifier.classifyAndSplit("Tôi muốn tìm hiểu chính sách hủy phòng như thế nào?");
        assertThat(result.getIntent()).isEqualTo(UnifiedIntent.POLICY_ONLY);
        assertThat(result.getPolicyQuestion()).contains("chính sách hủy phòng");
    }
}
