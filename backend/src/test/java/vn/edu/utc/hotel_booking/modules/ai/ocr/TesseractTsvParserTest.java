package vn.edu.utc.hotel_booking.modules.ai.ocr;

import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.modules.ai.ocr.service.TesseractTsvParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TesseractTsvParserTest {

    private final TesseractTsvParser parser = new TesseractTsvParser();

    @Test void nameCropExcludesCaptionAbsorbedIntoOneTallWordBox() {
        String tsv = header()
                + "5\t1\t1\t1\t1\t1\t160\t80\t90\t110\t70\tNGUYEN\n"
                + "5\t1\t1\t1\t1\t2\t260\t100\t45\t90\t90\tVĂN\n"
                + "5\t1\t1\t1\t1\t3\t315\t115\t65\t60\t90\tMINH\n";
        var parsed = parser.parse(tsv);
        assertThat(parsed.nameRegion("NGUYEN VĂN MINH").top()).isGreaterThan(85);
        assertThat(parsed.surnameRegion("NGUYEN VĂN MINH").top())
                .isEqualTo(parsed.nameRegion("NGUYEN VĂN MINH").top());
        assertThat(parsed.surnameRegion("NGUYEN VĂN MINH").width()).isEqualTo(90);
    }

    @Test void residenceCropExcludesTinyBorderNoiseWithoutDroppingItsContinuation() {
        String tsv = header()
                + word(1, 160, 180, 160, "Nơi thường trú:")
                + word(1, 330, 180, 95, "Thôn Hoa Mẫu")
                + word(2, 160, 210, 220, "Phường Minh Khai, Hà Nội")
                + "5\t1\t1\t1\t3\t1\t900\t230\t12\t2\t80\tzz\n";
        assertThat(parser.parse(tsv).addressRegion())
                .isEqualTo(new TesseractTsvParser.Region(160, 180, 265, 50));
    }

    @Test void locatesNameWithinMergedCaptionRowWithoutIncludingNeighbouringFields() {
        String tsv = header()
                + word(1, 20, 100, 120, "Họ và tên:")
                + word(1, 160, 100, 90, "NGUYEN")
                + word(1, 260, 100, 45, "VĂN")
                + word(1, 315, 100, 65, "MINH")
                + word(1, 430, 100, 80, "Ngày sinh:");
        assertThat(parser.parse(tsv).nameRegion("NGUYEN VĂN MINH"))
                .isEqualTo(new TesseractTsvParser.Region(160, 100, 220, 20));
    }

    @Test void doesNotWalkBackFromResidenceLabelIntoNearbyExpiryColumn() {
        String tsv = header()
                + word(1, 10, 180, 125, "Date aforpiy")
                + word(1, 160, 180, 30, "Nơi")
                + word(1, 195, 180, 60, "thường")
                + word(1, 260, 180, 30, "trú:")
                + word(1, 300, 180, 40, "Thôn")
                + word(1, 345, 180, 80, "Hoa Mẫu")
                + word(2, 10, 210, 130, "15/01/2035")
                + word(2, 160, 210, 220, "Phường Minh Khai, Hà Nội");
        var result = parser.parse(tsv);
        assertThat(result.text()).contains("Thôn Hoa Mẫu", "Phường Minh Khai, Hà Nội")
                .doesNotContain("aforpiy", "2035");
        var request = new vn.edu.utc.hotel_booking.modules.ai.ocr.dto.OcrDocumentExtractRequestDto();
        request.setRawOcrText("Số CCCD: 000090000101\n" + result.text());
        assertThat(new vn.edu.utc.hotel_booking.modules.ai.ocr.service.IdentityDocumentOcrService()
                .extract(request).getPermanentAddress().getValue())
                .isEqualTo("Thôn Hoa Mẫu Phường Minh Khai, Hà Nội");
    }

    private String header() {
        return "level\tpage_num\tblock_num\tpar_num\tline_num\tword_num\tleft\ttop\twidth\theight\tconf\ttext\n";
    }

    private String word(int line, int left, int top, int width, String value) {
        return "5\t1\t1\t1\t" + line + "\t1\t" + left + "\t" + top + "\t" + width + "\t20\t90\t" + value + "\n";
    }

    @Test void keepsResidenceColumnAndExcludesExpiryEvenWhenTesseractMergesTheRows() {
        String header = "level\tpage_num\tblock_num\tpar_num\tline_num\tword_num\tleft\ttop\twidth\theight\tconf\ttext\n";
        String tsv = header
                + row(1, 400, 200, "Nơi thường trú / Place of residence: Thôn Hoa Mẫu")
                + row(2, 20, 230, "Date aforpiy")
                + row(2, 400, 230, "Phường Minh Khai, Hà Nội");
        var result = parser.parse(tsv);
        assertThat(result.text()).contains("Thôn Hoa Mẫu", "Phường Minh Khai, Hà Nội")
                .doesNotContain("Date", "aforpiy");
    }

    @Test void retainsNameCoordinatesForRecognitionFromOriginalPixels() {
        String tsv = "level\tpage_num\tblock_num\tpar_num\tline_num\tword_num\tleft\ttop\twidth\theight\tconf\ttext\n"
                + row(1, 400, 200, "NGUYEN VAN MINH");
        assertThat(parser.parse(tsv).nameRegion("NGUYỄN VĂN MINH"))
                .isEqualTo(new TesseractTsvParser.Region(400, 200, 200, 20));
        assertThat(parser.parse(tsv).nameRegion("TRẦN THỊ LAN")).isNull();
    }

    private String row(int line, int left, int top, String value) {
        return "5\t1\t1\t1\t" + line + "\t1\t" + left + "\t" + top + "\t200\t20\t90\t" + value + "\n";
    }

    @Test
    void preservesLineBoundariesAndCalculatesMeanWordConfidence() {
        String tsv = "level\tpage_num\tblock_num\tpar_num\tline_num\tword_num\tleft\ttop\twidth\theight\tconf\ttext\n"
                + "5\t1\t1\t1\t1\t1\t0\t0\t20\t10\t80.0\tHo\n"
                + "5\t1\t1\t1\t1\t2\t21\t0\t30\t10\t100.0\tten\n"
                + "5\t1\t1\t1\t2\t1\t0\t20\t40\t10\t60.0\tTEST\n";

        TesseractTsvParser.ParsedTsv result = parser.parse(tsv);

        assertThat(result.text()).isEqualTo("Ho ten\nTEST");
        assertThat(result.meanWordConfidence()).isEqualTo(0.8);
        assertThat(result.wordCount()).isEqualTo(3);
    }

    @Test
    void ignoresNonWordRowsAndInvalidConfidence() {
        String tsv = "level\tpage_num\tblock_num\tpar_num\tline_num\tword_num\tleft\ttop\twidth\theight\tconf\ttext\n"
                + "4\t1\t1\t1\t1\t0\t0\t0\t100\t10\t95\t\n"
                + "5\t1\t1\t1\t1\t1\t0\t0\t20\t10\t-1\tbad\n";

        TesseractTsvParser.ParsedTsv result = parser.parse(tsv);

        assertThat(result.text()).isEmpty();
        assertThat(result.meanWordConfidence()).isZero();
        assertThat(result.wordCount()).isZero();
    }

    @Test
    void rejectsMalformedTsvInsteadOfReturningSuccess() {
        assertThatThrownBy(() -> parser.parse("not-a-tesseract-tsv"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
