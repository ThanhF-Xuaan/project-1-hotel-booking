package vn.edu.utc.hotel_booking.modules.ai.ocr;

import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.modules.ai.ocr.dto.OcrDocumentExtractRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.ocr.service.IdentityDocumentOcrService;
import vn.edu.utc.hotel_booking.modules.ai.ocr.service.OcrImagePreparation;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.Gender;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class IdentityBilingualExtractionTest {
    @Test void removesDamagedEnglishNationalityCaptionWithoutGuessingCitizenship() {
        for (String caption : java.util.List.of("Nationaiity: .", "Nationallty /", "Nationality:")) {
            var draft = extract("Quốc tịch / " + caption + " Việt Nam\nSố CCCD: 000090000101");
            assertThat(draft.getNationality().getValue()).as(caption).isEqualTo("Việt Nam");
        }
        assertThat(extract("Quốc tịch / Nationaiity:\nSố CCCD: 000090000101")
                .getNationality().getValue()).isNull();
        assertThat(extract("Nationality: Canada\nSố CCCD: 000090000101")
                .getNationality().getValue()).isEqualTo("Canada");
    }

    @Test void mixedExpiryCaptionIsNeverReturnedAsResidence() {
        var draft = extract("""
                Số CCCD: 000090000101
                Nơi thường trú / Place of residence: Thôn Hoa Mẫu
                Date aforpiy _ Phường Minh Khai, Hà Nội
                """);
        assertThat(draft.getPermanentAddress().getValue()).isNull();
    }

    @Test void nameLineCropIsBoundedAndDoesNotUseDisk() throws Exception {
        byte[] original;
        try (var input = getClass().getResourceAsStream("/ai/ocr/cccd-synthetic.png")) { original = input.readAllBytes(); }
        byte[] row = OcrImagePreparation.textLine(original,
                new vn.edu.utc.hotel_booking.modules.ai.ocr.service.TesseractTsvParser.Region(300, 300, 600, 40));
        assertThat(row).isNotNull().hasSizeLessThan(5_242_880);
        assertThat(OcrImagePreparation.textLine(original,
                new vn.edu.utc.hotel_booking.modules.ai.ocr.service.TesseractTsvParser.Region(9999, 9999, 600, 40))).isNull();
    }
    @Test void readsBilingualWrappedFieldsAndDoesNotConfuseExpiryWithBirth() {
        var draft = extract("""
                CĂN CƯỚC CÔNG DÂN
                Có giá trị đến / Date of expiry: 15/01/2035
                Số / No.: 000090000101
                Họ và tên / Full name:
                NGUYỄN VĂN MINH
                Ngày sinh / Date of birth: 15/01/1990
                Giới tính / Sex: Nam Quốc tịch / Nationality: Việt Nam
                Quê quán / Place of origin: Phường Minh Khai, Hà Nội
                Nơi thường trú / Place of residence: - 2
                Số 101, Đường Hoa Mẫu Đơn,
                Phường Minh Khai, Hà Nội
                KHÔNG CÓ GIÁ TRỊ PHÁP LÝ
                """);
        assertThat(draft.getFullName().getValue()).isEqualTo("NGUYỄN VĂN MINH");
        assertThat(draft.getDateOfBirth().getValue()).isEqualTo(LocalDate.of(1990, 1, 15));
        assertThat(draft.getGender().getValue()).isEqualTo(Gender.MALE);
        assertThat(draft.getNationality().getValue()).isEqualTo("Việt Nam");
        assertThat(draft.getDocumentNumber().getValue()).isEqualTo("000090000101");
        assertThat(draft.getPermanentAddress().getValue()).isEqualTo("Số 101, Đường Hoa Mẫu Đơn, Phường Minh Khai, Hà Nội");
        assertThat(draft.isRequiresHumanVerification()).isTrue();
    }

    @Test void missingBirthAndAddressNeverConsumeExpiryOrWatermark() {
        var draft = extract("""
                Số CCCD: 000195000102
                Ho va ten / Full name:
                TRAN THI LAN
                Ngay sinh / Date of birth:
                Gioi tinh / Sex: Nu
                Noi thuong tru / Place of residence:
                KHONG CO GIA TRI PHAP LY
                Date of expiry: 22/08/2035
                """);
        assertThat(draft.getFullName().getValue()).isEqualTo("TRAN THI LAN");
        assertThat(draft.getGender().getValue()).isEqualTo(Gender.FEMALE);
        assertThat(draft.getDateOfBirth().getValue()).isNull();
        assertThat(draft.getPermanentAddress().getValue()).isNull();
        assertThat(draft.getNationality().getValue()).isNull();
    }

    @Test void identityRegionIsAnInMemoryBoundedPng() throws Exception {
        byte[] original;
        try (var input = getClass().getResourceAsStream("/ai/ocr/cccd-synthetic.png")) {
            original = input.readAllBytes();
        }
        byte[] prepared = OcrImagePreparation.identityRegion(original);
        assertThat(prepared).isNotNull().hasSizeLessThan(5_242_880);
        try (var input = new javax.imageio.stream.MemoryCacheImageInputStream(new java.io.ByteArrayInputStream(prepared))) {
            var reader = javax.imageio.ImageIO.getImageReaders(input).next();
            try {
                reader.setInput(input);
                assertThat(reader.getWidth(0)).isEqualTo(1700);
                assertThat(reader.getHeight(0)).isLessThanOrEqualTo(2200);
            } finally { reader.dispose(); }
        }
    }

    private vn.edu.utc.hotel_booking.modules.ai.ocr.dto.OcrStayGuestDraftDto extract(String text) {
        var request = new OcrDocumentExtractRequestDto();
        request.setRawOcrText(text);
        return new IdentityDocumentOcrService().extract(request);
    }
}
