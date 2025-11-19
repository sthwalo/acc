/*
 * FIN Financial Management System
 *
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * Contact: sthwaloe@gmail.com | +27 61 514 6185
 *
 * This source code is licensed under the Apache License 2.0.
 * Commercial use of the APPLICATION requires separate licensing.
 *
 * Contains proprietary algorithms and business logic.
 * Unauthorized commercial use is strictly prohibited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fin.repository;

import fin.model.DepreciationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for DepreciationSchedule entities
 */
@Repository
public interface DepreciationRepository extends JpaRepository<DepreciationSchedule, Long> {

    /**
     * Find depreciation schedules by company ID
     */
    List<DepreciationSchedule> findByCompanyId(Long companyId);

    /**
     * Find depreciation schedules by asset ID
     */
    List<DepreciationSchedule> findByAssetId(Long assetId);

    /**
     * Find depreciation schedule by company ID and schedule number
     */
    Optional<DepreciationSchedule> findByCompanyIdAndScheduleNumber(Long companyId, String scheduleNumber);

    /**
     * Find active depreciation schedules by company ID
     */
    List<DepreciationSchedule> findByCompanyIdAndStatus(Long companyId, String status);

    /**
     * Find depreciation schedules by depreciation method
     */
    List<DepreciationSchedule> findByDepreciationMethod(String depreciationMethod);

    /**
     * Find depreciation schedules by company ID and depreciation method
     */
    List<DepreciationSchedule> findByCompanyIdAndDepreciationMethod(Long companyId, String depreciationMethod);

    /**
     * Count depreciation schedules by company ID
     */
    long countByCompanyId(Long companyId);

    /**
     * Count active depreciation schedules by company ID
     */
    long countByCompanyIdAndStatus(Long companyId, String status);

    /**
     * Find depreciation schedules by useful life years
     */
    List<DepreciationSchedule> findByUsefulLifeYears(Integer usefulLifeYears);

    /**
     * Find depreciation schedules with remaining useful life
     */
    @Query("SELECT ds FROM DepreciationSchedule ds WHERE ds.companyId = :companyId AND ds.status = 'ACTIVE' AND ds.usefulLifeYears > 0")
    List<DepreciationSchedule> findActiveSchedulesWithRemainingLife(@Param("companyId") Long companyId);

    /**
     * Find depreciation schedules by schedule name containing (case insensitive)
     */
    List<DepreciationSchedule> findByScheduleNameContainingIgnoreCase(String scheduleName);
}