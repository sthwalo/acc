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

package fin.controller;

import fin.controller.ApiResponse;
import fin.dto.IndustryDto;
import fin.entity.Industry;
import fin.repository.IndustryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring REST Controller for industry management operations.
 */
@RestController
@RequestMapping("/api/v1/industries")
public class IndustryController {

    private final IndustryRepository industryRepository;

    public IndustryController(IndustryRepository industryRepository) {
        this.industryRepository = industryRepository;
    }

    /**
     * Get all active industries
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<IndustryDto>>> getIndustries() {
        try {
            List<Industry> industries = industryRepository.findByIsActiveTrue();
            List<IndustryDto> industryDtos = industries.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success(industryDtos));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Failed to retrieve industries: " + e.getMessage())
            );
        }
    }

    /**
     * Get industry by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<IndustryDto>> getIndustry(@PathVariable Long id) {
        try {
            Industry industry = industryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Industry not found"));
            IndustryDto industryDto = convertToDto(industry);
            return ResponseEntity.ok(ApiResponse.success(industryDto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Failed to retrieve industry: " + e.getMessage())
            );
        }
    }

    /**
     * Convert Industry entity to IndustryDto
     */
    private IndustryDto convertToDto(Industry industry) {
        return new IndustryDto(
            industry.getId(),
            industry.getDivisionCode(),
            industry.getName(),
            industry.getDescription(),
            industry.getCategory(),
            industry.getIsActive(),
            industry.getIsSarsCompliant()
        );
    }
}