package vn.edu.utc.hotel_booking.modules.ai.ocr;

import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.modules.ai.ocr.service.TesseractTsvParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TesseractTsvParserTest {

    private final TesseractTsvParser parser = new TesseractTsvParser();

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
