/*
 * FIN Financial Management System
 *
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * Contact: sthwaloe@gmail.com | +27 61 514 6185
 *
 * Licensed under Apache License 2.0 - Commercial use requires separate licensing
 */

package fin.repository;

import fin.entity.RuleTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for RuleTemplate entities.
 * Provides access to industry-specific transaction mapping rule templates.
 */
@Repository
public interface RuleTemplateRepository extends JpaRepository<RuleTemplate, Long> {

    /**
     * Find all rule templates for a specific industry
     */
    List<RuleTemplate> findByIndustryIdOrderByPriorityDesc(Long industryId);

    /**
     * Find all active rule templates for a specific industry
     */
    @Query("SELECT rt FROM RuleTemplate rt WHERE rt.industry.id = :industryId AND rt.isDefaultEnabled = true ORDER BY rt.priority DESC")
    List<RuleTemplate> findActiveByIndustryId(@Param("industryId") Long industryId);

    /**
     * Find rule templates by industry and category
     */
    List<RuleTemplate> findByIndustryIdAndCategoryOrderByPriorityDesc(Long industryId, String category);

    /**
     * Count rule templates for an industry
     */
    long countByIndustryId(Long industryId);
}