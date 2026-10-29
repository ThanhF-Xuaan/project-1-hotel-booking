# U13 local image v1 — contract delta và test oracle

- Trạng thái: **PROPOSED — tài liệu C01, chờ review; chưa triển khai OCR ảnh**.
- Baseline **CONFIRMED**: `5509af2e26daca6dd67a32acb987a86648808df2`, ngày 2026-10-28.
- Owner: Member 1; Member 3 duyệt tích hợp check-in. Tham chiếu plan `m1-feature-completion-plan-2026-10-28.md`, C01/OCR-01/OCR-02; A00 đã `TASK_PASSED` tại H002 của handoff canonical trong worktree lập tài liệu.
- Data mode: **synthetic-only**. C01 chỉ thêm tài liệu; không đổi route, DTO, cấu hình, gold hoặc nghiệp vụ.

## 1. Contract hiện tại — CONFIRMED từ source

Các đường dẫn source dưới đây tính từ repository root, tại đúng SHA trên:

| Bằng chứng | Nội dung đối chiếu |
|---|---|
| `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/ocr/controller/IdentityDocumentOcrController.java` | `extract`, `extractFile`, local error handlers, role annotation |
| `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/ocr/dto/OcrDocumentExtractRequestDto.java` | JSON input và Bean Validation |
| `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/ocr/dto/OcrStayGuestDraftDto.java` | Draft output và `requiresHumanVerification` |
| `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/ocr/model/ExtractedFieldDto.java` | `value`, `confidence`, `valid`, `validationMessage` |
| `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/ocr/validator/OcrUploadValidator.java` | Upload size/name/MIME/header checks |
| `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/ocr/service/IdentityDocumentOcrService.java` | Parser regex/MRZ, implements `OcrVisionPort`; chưa nhận diện pixel |
| `backend/src/main/java/vn/edu/utc/hotel_booking/common/config/SecurityConfig.java` | JWT cho non-public endpoints; không suy ra tenant ownership từ role |
| `backend/src/main/java/vn/edu/utc/hotel_booking/common/dto/ApiResponse.java` | Success envelope `code=200`, `message`, `result` |

### Route và request

| HTTP route | Content type / input | Kết quả hiện tại |
|---|---|---|
| `POST /api/v1/ai/ocr/extract` | JSON `OcrDocumentExtractRequestDto` | Phân tích text do client gửi |
| `POST /api/v1/ai/ocr/extract-file` | Multipart: `file` bắt buộc; `rawOcrText` tuỳ chọn | Validate file; nếu có text thì phân tích text, chưa OCR ảnh |

JSON `/extract`: `fileName` bắt buộc, không rỗng, tối đa 255 ký tự, cấm `..`, `/`, `\`; `contentType` bắt buộc; `fileSizeBytes` từ 1 đến 5242880; `rawOcrText` bắt buộc, không blank, tối đa 10000 ký tự. Tuỳ chọn: `hotelId >= 1`, `documentTypeHint` dùng enum `DocumentType`, `fileHeaderBytes` là byte array Jackson mã hoá base64. Ràng buộc MIME/header thực tế nằm ở upload validator; không suy diễn rằng JSON metadata chứng minh ảnh hợp lệ.

Multipart validator: `0 < file.size <= 5242880` byte (5 MiB), MIME JPEG/PNG/WebP; extension `.jpg/.jpeg/.png/.webp`; tên không traversal; ít nhất 12 byte; magic bytes phải khớp MIME. Validator chỉ kiểm tra header, chưa decode ảnh hoặc giới hạn pixel. Không có tham số `hotelId` hay `documentTypeHint` được controller bind cho multipart. `rawOcrText` multipart chưa có Bean Validation 10000 ký tự như JSON.

Role hiện tại cho cả hai route: `CHAIN_ADMIN`, `PROPERTY_MANAGER`, `RECEPTIONIST`. Không token: bị security filter từ chối; sai role: method security từ chối. HTTP 401/403 là oracle security cần kiểm chứng với filter thật; controller test standalone không chứng minh full security envelope. JSON `hotelId` không tự tạo quyền tenant; parser không truy xuất hotel/guest/history.

### Response và lỗi

`ApiResponse<OcrStayGuestDraftDto>` chứa bảy trường: `fullName`, `dateOfBirth` (ISO date), `gender`, `nationality`, `documentType`, `documentNumber`, `permanentAddress`. Mỗi trường nếu có dùng `{value, confidence, valid, validationMessage}`; field wrapper hoặc value có thể null. Confidence hiện từ parser heuristic, không phải confidence của engine ảnh.

Metadata: `status`, `overallConfidence`, `requiresHumanVerification=true`, `missingOrLowConfidenceFields`, `warnings`. Status enum hiện tại: `SUCCESS`, `LOW_CONFIDENCE_REVIEW_REQUIRED`, `UNREADABLE_DOCUMENT`, `INVALID_FILE`. **Giữ tên `requiresHumanVerification`; không thêm/đổi sang `needsHumanReview` trong C01.**

| Tình huống hiện tại | HTTP / body |
|---|---|
| Text parse được, confidence thấp hoặc unreadable | 200, `code=200`, draft status tương ứng; SUCCESS vẫn cần người xác minh |
| Multipart hợp lệ, thiếu/blank text | 200, `UNREADABLE_DOCUMENT`, confidence 0, missing `rawOcrText`; không gọi extractor |
| Multipart validator từ chối | 400, `code=400`, draft `INVALID_FILE`, warnings chứa lý do |
| JSON Bean Validation hoặc filename traversal | 400, `code=400`, `message`; local handler không có draft result |
| Multipart sai cấu trúc / vượt giới hạn servlet | **UNKNOWN** envelope cuối; không khẳng định luôn đi qua upload validator |

Ví dụ response của nhánh thiếu text, sau khi file đã qua validator; không phải bằng chứng OCR ảnh thành công:

```json
{
  "code": 200,
  "message": "Thành công",
  "result": {
    "fullName": null,
    "dateOfBirth": null,
    "gender": null,
    "nationality": null,
    "documentType": null,
    "documentNumber": null,
    "permanentAddress": null,
    "status": "UNREADABLE_DOCUMENT",
    "overallConfidence": 0.0,
    "requiresHumanVerification": true,
    "missingOrLowConfidenceFields": ["rawOcrText"],
    "warnings": ["Không nhận được văn bản OCR từ client scanner; endpoint này không tự nhận dạng nội dung ảnh."]
  }
}
```

## 2. Delta cho OCR-01/OCR-02 — PROPOSED, chưa có ở baseline

| Mục | Quyết định local đề xuất từ plan | Tương thích / gate |
|---|---|---|
| Engine | Tesseract local, `vie+eng`, không cloud/provider | Không thêm dependency/provider hoặc dùng shared storage |
| Feature flag | `hotel.ai.ocr.enabled=false` mặc định; hiện chưa có flag/adapter này | Flag false giữ text path và nhánh unreadable hiện tại; không báo ảnh đã OCR |
| Ảnh khi flag true | Multipart `file` được đọc thực sự; ảnh không cần client text; thêm binary input vào port nội bộ nếu cần | Không dùng 16 byte header làm toàn bộ ảnh; không đổi public route/DTO ngầm |
| Client text | Giữ compatibility cho text path; không tính request có text làm nghiệm thu OCR ảnh | Cần G1 chốt precedence khi request có cả ảnh và text; không trộn kết quả để che lỗi engine |
| Giới hạn | 5 MiB, tối đa 12000000 pixel sau decode, timeout engine 3 giây, tối đa 2 tác vụ engine đồng thời | Các ngưỡng pixel/timeout/semaphore chưa được implementation enforce; cần test boundary |
| Resource bounds | Cap stdout/stderr 1 MiB mỗi stream, đọc đồng thời để tránh deadlock; quá cap thì dừng process | Đề xuất local C01, phải review trước OCR-01; không truncate rồi báo SUCCESS |
| Confidence | TSV confidence thật, chuẩn hoá về [0,1], kết hợp validation; thiếu field phải báo warning | Không dùng heuristic cố định làm confidence ảnh; ngưỡng field 0.85 cần regression |
| Dependency lỗi | Thiếu binary/langdata, process lỗi/timeout/output quá cap: HTTP 503, `code=503`, generic message, không result thành công | **PROPOSED** error mapping, chưa có mã/enum OCR dependency riêng; G1 và review OCR-02 chốt trước đổi HTTP |
| Queue đầy | Không queue vô hạn; đề xuất HTTP 503 generic khi không nhận được slot | Chưa là contract đã triển khai; không thêm status enum công khai trong C01 |

Ảnh, text và draft chỉ trong request/memory; không file tạm, DB, object storage, log, trace hay eval chứa nội dung identity. Không log filename/arguments/output/stderr có PII. Chỉ synthetic/licensed fixtures; không gọi Gemini. Controller service không có write port; human review tại UI trước submit StayGuest riêng do Member 3 sở hữu. SUCCESS không cấp phép tự lưu hoặc tự verify.

## 3. Test oracle và giới hạn bằng chứng

| ID | Input / điều kiện | Expected outcome | Nguồn / task |
|---|---|---|---|
| O01 | JSON synthetic trong `frontend/e2e/api/ocr-synthetic.spec.ts` | HTTP 200, code 200, SUCCESS, human flag true; các field đúng assertion hiện hữu | Baseline Playwright; không phải ảnh OCR |
| O02 | JSON garbled text; tên traversal; thiếu text/filename; size 0 | Garbled: 200 UNREADABLE_DOCUMENT; các input sai: 400 | Playwright + `IdentityDocumentOcrControllerTest` |
| O03 | Valid multipart, không text, flag chưa có/false | 200 UNREADABLE_DOCUMENT, confidence 0, missing rawOcrText; extractor không gọi | Test `extractFile_missingRawOcrText_returnsUnreadableWithoutCallingExtractor` |
| O04 | MIME mismatch, traversal, file rỗng/quá 5 MiB/header sai | Validator fail; nếu đã vào controller: 400 INVALID_FILE; không gọi engine | `OcrUploadValidatorTest`, controller tests |
| O05 | Ảnh synthetic thật không kèm text, flag true | Engine đọc pixel, TSV/confidence và draft có thể được người kiểm tra; không SUCCESS giả | OCR-01, fixture chưa tạo trong C01 |
| O06 | 5 MiB/5 MiB+1; 12 MP/12 MP+1; ảnh decode lỗi; ảnh mờ | Boundary hợp lệ được xử lý; vượt cap/invalid bị từ chối; ảnh mờ cảnh báo/human review, không ép SUCCESS | OCR-01/OCR-02, PROPOSED |
| O07 | Thiếu engine/langdata, timeout, stdout/stderr quá cap, 3 request đồng thời | 503 mapping sau G1; tối đa 2 engine; process bị dừng/reap; không fallback giả | OCR-01, PROPOSED |
| O08 | Request/log capture và dependency spy | Không file/DB/business writes, không raw identity/text trong log; human flag luôn true | `U13ZeroWriteAndPiiSafetyTest` + bổ sung ảnh sau này |
| O09 | No JWT / sai role / client đổi hotelId | Security từ chối khi cần; hotelId không được dùng làm trusted auth; không đọc/ghi guest | Filter integration cần sandbox/G1; chưa được test standalone chứng minh |

Lệnh sau implementation, không thực thi trong C01: từ `backend/`, `.\mvnw.cmd "-Dtest=OcrUploadValidatorTest,IdentityDocumentOcrServiceTest,IdentityDocumentOcrControllerTest,U13ZeroWriteAndPiiSafetyTest" test`; thêm adapter/parser tests khi các class đã tồn tại. API test cần backend/Keycloak sandbox đã duyệt. Không tạo test giả hoặc nới assertion hiện có.

## 4. Gate, owner và điều kiện dừng

| Gate / owner | Decision hiện tại | Cần duyệt / stop condition |
|---|---|---|
| G1 — Member 3 + Member 1; Member 2 nếu đổi security/envelope chung | **BLOCKED integration**; chưa có sign-off trong handoff | Giữ multipart file/rawOcrText, human flag/mapping; precedence ảnh-text, lỗi engine, consent/retention. Thiếu quyết định thì dừng phần tích hợp/HTTP delta |
| G7 — Member 1 | **UNKNOWN** sandbox/binary/langdata ở lượt C01 | Xác minh Tesseract và fixture người đọc được; API/runtime chỉ sau khi đúng sandbox. Không startup Docker/migration để viết contract |

Rollout đề xuất: flag mặc định tắt; unit tests offline/synthetic trước; UI do Member 3 review riêng. Rollback implementation bằng tắt flag và tháo wiring adapter, giữ text API hiện có; C01 bị từ chối thì chỉ sửa tài liệu mới theo review, giữ lịch sử handoff. Không tuyên bố OCR ảnh hoặc check-in end-to-end đã PASS.
