package vn.edu.utc.hotel_booking.modules.ai.advisor.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.ComparisonFocus;
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

        String input2 = "Lỡ đặt rồi mà gần tới ngày bận việc đột xuất không đi được thì có được hoàn tiền cọc không shop ơi?";
        PolicyIntentClassifier.ClassificationResult result2 = classifier.classifyAndSplit(input2);
        assertThat(result2.isWriteIntent()).isFalse();
        assertThat(result2.getIntent()).isEqualTo(UnifiedIntent.POLICY_ONLY);
        assertThat(result2.getPolicyQuestion()).contains("hoàn tiền cọc");

        String input3 = "Lỡ đặt cọc rồi mà bận việc đột xuất có được hoàn cọc không shop?";
        PolicyIntentClassifier.ClassificationResult result3 = classifier.classifyAndSplit(input3);
        assertThat(result3.isWriteIntent()).isFalse();
        assertThat(result3.getIntent()).isEqualTo(UnifiedIntent.POLICY_ONLY);
        assertThat(result3.getPolicyQuestion()).contains("hoàn cọc");
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

    @Test
    @DisplayName("Follow-up room comparison with family context classifies as ROOM_COMPARISON (not SEARCH_AND_COMPARISON)")
    void followUpComparisonClassifiesAsRoomComparison() {
        String input = "So sánh giúp tôi phòng Deluxe và Executive Suite, gia đình tôi có con nhỏ thì nên ở phòng nào?";
        var result = classifier.classifyAndSplit(input);

        assertThat(result.getIntent()).isEqualTo(UnifiedIntent.ROOM_COMPARISON);
        assertThat(result.isWriteIntent()).isFalse();
        assertThat(result.getComparisonFocus()).isEqualTo(ComparisonFocus.FAMILY_KIDS);
        assertThat(result.getTargetRoomHints()).containsExactly("deluxe", "executive");
    }

    @Test
    @DisplayName("Comparison question ending with '?' routes to ROOM_COMPARISON instead of POLICY_ONLY")
    void comparisonQuestionWithQuestionMarkRoutesToRoomComparison() {
        var result = classifier.classifyAndSplit("Phòng Superior và Deluxe khác gì nhau để tiết kiệm chi phí?");

        assertThat(result.getIntent()).isEqualTo(UnifiedIntent.ROOM_COMPARISON);
        assertThat(result.getComparisonFocus()).isEqualTo(ComparisonFocus.BUDGET_SAVER);
        assertThat(result.getTargetRoomHints()).contains("superior", "deluxe");
    }

    @Test
    @DisplayName("Comparison combined with new location and dates routes to SEARCH_AND_COMPARISON")
    void comparisonWithNewSearchCriteriaRoutesToSearchAndComparison() {
        var result = classifier.classifyAndSplit(
                "Tìm phòng ở Đà Nẵng từ 10/12 đến 12/12 cho 2 người lớn rồi so sánh giúp tôi"
        );

        assertThat(result.getIntent()).isEqualTo(UnifiedIntent.SEARCH_AND_COMPARISON);
        assertThat(result.isWriteIntent()).isFalse();
    }

    @Test
    @DisplayName("stripComparisonRoomCount converts 'so sánh 2 phòng' so roomsCount regex is not falsely triggered")
    void stripComparisonRoomCountPreventsRoomsCountFalsePositive() {
        String input = "Tìm phòng ở Đà Nẵng từ 2026-11-13 đến 2026-11-15 cho 2 người lớn rồi so sánh 2 phòng giúp tôi";
        String stripped = PolicyIntentClassifier.stripComparisonRoomCount(input);
        assertThat(stripped).contains("so sánh các phòng");

        var nlu = new vn.edu.utc.hotel_booking.modules.ai.advisor.llm.HybridRoomAdvisorLlmService();
        var patchRaw = nlu.extractCriteriaPatch(input, null);
        assertThat(patchRaw.getRoomsCount()).isEqualTo(2);

        var patchStripped = nlu.extractCriteriaPatch(stripped, null);
        assertThat(patchStripped.getRoomsCount()).isNull();
    }

    @Test
    @DisplayName("Comparison combined with cancellation question cleanly separates policy and does not trigger OUT_OF_SCOPE")
    void comparisonWithCancellationQuestionSeparatesPolicySafely() {
        String input = "Tìm phòng ở Đà Nẵng từ 2026-11-13 đến 2026-11-15 cho 2 người lớn rồi so sánh 2 phòng giúp tôi, hủy phòng trước mấy ngày thì được hoàn tiền?";
        var result = classifier.classifyAndSplit(input);

        assertThat(result.getIntent()).isEqualTo(UnifiedIntent.SEARCH_AND_COMPARISON);
        assertThat(result.isWriteIntent()).isFalse();
        assertThat(result.getSearchText()).contains("Đà Nẵng").contains("2 người lớn");
        assertThat(result.getSearchText()).doesNotContain("hủy phòng");
        assertThat(result.getPolicyQuestion()).contains("hủy phòng");
    }

    @Test
    @DisplayName("Check-in and check-out difference question routes to POLICY_ONLY, not ROOM_COMPARISON")
    void checkInCheckOutDifferenceRoutesToPolicyOnly() {
        String input = "Quy định nhận phòng và trả phòng khác nhau thế nào?";
        var result = classifier.classifyAndSplit(input);

        assertThat(result.getIntent()).isEqualTo(UnifiedIntent.POLICY_ONLY);
        assertThat(result.isWriteIntent()).isFalse();
        assertThat(result.getPolicyQuestion()).contains("nhận phòng");
    }

    @Test
    @DisplayName("'Phòng bé quá' triggers SPACE_COMFORT, not FAMILY_KIDS")
    void smallRoomPhraseDoesNotTriggerKidsFocus() {
        String input = "So sánh phòng Deluxe và Suite, phòng Standard bé quá thì nên đổi phòng nào rộng hơn?";
        var result = classifier.classifyAndSplit(input);

        assertThat(result.getIntent()).isEqualTo(UnifiedIntent.ROOM_COMPARISON);
        assertThat(result.getComparisonFocus()).isEqualTo(ComparisonFocus.SPACE_COMFORT);
    }

    @Test
    @DisplayName("English query with 'would be better for business' triggers WORK_TRIP")
    void englishBusinessQueryTriggersWorkTrip() {
        String input = "Compare Deluxe and Executive Suite, which room would be better for business travel?";
        var result = classifier.classifyAndSplit(input);

        assertThat(result.getIntent()).isEqualTo(UnifiedIntent.ROOM_COMPARISON);
        assertThat(result.getComparisonFocus()).isEqualTo(ComparisonFocus.WORK_TRIP);
        assertThat(result.getTargetRoomHints()).containsExactly("deluxe", "executive");
    }

    @Test
    @DisplayName("hasExplicitPolicyMarker distinguishes genuine policy questions from comparison selection questions")
    void hasExplicitPolicyMarkerIdentifiesPolicyMarkersVsGeneralQuestions() {
        assertThat(classifier.hasExplicitPolicyMarker("Khách sạn có cho mang thú cưng không?")).isTrue();
        assertThat(classifier.hasExplicitPolicyMarker("Mấy giờ nhận phòng và trả phòng?")).isTrue();
        assertThat(classifier.hasExplicitPolicyMarker("Chính sách hủy phòng thế nào?")).isTrue();
        assertThat(classifier.hasExplicitPolicyMarker("Gia đình có con nhỏ nên chọn cái nào?")).isFalse();
        assertThat(classifier.hasExplicitPolicyMarker("Cái nào phù hợp hơn cho 2 người?")).isFalse();
        assertThat(classifier.hasExplicitPolicyMarker("")).isFalse();
        assertThat(classifier.hasExplicitPolicyMarker(null)).isFalse();
    }
}
