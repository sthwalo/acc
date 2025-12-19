# Extraction Strategy: OCR-first + Hybrid Mode

📌 **Purpose**: This document records the design decision, rationale, implementation steps, testing strategy and rollout plan for using an OCR-first extraction pipeline with an optional PDFBox pass for high-quality selectable-text PDFs ("Hybrid" mode).

---

## TL;DR ✅
- **Default behavior**: OCR-first (rasterize → Tesseract at 200 DPI) for robustness.
- **Hybrid mode**: run a *quick PDFBox quality check* and, when PDFBox yields high-quality selectable text, prefer PDFBox output or merge it with OCR results.
- **Why**: OCR works on any document (scans or selectable text) and avoids PDFBox font-provider initialization failures on some environments while hybrid preserves PDFBox accuracy when available.

---

## Goals & Non-Goals
- **Goals**:
  - Centralize extraction in a single facade (`DocumentTextExtractor`).
  - Make extraction deterministic and robust across all upload types.
  - Preserve accuracy by preferring PDFBox when it clearly offers higher-quality text.
  - Keep the migration reversible (feature-flagged behavior).

- **Non-Goals**:
  - Remove PDFBox immediately from the codebase (we keep it as a fallback and for high-quality outputs until proven safe to remove).

---

## Benefits & Trade-offs
- **Benefits**:
  - Fewer runtime failure modes (PDFBox font-provider errors are avoided).
  - One primary extraction code path → easier maintenance, telemetry and debugging.
  - Deterministic fallback behavior for problem PDFs.

- **Trade-offs**:
  - OCR introduces substitution errors; requires heavier post-processing (date/amount/account normalization).
  - OCR is CPU & I/O heavy; deploy worker sizing and queueing accordingly.

---

## Decision Flow (high level)
1. **OCR-first**: Always run OCR extraction (`ocrLines`) using `pdftoppm` → Tesseract (200 DPI). Score OCR quality.
2. **Quick PDFBox check**: Run a light PDFBox check on sampled pages (1, mid, last or first 3 pages) to compute `pdfQuality`.
3. **Choose output**:
   - If `pdfQuality >= PDF_THRESHOLD` → run full PDFBox extraction and prefer/merge `pdfLines`.
   - Else if `ocrQuality >= OCR_THRESHOLD` → accept `ocrLines`.
   - Else → try fallback steps (higher DPI, re-run OCR, mark for human review).
4. **Record** both raw outputs and quality scores to logs/metrics for audit & threshold tuning.

---

## Heuristics and Example Thresholds
- **PDFBox scoring (example)**:
  - totalNonEmptyLines >= 20 → +2
  - shortLinePercentage < 50% → +1
  - amountLineCount >= 5 → +1
  - financialTermCount >= 3 → +1
  - **pdfQuality >= 4** => "high-quality selectable text"

- **OCR scoring (example)**:
  - linesCount >= 20 → +1
  - statement period parsed → +2
  - account number parsed → +2
  - **ocrQuality >= 4** => confident OCR parsing

> Note: These thresholds are suggestions — tune using the golden dataset.

---

## Implementation plan (code-level)

### Config & Flags
- Add config key or enum:
```properties
# application.properties
extraction.mode=HYBRID  # values: OCR_FIRST | HYBRID | PDFBOX
```
- Implement `ExtractionMode` enum in code.

### Main changes in code
- Centralize logic in `DocumentTextExtractor`:
  - New methods to add:
    - `ExtractionMode getMode()` (config-backed)
    - `QuickPdfCheckResult quickPdfBoxQualityCheck(File pdf, int samplePages)`
    - `List<String> extractPdfBoxFull(File pdf)`
    - `int scorePdfQuality(QuickPdfCheckResult)`
    - `int scoreOcrQuality(List<String> ocrLines, Metadata metadata)`
    - `List<String> mergePreferred(List<String> pdfLines, List<String> ocrLines)`
  - Existing `extractWithOCR(...)` will be used as the canonical OCR path (200 DPI)
  - Decision logic pseudocode:
```java
if (mode == PDFBOX) return extractPdfBoxFull(pdf);
List<String> ocrLines = extractWithOCR(...);
int ocrScore = scoreOcrQuality(ocrLines...);
if (mode == OCR_FIRST && ocrScore >= OCR_CONFIDENCE) return ocrLines;
QuickPdfCheckResult pdfCheck = quickPdfBoxQualityCheck(pdf);
if (pdfCheck.quality >= PDF_CONFIDENCE) {
    List<String> pdfLines = extractPdfBoxFull(pdf);
    return mergePreferred(pdfLines, ocrLines);
}
if (ocrScore >= OCR_CONFIDENCE) return ocrLines;
// fallback: higher DPI or human review
```

### Files likely affected
- `app/src/main/java/fin/service/upload/DocumentTextExtractor.java` (primary)
- `app/src/main/java/fin/config/PdfBoxConfigurator.java` (reads mode or marks pdfbox availability)
- `app/src/main/java/fin/controller/ImportController.java` (debug endpoints will return both outputs for analysis)
- Tests in `app/src/test/java/fin/service/upload/`

---

## Tests & Validation
- **Golden dataset**: collect representative PDFs (scans, selectable text, multi-bank templates).
- **Automated comparisons**: run OCR-only, PDFBox-only and Hybrid across dataset and compute:
  - statement-period parse rate
  - account number exact-match rate
  - false positive transaction lines
- **Unit tests**:
  - `quickPdfBoxQualityCheck` behavior
  - `scorePdfQuality` thresholds
  - `mergePreferred` merging logic
- **Integration tests**:
  - E2E test that asserts Hybrid chooses PDFBox on high-quality PDFs
  - OCR fallback test when `PdfBoxConfigurator` marks PDFBox unavailable

---

## Observability & Metrics
- Per-job metrics to emit:
  - `extraction.mode` (HYBRID/OCR_FIRST/PDFBOX)
  - `pdfCheckMs`, `pdfFullMs`, `ocrMs`
  - `pdfQuality`, `ocrQuality`
  - `parseSuccess` booleans (statementPeriod/accountNumber)
  - `externalRasterizerUsed` boolean
- Persist sample raw outputs and parsed metadata for auditing (redact PII in logs)

---

## CI / Build & Docker considerations
- Ensure `pdftoppm` (poppler) and `tesseract` are present in dev/CI images.
- Add a CI health check that fails the build if `extraction.mode` is enabled but system binaries are missing.
- Update developer docs and onboarding for local installs (`brew install poppler tesseract` etc.)

---

## Benchmark plan (minimal)
- For representative set of PDFs, measure:
  - ms/page at 200 vs 300 DPI
  - memory usage per page
  - CPU-seconds per page
  - parse success rates
- Adjust worker sizing and DPI based on the results.

---

## Rollout & Migration plan
1. Implement HYBRID behind config flag (default `HYBRID` if you want to test), but allow switching at runtime.
2. Run offline benchmarks and tune thresholds.
3. Canary on a small subset of traffic or tenant(s) (or run as passive collection to compare outputs without switching behavior).
4. If metrics acceptable, increase traffic percentage.
5. When stable, consider removing or deprecating old PDFBox-only paths.

---

## Acceptance Criteria (examples)
- On golden dataset, hybrid mode meets:
  - Statement period parse ≥ 98%
  - Account number parsed with ≥ 99% exact match
  - Median CPU time per page within budget at 200 DPI

---

## Security & Privacy
- Treat temporary rasterized images as sensitive. Use ephemeral temp locations with restrictive permissions and immediate cleanup.
- Avoid persisting PII in logs. Where sample outputs are kept for debugging, redact PII or store in a restricted location only.

---

## Next steps / Action items
- Create `ExtractionMode` enum + config and add to `DocumentTextExtractor`.
- Implement `quickPdfBoxQualityCheck()` and `mergePreferred()` helpers.
- Add tests and the golden dataset runner.
- Update Docker images / CI to include `pdftoppm` and `tesseract`.

---

If you'd like, I can now implement the code scaffolding (flag, quick check and decision logic) and add the first round of tests and the golden dataset runner. Say **"yes, implement"** and I will start by adding the config enum and scaffolding in `DocumentTextExtractor` and tests.


Great — I’ll outline a concrete hybrid design (OCR-first + optional PDFBox pass), include exact heuristics, code-level changes, testing and rollout steps, and expected performance trade-offs. Next I'll show decision flow, thresholds, API/config changes, and a small pseudocode snippet you can drop into `DocumentTextExtractor`.## Summary — what we’ll do ✅
- Implement **Hybrid (OCR-first) mode**: always run OCR (pdftoppm → Tesseract at 200 DPI), but run a lightweight PDFBox check and, when PDFBox yields *high-quality selectable text*, prefer the PDFBox result (or merge outputs) for higher accuracy.  
- Keep this behavior **configurable** and feature-flagged so we can benchmark and rollback easily.

---

## Decision flow (high level) 🔁
1. Always perform OCR extraction first (fast, robust). Produce `ocrLines`, metadata (statement period, account no) and a quality score (ocrQuality).  
2. Run a lightweight PDFBox *quality check* (sample first N pages or full doc only if cheap) returning `pdfQuality` and `pdfLines` (if high-quality).  
3. Decision:
   - If `pdfQuality >= pdfThreshold` → prefer PDFBox output (use pdfLines).  
   - Else if `ocrQuality >= ocrThreshold` → accept OCR.  
   - Else (both low) → either try more expensive rescans (higher DPI), combine outputs heuristically (e.g., use PDFBox where matches are exact), or mark for human review.  
4. Record metrics and keep both raw outputs for auditing.

---

## Concrete heuristics & thresholds (recommended) ⚖️
- Quick PDFBox check: strip text only from first 3 pages (or sample pages: 1, mid, last) to avoid expensive full render.  
- pdfQuality scoring (example):
  - totalNonEmptyLines >= 20 → +2
  - shortLinePercentage < 50% → +1
  - amountLineCount >= 5 → +1
  - financialTermCount >= 3 → +1  
  - pdfQuality >= 4 → treat as “high-quality selectable text”
- ocrQuality scoring (example):
  - linesCount >= 20 → +1
  - parsed statement period found → +2
  - parsed account number found → +2  
  - ocrQuality >= 4 → accept OCR confidently
- Use a `delta` (e.g., 1-2 points) when comparing pdfQuality vs ocrQuality if both available.

Adjust thresholds after running the golden dataset.

---

## Implementation notes & minimal code changes 🔧
- Add config / enum:
  - `ExtractionMode { OCR_FIRST, HYBRID, PDFBOX }`
  - `extraction.mode=HYBRID` in application.properties (default)
- In `DocumentTextExtractor.parseDocument(File pdf)`:
  - Always run OCR first (current `extractWithOCR` path).
  - Add `quickPdfBoxQualityCheck(File pdf)` (read text with PDFTextStripper from first N pages).
  - If mode == HYBRID and `pdfQuality >= threshold`, run full PDFBox extraction and use pdfLines; otherwise use OCR output.
  - Optionally, implement `mergeOutputs(List<String> pdfLines, List<String> ocrLines)` to prefer exact matches from PDFBox for items like accountNumber, dates, and fill gaps from OCR.
- Persist raw `ocrRaw`, `pdfRaw` to debug logs / storage for audit and troubleshooting (redact PII in logs).
- Add robust metrics: `extraction.mode`, `ocr.time_ms`, `pdf.time_ms`, `pdf.quality`, `ocr.quality`, `parse.success` (statementPeriod/accountNumber).

Pseudo-snippet:
```java
if (mode == PDFBOX) return extractPdfBoxFull(pdfFile);

List<String> ocrLines = extractWithOCR(...); // existing
int ocrScore = scoreOcr(ocrLines);

if (mode == OCR_FIRST) {
   if (ocrScore >= OCR_CONFIDENCE) return ocrLines;
   // else optionally attempt PDFBox
}

if (mode == HYBRID) {
   PdfCheckResult pdfCheck = quickPdfBoxQualityCheck(pdfFile);
   if (pdfCheck.quality >= PDF_THRESHOLD) {
       List<String> pdfLines = extractPdfBoxFull(pdfFile);
       return selectPreferred(pdfLines, ocrLines);
   }
   return ocrLines;
}
```

---

## Testing & validation plan 🧪
- **Golden dataset**: build a dataset with representative PDFs (selectable text, scans, banks, multi-page).
- **Automated comparisons**:
  - Run OCR-only vs PDFBox-only vs Hybrid for the dataset and compute:
    - statement period parse rate,
    - account number exact match rate,
    - number of false/incorrect transaction lines.
- **Unit tests**:
  - Test `quickPdfBoxQualityCheck()` logic and thresholds.
  - Test `selectPreferred()` merging logic (prefer PDFBox exact match for critical fields).
- **Integration tests**:
  - Ensure fallback works when PDFBox fails (font-provider errors).
  - End-to-end test with pdftoppm/tesseract installed (skip where not available).
- **Acceptance criteria**: meet chosen thresholds (e.g., statement period parsed ≥ 98% across dataset for Hybrid).

---

## Performance & resource impact (what to expect) 📊
- Running a **lightweight PDFBox check** (first N pages) adds small CPU cost but is negligible vs full OCR.  
- Running full PDFBox extraction for many files adds only modest CPU and is typically much cheaper than OCR (PDFBox text extraction is cheap compared to OCR rendering).  
- Hybrid avoids frequent full PDFBox when PDFs are scanned-only, so it’s a good balance: you only pay PDFBox cost when it's likely to be helpful.

---

## Observability & operational controls 📈
- Metrics to add:
  - extraction.mode (per job)
  - pdfQuality, ocrQuality
  - times: pdfCheckMs, pdfFullMs, ocrMs
  - parseSuccess boolean
- Feature flag / config to toggle modes and thresholds at runtime.
- Keep both raw outputs for audits and to retrain thresholds.

---

## Rollout plan (safe & incremental) 🛡️
1. Implement HYBRID behind config default `HYBRID` but keep `OCR_FIRST` and `PDFBOX` (switchable).
2. Add golden dataset & run offline benchmarking to tune thresholds.
3. Canary: enable HYBRID for small % of traffic or a specific tenant; monitor metrics.
4. If metrics look good, expand to all traffic; otherwise tune preprocessing & thresholds.
5. After stable period, consider cleaning up unused code or making HYBRID default.

---

## Recommended immediate next step ✅
- I can implement the `ExtractionMode` + `quickPdfBoxQualityCheck()` and add the decision logic to `DocumentTextExtractor`, plus tests and basic metrics. Then we run the golden dataset comparison (automated) and tune thresholds.

Would you like me to implement the changes now and add the benchmark job for the golden dataset?