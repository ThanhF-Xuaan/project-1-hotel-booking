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
}
