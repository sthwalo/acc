Searched text for `class.*Depreciation` (`**/Depreciation*.java`), 11 results

Read [](file:///Users/sthwalonyoni/FIN/app/src/main/java/fin/service/DepreciationService.java#1-1), lines 1 to 101

Read [](file:///Users/sthwalonyoni/FIN/app/src/main/java/fin/service/DepreciationService.java#100-100), lines 100 to 1120

Read [](file:///Users/sthwalonyoni/FIN/app/src/main/java/fin/repository/DepreciationRepository.java#130-130), lines 130 to 1103

Based on your IAS 16 fiscal-year-based depreciation requirements, here are the **files that will be affected** and **how they will change**:

## **PRIMARY FILES - MAJOR CHANGES REQUIRED:**

### 1. **DepreciationRepository.java** - **Period Calculation Logic**
**Current Issue:** Uses calendar year boundaries (`acquisitionDate.plusYears(year.getYear() - 1)`)
**Required Changes:**
- Modify `saveDepreciationEntries()` to calculate periods based on fiscal year end (Feb 28/29)
- **Year 0**: Nov 2021 - Feb 2022 (4 months)
- **Year 1**: Mar 2022 - Feb 2023 (12 months)  
- **Year 2**: Mar 2023 - Feb 2024 (12 months)
- **Year 3**: Mar 2024 - Feb 2025 (12 months)
- **Year 4**: Mar 2025 - Feb 2026 (12 months)

### 2. **DepreciationService.java** - **Business Logic & Posting**
**Current Issues:**
- Year numbering starts at 1, needs to start at 0 for partial first year
- Posting logic finds current fiscal period depreciation by date overlap
- No handling for partial Year 0 depreciation amounts

**Required Changes:**
- **`calculateDepreciationSchedule()`**: Handle Year 0 as partial year (4/12 of annual depreciation)
- **`postDepreciationToJournal()`**: Update logic to identify which fiscal period year to post
- **`calculateAnnualDepreciation()`**: Apply partial year factor for Year 0
- **Year numbering**: 0, 1, 2, 3, 4 instead of 1, 2, 3, 4, 5

## **SECONDARY FILES - MINOR CHANGES:**

### 3. **DepreciationYear.java** - **Model Updates**
**Possible Changes:**
- Add fields for fiscal period start/end dates
- Add partial year factor (e.g., 4/12 for Year 0)

### 4. **DepreciationRequest.java** - **Request Model**
**Possible Changes:**
- Add fiscal year end date parameter
- Add company fiscal period configuration

### 5. **`FiscalPeriodRepository.java`** - **Enhanced Usage**
**Current Usage:** Already used to get current fiscal period
**Additional Usage:** May need methods to calculate fiscal year boundaries from acquisition date

## **DATABASE SCHEMA - POTENTIAL CHANGES:**

### 6. **`depreciation_entries` table** - **Schema Updates**
**Current Columns:** `period_start`, `period_end`, `fiscal_year`
**Potential Additions:**
- `fiscal_period_start` (Mar 1)
- `fiscal_period_end` (Feb 28/29)  
- `partial_year_factor` (4/12 for Year 0)

### 7. **`companies` table** - **Company Settings**
**Potential Addition:**
- `fiscal_year_end_month` (default: 2 for February)
- `fiscal_year_end_day` (default: 28/29)

## **IMPLEMENTATION SEQUENCE:**

1. **First**: Update `DepreciationRepository.saveDepreciationEntries()` to use fiscal year boundaries
2. **Second**: Update `DepreciationService` year numbering and partial year calculations  
3. **Third**: Update posting logic to work with fiscal periods
4. **Fourth**: Test with Computer asset (Nov 2021 acquisition)
5. **Fifth**: Verify Year 4 posts in current fiscal period (Mar 2025 - Feb 2026)

## **TESTING IMPACT:**
- **DepreciationServiceTest.java**: Will need updates for new year numbering
- **`DepreciationRepositoryTest.java`**: Will need updates for fiscal period calculations

**Total files requiring changes: 7+ files** with DepreciationRepository.java and DepreciationService.java being the most critical.
