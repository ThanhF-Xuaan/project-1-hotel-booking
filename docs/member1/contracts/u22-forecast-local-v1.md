# U22-02 — Forecast/backtest local v1, 31/10/2026

## Quyết định trước implementation và test

CONFIRMED: user cho phép tự quyết và triển khai phần Member 1/team liên quan. Base vừa fetch `origin/develop@20e06e5135def269b91531b3997b784b271e17c8`; managed worktree mới m1-u22-forecast sạch trước task, nhập nguyên patch U22-01 gồm13file. Source U22-01 và T12 ở worktree cũ được bảo toàn. Không giả Member 2 đã xác nhận dữ liệu thật.

CONFIRMED: plan canonical U22-02 yêu cầu seasonal-naive7, ridge trend/weekday lambda1, 5 rolling folds ×30ngày, train84–180ngày, MAE gate và residual range. Contract U22-01 đã có history port/service và fixture840row synthetic. UNKNOWN: capacity/actual history thật. Chỉ local/synthetic trong slice này; không business API, Spring bean, IAM/Core adapter, DB, UI hoặc live model.

Các lựa chọn chi tiết dưới đây được chốt theo ủy quyền user **trước lần chạy test/evaluation đầu**, không chỉnh threshold sau khi xem metric. Đơn vị dự báo/MAE/radius là điểm phần trăm occupancy, không phải số phòng hay giá.

## Input và bounded behavior

- Forecast service gọi OccupancyHistoryService đã authorize, không đọc repository. Giữ nguyên status deny/invalid/dependency failure; lỗi không có forecast hoặc provenance giả. Không fallback synthetic khi nguồn lỗi.
- History hợp lệ: một hotel, tối đa420ngày, đủ row theo calendar inclusive, không duplicate; sort date tăng dần. MISSING/capacity0 không tạo target và không được xóa calendar position hoặc điền0. Kiểm metadata window/hotel/mode/asOf. Ngày cuối phải là yesterday theo asOf ở Asia/Ho_Chi_Minh; stale history không kéo thành forecast tương lai giả.
- Forecast30ngày liên tiếp từ end+1, giới hạn point/lower/upper trong[0; 100]. Không nhân tỷ lệ với current room count hoặc sửa Pricing/Inventory.
- Final fit chỉ dùng180calendar days gần nhất, cần ít nhất84target hợp lệ và đủ7ngày cuối cho baseline. Không đủ trả INSUFFICIENT_COVERAGE, không tạo forecast. Không nhận horizon/lambda từ client.

## Model và cách chọn — đã chốt

- Baseline seasonal-naive7: dùng đúng target của7calendar days cuối, lặp theo weekday cho30ngày. Không cập nhật bằng actual tương lai hoặc lấp gap bằng ngày khác.
- Candidate ridge: intercept, trend `(date - trainStart)/180`, sáu dummy Tue..Sun, Monday là reference. Lambda=1 cho tất cả coefficient trừ intercept. Fit trên target hợp lệ trong training window; không impute label. Giải hệ8×8 bằng partial pivot, không thêm dependency. Prediction clamp[0; 100] trước scoring.
- N là số calendar days của history. 5fold không chồng test: origin N-150,N-120,N-90,N-60,N-30; mỗi test30ngày. Train `[max(0,origin-180),origin)`, ít nhất84calendar days và84usable targets, đủ7ngày cuối. Mỗi test cần ít nhất24usable targets/30; chấm baseline/candidate trên cùng ngày hợp lệ. Không fit/scaling/calibration từ test của chính fold.
- Thiếu một fold hợp lệ thì backtest INSUFFICIENT_COVERAGE: không aggregate/select bằng số fold ít hơn5. Final baseline fallback vẫn được phép nếu final window đạt84usable/last7 và có ít nhất28causal weekly residuals để tạo range.
- Đủ5fold: MAE tổng là tổng absolute error chia tổng số target thực được chấm, không trung bình không trọng số giữa fold. Chọn ridge chỉ khi aggregate MAE <=baseline và từng fold MAE <=baseline+5điểm; epsilon1e-9 chỉ cho sai số số thực. Nếu không, giữ seasonal baseline. Không chọn model theo final fixture metric ngoài gate này.
- Range khi backtest đủ: radius=nearest-rank p90 của absolute errors out-of-sample của model đã chọn qua5fold. Đây là empirical error range, không confidence/prediction interval đã calibration hoặc bảo đảm90% coverage. Final fit dùng history đã biết tới asOf; không có holdout độc lập sau model selection.
- Fallback chưa đủ5fold: seasonal baseline, radius từ absolute error `y[t]-y[t-7]` với cả hai target quan sát trong final window, tối thiểu28pairs. Nhãn range khác rõ ràng với rolling backtest. Không có MAE five-fold; không báo MAE=0.

## Output và kiểm chứng

Output ghi status FORECAST_READY / BASELINE_FALLBACK / INSUFFICIENT_COVERAGE / INVALID_REQUEST / ACCESS_DENIED / DEPENDENCY_UNAVAILABLE, source mode, asOf, model/version, reason,30points và range method/radius. Backtest summary có status,5fold dates/train/test counts/MAEs và aggregate MAE/scored count khi đủ. Fold traces dùng để test causality, không đưa daily labels vào response.

Allowlist mới: `forecast/dto/OccupancyForecastResponse.java`; `forecast/service/OccupancyModels.java`, `ForecastBacktestEvaluator.java`, `OccupancyForecastService.java`; tests `OccupancyForecastServiceTest.java`, `ForecastBacktestEvaluatorTest.java`, `OccupancyForecastFixtureTest.java`; contract này và report `docs/member1/acceptance/u22-02-forecast-2026-10-31.md`. Nhập nguyên13file U22-01; không sửa source/test/fixture/gold cũ. Fixture dùng lại occupancy-history-420-days.json, SHA256 `8c1f3e81019238948be47a61c49ae4821b92752381a7d99d70216e8437651c20`, không tạo bản sao dễ lệch.

AC:30date/model/MAE/range; gate equality/aggregate/per-fold; weighted MAE; deterministic ordering; calendar gaps/capacity0; no future leakage bằng mutation test heldout labels và trace predictions; bounded/invalid/cross-hotel/stale input; denied/outage không có forecast; low-data baseline/insufficient không giả metric; fixture mode/hash giữ nguyên; architecture không Core/write/production fixture wiring.

Lệnh targeted Maven: `-Dtest=OccupancyForecastServiceTest,ForecastBacktestEvaluatorTest,OccupancyForecastFixtureTest`; regression thêm50test U22-01, personalization và T12 zero-write architecture. Evidence gồm actual Surefire counts/metric synthetic, hash before/after, diff/allowlist/preservation và portable patches. Không full runtime/production, không khẳng định toàn U22/SRS hoàn tất. Rollback: không wire service mới; chưa có DB side effect hoặc process cần cleanup. Chưa stage/commit/push/merge trong slice này.
