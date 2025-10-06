# Quick Reference - Payroll Changes (Oct 6, 2025)

## 🎯 What's New

### 1. SDL (Skills Development Levy) ✅
- Automatically calculates 1% SDL on payroll
- Only applies when total payroll > R41,667/month
- Stored in database for SARS reporting

### 2. Reprocessing Payroll ✅
- Can now reprocess payroll periods without errors
- System automatically clears old data
- No need to create duplicate periods

---

## 🚀 Quick Start

### To Reprocess Payroll with SDL:
```bash
./run.sh
```
1. Choose "Payroll Management"
2. Select "Process Payroll"
3. Choose September 2025 (or any period)
4. System automatically:
   - Clears existing payslips
   - Recalculates with SDL
   - Generates new PDFs

**Expected Result:** All payslips now have SDL calculated

---

## 📊 Verification Queries

### Check SDL Values
```sql
SELECT payslip_number, gross_salary, sdl_levy 
FROM payslips 
WHERE payroll_period_id = 10;
```

### Check Total SDL
```sql
SELECT 
    SUM(gross_salary) as total_gross,
    SUM(sdl_levy) as total_sdl
FROM payslips
WHERE payroll_period_id = 10;
```

**Expected for Sept 2025:**
- Total Gross: R170,100
- Total SDL: R1,701.00

---

## ⚠️ Important Notes

1. **Reprocessing Deletes Old Data**
   - Old payslips permanently deleted
   - New payslips created immediately
   - Transaction ensures all-or-nothing

2. **PDF Files**
   - New PDFs generated automatically
   - Old PDF files remain on disk
   - Manually delete old PDFs if needed

3. **SDL Threshold**
   - SDL only applies if monthly payroll > R41,666.67
   - Current payroll (R170,100) exceeds threshold
   - SDL will be R1,701 per month

---

## 🔧 Troubleshooting

### Issue: "No open payroll periods"
**Solution:** Fixed! System now automatically allows reprocessing

### Issue: SDL showing as zero
**Possible Causes:**
1. Payroll not reprocessed yet → Reprocess payroll
2. Total payroll below threshold → Check total gross salary
3. Database column missing → Run: `\d payslips` to verify `sdl_levy` column exists

### Issue: Old totals showing
**Solution:** Reprocess the payroll period to update totals

---

## 📁 Documentation Files

- **SDL Details:** `/docs/SDL_IMPLEMENTATION_2025-10-06.md`
- **Reprocessing Details:** `/docs/PAYROLL_REPROCESSING_2025-10-06.md`
- **Full Summary:** `/docs/CHANGES_SUMMARY_2025-10-06.md`
- **This Card:** `/docs/QUICK_REFERENCE_2025-10-06.md`

---

## ✅ Testing Checklist

- [ ] Run application successfully
- [ ] Select September 2025 payroll
- [ ] Process payroll (no errors)
- [ ] Query database to verify SDL values
- [ ] Check PDFs generated
- [ ] Verify totals updated correctly

---

## 🎓 Key Concepts

**SDL (Skills Development Levy):**
- 1% of gross salary
- Employer-only levy
- Required by SARS
- Threshold: R500k/year (R41,667/month)

**Reprocessing:**
- Clears existing payslips
- Resets period status
- Recalculates everything
- Generates fresh PDFs
- All within single transaction

---

**Build Status:** ✅ Successful  
**Ready for:** User Testing  
**Next:** Verify SDL calculations correct
