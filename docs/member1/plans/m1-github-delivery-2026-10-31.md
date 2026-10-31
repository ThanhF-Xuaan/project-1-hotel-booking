# Member 1 — Review, fix và bàn giao GitHub

CONFIRMED — user ngày31/10/2026 cho phép implement → review → fix/refactor khi fail → review PASS → commit/push → PR → merge develop; cho phép sửa phần liên quan của team và không cần xin quyền member khác. Không giả peer approval hoặc dữ liệu thật. Bổ sung này thay trạng thái chưa được phép Git delivery trong các report lịch sử, không sửa lịch sử đó.

Preflight: origin/develop và HEAD cùng20e06e5135def269b91531b3997b784b271e17c8; 103file trong delivery manifest giữ nguyên hash, index trống. Tạo branch codex/m1-feature-completion-20261031 từ HEAD, giữ dirty work. RepoGitHub ThanhF-Xuaan/project-1-hotel-booking, tài khoản connector NDTrung36 có push. Không reset/clean/stash/rebase/force-push, không sửa repoD hoặc source worktree cũ.

Review bổ sung kiểm các trường hợp biên: policy giờ nhận/trả phòng mâu thuẫn ở dạng HHhmm/HH:mm, giờ tương đương9:00/09h00, giờ không hợp lệ, lấy nhầm giờ của chủ đề kế tiếp và guard tiếng Việt viết hoa Unicode. Những điểm này có thể làm kết quả grounded hoặc nhận diện dữ liệu nhạy cảm sai; cần reproducer trước khi đóng finding.

Allowlist delta: RagGroundedAnswerService.java, RagCorpusGovernanceValidator.java, test mới RagReviewRegressionTest.java; report review/delivery trong docs/member1/acceptance và append canonical handoff/plan. Không đổi DTO, gold32case, threshold, schema, Core/auth hoặc API/UI business. Sau targeted baseline/fix, chạy related RAG suites rồi full Verify trên đúng mã cuối. Giữ raw evidence của các lượt FAIL.

Khi PASS: kiểm diff/PII/frozen fixtures/allowlist, stage đúng file của task, commit/push branch, tạo PR base develop với review và test evidence. Chờ kiểm tra GitHub và xử lý finding có căn cứ; merge theo expected head SHA, không bypass branch protection hoặc giả approval. Sau merge fetch develop, đối chiếu tree/diff và append bằng chứng bàn giao. Data gates U22/utility/Group/format BCA giữ NOT READY; không bật mock provider hoặc tuyên bố production hoàn tất.

Review finding bổ sung: Git core.autocrlf=true đổi cả3fixture U22/Group khi checkout, khiến SHA oracle sai. Thêm .gitattributes giới hạn đúng3file ở hai thư mục fixture; giữ nguyên byte/hash và test. Git add + checkout-index trong repo tạm trước fix sai3/3hash, sau fix đúng3/3. Không stage source thật hoặc tạo commit để làm proof này. Allowlist thêm hai .gitattributes; artifact fixture-eol-verification.json.

F004 từ clone thật sau commit đầu: governance-coverage pin hash rawLF của eval/eval-set-t12-room-recommendation-v2-draft.md, Windows checkout đổi CRLF làm validator fail. Allowlist thêm eval/.gitattributes cho đúng file đó, không sửa source/gold/mapping/approval hoặc threshold. Kiểm lại bằng clone mới và validator reviewed-widget-sources; commit follow-up metadata/doc, không amend hoặc bỏ check. Full Java/runtime không thay nên giữ evidence Verify cuối, bổ sung proof checkout.
