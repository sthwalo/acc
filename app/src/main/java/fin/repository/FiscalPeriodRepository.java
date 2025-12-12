package fin.repository;

import fin.entity.FiscalPeriod;
import fin.entity.FiscalPeriodSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FiscalPeriodRepository extends JpaRepository<FiscalPeriod, Long> {

    /**
     * Find all fiscal periods for a specific company
     */
    List<FiscalPeriod> findByCompanyId(Long companyId);

    /**
     * Find all active (non-closed) fiscal periods for a specific company
     */
    List<FiscalPeriod> findByCompanyIdAndIsClosedFalse(Long companyId);

    /**
     * Find fiscal periods that contain a specific date for a company
     */
    @Query("SELECT fp FROM FiscalPeriod fp WHERE fp.companyId = :companyId AND fp.startDate <= :date AND fp.endDate >= :date")
    List<FiscalPeriod> findByCompanyIdAndDateRange(@Param("companyId") Long companyId, @Param("date") LocalDate date);

    /**
     * Find fiscal periods within a date range for a company
     */
    @Query("SELECT fp FROM FiscalPeriod fp WHERE fp.companyId = :companyId AND ((fp.startDate <= :endDate AND fp.endDate >= :startDate))")
    List<FiscalPeriod> findByCompanyIdAndDateRangeBetween(@Param("companyId") Long companyId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find the current fiscal period for a company (active period containing today's date)
     */
    @Query("SELECT fp FROM FiscalPeriod fp WHERE fp.companyId = :companyId AND fp.isClosed = false AND fp.startDate <= CURRENT_DATE AND fp.endDate >= CURRENT_DATE")
    Optional<FiscalPeriod> findCurrentPeriodByCompanyId(@Param("companyId") Long companyId);

    /**
     * Check if a fiscal period exists for a company with the given name
     */
    boolean existsByCompanyIdAndPeriodName(Long companyId, String periodName);

    /**
     * Find fiscal periods by company ID ordered by start date descending
     */
    List<FiscalPeriod> findByCompanyIdOrderByStartDateDesc(Long companyId);

    /**
     * Delete fiscal periods by company ID
     */
    void deleteByCompanyId(Long companyId);

    /**
     * Get all fiscal periods with company information for frontend display
     */
    @Query("SELECT new fin.entity.FiscalPeriodSummary(c.name, fp.id, fp.periodName, fp.startDate, fp.endDate, fp.isClosed) " +
           "FROM Company c LEFT JOIN FiscalPeriod fp ON c.id = fp.companyId " +
           "ORDER BY c.name, fp.startDate")
    List<FiscalPeriodSummary> findAllFiscalPeriodsWithCompanyInfo();
}