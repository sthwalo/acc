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

import fin.model.DataCorrection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for data correction operations
 */
@Repository
public interface DataCorrectionRepository extends JpaRepository<DataCorrection, Long> {

    List<DataCorrection> findByCompanyIdOrderByCorrectedAtDesc(Long companyId);

    List<DataCorrection> findByTransactionIdOrderByCorrectedAtDesc(Long transactionId);

    List<DataCorrection> findByCorrectedByOrderByCorrectedAtDesc(String correctedBy);

    @Query("SELECT dc FROM DataCorrection dc WHERE dc.companyId = :companyId AND dc.correctedAt BETWEEN :startDate AND :endDate ORDER BY dc.correctedAt DESC")
    List<DataCorrection> findByCompanyIdAndCorrectedAtBetweenOrderByCorrectedAtDesc(
        @Param("companyId") Long companyId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(dc) FROM DataCorrection dc WHERE dc.companyId = :companyId")
    long countByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT COUNT(dc) FROM DataCorrection dc WHERE dc.correctedBy = :correctedBy")
    long countByCorrectedBy(@Param("correctedBy") String correctedBy);

    void deleteByCompanyId(Long companyId);
}