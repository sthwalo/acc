package fin.entity;

import java.time.LocalDate;

/**
 * Projection class for fiscal period summary with company information.
 * Used in FiscalPeriodRepository.findAllFiscalPeriodsWithCompanyInfo()
 */
public class FiscalPeriodSummary {

    private final String companyName;
    private final Long fiscalPeriodId;
    private final String periodName;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final Boolean isClosed;

    public FiscalPeriodSummary(String companyName, Long fiscalPeriodId, String periodName,
                              LocalDate startDate, LocalDate endDate, Boolean isClosed) {
        this.companyName = companyName;
        this.fiscalPeriodId = fiscalPeriodId;
        this.periodName = periodName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isClosed = isClosed;
    }

    public String getCompanyName() {
        return companyName;
    }

    public Long getFiscalPeriodId() {
        return fiscalPeriodId;
    }

    public String getPeriodName() {
        return periodName;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Boolean getIsClosed() {
        return isClosed;
    }
}