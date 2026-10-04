---
name: m1-data-processing-developer
description: Data pipelines, specialized file processing, OCR identity parsing, and time-series analytics standards for Member 1 (AI & Data Specialist). Use this skill when developing Utility meters (T04), Payroll import & BCA Lodging export (T05), AI OCR Passport/CCCD parser (U13), or Anomaly detection (U22).
---

# 📊 M1 Data Processing Developer (File Streams, BCA Police Export, OCR & Analytics)

This skill provides technical specifications for specialized external file handling (Excel/CSV/XML streams), OCR identity document parsing, and time-series anomaly detection algorithms for **Member 1 (AI & Data Specialist)**.

---

## 🎯 When to Activate This Skill

Activate when implementing data-intensive features:
* **T04**: Utility Module (Cumulative consumption calculation, meter readings tracking)
* **T05**: Payroll Summary Import (Streaming large Excel/CSV payroll sheets) & Lodging Queue Export (Vietnamese Police BCA lodging registration export)
* **U13**: AI OCR Passport/CCCD Parser (Automated identity extraction from captured guest documents)
* **U22**: AI Predictive Analytics & Anomaly Detection (Utility leakage detection & 30-day occupancy forecasting)

---

## 📁 1. External File Processing Standards (T05 Payroll & Lodging)

### A. Payroll Summary Import:
* **Memory Protection (OOM Prevention):**
  * Never load entire Excel `.xlsx` workbooks into RAM via `WorkbookFactory.create(file)`.
  * Use **Streaming Readers** (e.g., SAX-based `ExcelStreamingReader`, FastCSV) to process row-by-row.
* **Validation & Atomic Rollback:**
  * Validate each row: employee code existence, non-negative monetary amounts, valid payroll period format.
  * *All-or-Nothing Policy:* If fatal validation errors occur, abort the transaction and return a detailed report listing offending row numbers, columns, and validation error messages.
* **Batch Inserts:**
  * Use JDBC batching or `repository.saveAll()` chunked into batches of 100-500 records to reduce round-trips to PostgreSQL.

---

### B. Vietnamese Police Lodging Export (BCA Lodging Declaration):
* **Business Requirement:** Vietnamese lodging regulations require hotels to export a structured declaration of staying guests (`StayGuest`) for local police registration (BCA format - XML/CSV/Fixed-width text).
* **Mandatory Export Fields:**
  1. Personal Identification Number (12-digit CCCD or Passport Number)
  2. Full Name (Uppercase UTF-8, matching official identity card)
  3. Date of Birth (`YYYY-MM-DD` or `DD/MM/YYYY` per BCA format spec)
  4. Gender (`NAM` / `NU` or code equivalents)
  5. Nationality (ISO-3166 code or standardized country name)
  6. Assigned Room Number
  7. Exact Check-in and Expected Check-out timestamps
* **Data Security:** Lodging export contains sensitive PII. Generate files into secure temporary storage, transmit exclusively over HTTPS with `@PreAuthorize("hasAnyRole('ROLE_RECEPTIONIST', 'ROLE_PROPERTY_MANAGER')")`, and purge temporary files immediately after streaming to the client.

---

## 👁️ 2. OCR Identity Document Extraction (U13 AI OCR Parser)

When extracting text from guest identification cards (CCCD / Passport):

```text
[Captured Image] ──► [Image Preprocessing] ──► [Vision OCR Engine] ──► [Post-processing Normalizer] ──► [StayGuest DTO]
```

1. **Preprocessing Pipeline:**
   * Validate file size (max 10MB) and MIME type (`image/jpeg`, `image/png`, `image/webp`).
   * Deskew and rotate images to upright orientation; adjust contrast if underexposed.
2. **Entity Extraction:**
   * ID Number: 12 numeric digits for Vietnamese CCCD; alphanumeric for international passports.
   * Full Name: Normalize to clean uppercase (e.g., `NGUYEN VAN A`).
   * Date of Birth, Place of Origin (Quê quán), Permanent Residence (Nơi thường trú).
3. **Confidence Scoring:**
   * Each extracted field must carry a confidence score (`confidence >= 0.85`).
   * Flag low-confidence fields with `requires_manual_check = true` in the front-desk UI for visual verification.

---

## 📈 3. Time-Series & Anomaly Detection Algorithms (T04 & U22)

### A. Utility Anomaly Detection (Water/Power Leakage):
* **Consumption Calculation:**
  $$\Delta = Reading_{current} - Reading_{previous}$$
  * If $\Delta < 0$: Flag meter replacement or rollover error (throw validation exception).
* **Statistical Anomaly Threshold:**
  * Compare current consumption against historical rolling average ($\mu$) and standard deviation ($\sigma$):
    $$Z\text{-Score} = \frac{\Delta - \mu}{\sigma}$$
  * If $|Z\text{-Score}| > 2.5$ or consumption surges by $> 150\%$ without a corresponding increase in room occupancy rate, trigger a high-priority Utility Anomaly Alert.

### B. 30-Day Occupancy Forecasting:
* Ingest 12-24 months of historical booking data factoring in day-of-week seasonality and public holiday calendars.
* API Output Structure: Array of 30 days containing: `date`, `predictedOccupancyRate` (0.0 to 1.0), and confidence interval `[minRate, maxRate]`.
