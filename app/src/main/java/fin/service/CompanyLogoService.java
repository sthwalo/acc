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
package fin.service;

import fin.model.Company;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing company logos across the entire system.
 * Handles logo upload, validation, storage, and retrieval for use in
 * payslips, reports, invoices, and other company-branded documents.
 */
public class CompanyLogoService {
    private final CompanyService companyService;

    // Supported image formats
    private static final List<String> SUPPORTED_FORMATS = Arrays.asList(
        "png", "jpg", "jpeg", "gif", "bmp", "svg"
    );

    // Maximum file size (5MB)
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    // Logo storage directory
    private static final String LOGO_DIRECTORY = "logos";

    public CompanyLogoService(String dbUrl) {
        this.companyService = new CompanyService(dbUrl);
        try {
            ensureLogoDirectoryExists();
        } catch (Exception e) {
            System.err.println("Warning: Failed to initialize logo directory: " + e.getMessage());
            // Don't throw exception from constructor - allow service to be created
            // Logo operations will fail gracefully if directory doesn't exist
        }
    }

    /**
     * Upload and set a company logo
     * @param companyId The company ID
     * @param logoFilePath Path to the logo file to upload
     * @return Updated company with logo path set
     * @throws IOException If file operations fail
     * @throws IllegalArgumentException If validation fails
     */
    public Company uploadCompanyLogo(Long companyId, String logoFilePath)
            throws IOException, IllegalArgumentException {

        // Validate inputs
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID cannot be null");
        }
        if (logoFilePath == null || logoFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Logo file path cannot be null or empty");
        }

        // Get company
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new IllegalArgumentException("Company not found with ID: " + companyId);
        }

        // Validate logo file
        Path sourcePath = Paths.get(logoFilePath);
        validateLogoFile(sourcePath);

        // Generate unique filename
        Path sourceFileName = sourcePath.getFileName();
        if (sourceFileName == null) {
            throw new IllegalArgumentException("Invalid logo file path: no filename component");
        }
        String fileExtension = getFileExtension(sourceFileName.toString());
        String uniqueFileName = generateUniqueFileName(companyId, fileExtension);

        // Copy file to logo directory
        Path targetPath = Paths.get(LOGO_DIRECTORY, uniqueFileName);
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

        // Update company with logo path
        company.setLogoPath(targetPath.toString());
        return companyService.updateCompany(company);
    }

    /**
     * Get the logo path for a company
     * @param companyId The company ID
     * @return Logo file path, or null if no logo set
     */
    public String getCompanyLogoPath(Long companyId) {
        Company company = companyService.getCompanyById(companyId);
        return company != null ? company.getLogoPath() : null;
    }

    /**
     * Remove a company's logo
     * @param companyId The company ID
     * @return Updated company with logo path cleared
     */
    public Company removeCompanyLogo(Long companyId) {
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new IllegalArgumentException("Company not found with ID: " + companyId);
        }

        // Delete physical file if it exists
        if (company.getLogoPath() != null) {
            try {
                Files.deleteIfExists(Paths.get(company.getLogoPath()));
            } catch (IOException e) {
                System.err.println("Warning: Could not delete logo file: " + company.getLogoPath());
            }
        }

        // Clear logo path in database
        company.setLogoPath(null);
        return companyService.updateCompany(company);
    }

    /**
     * Check if a company has a logo
     * @param companyId The company ID
     * @return true if company has a logo set
     */
    public boolean hasCompanyLogo(Long companyId) {
        String logoPath = getCompanyLogoPath(companyId);
        return logoPath != null && !logoPath.trim().isEmpty();
    }

    /**
     * Validate logo file format and size
     */
    private void validateLogoFile(Path filePath) throws IllegalArgumentException, IOException {
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("Logo file does not exist: " + filePath);
        }

        if (!Files.isRegularFile(filePath)) {
            throw new IllegalArgumentException("Path is not a regular file: " + filePath);
        }

        // Check file size
        long fileSize = Files.size(filePath);
        if (fileSize > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                String.format("Logo file too large: %d bytes (max: %d bytes)",
                    fileSize, MAX_FILE_SIZE));
        }

        // Check file extension
        Path fileNamePath = filePath.getFileName();
        if (fileNamePath == null) {
            throw new IllegalArgumentException("Invalid file path: no filename component");
        }
        String fileName = fileNamePath.toString().toLowerCase();
        String extension = getFileExtension(fileName);
        if (!SUPPORTED_FORMATS.contains(extension)) {
            throw new IllegalArgumentException(
                "Unsupported logo format: " + extension +
                ". Supported formats: " + String.join(", ", SUPPORTED_FORMATS));
        }
    }

    /**
     * Generate a unique filename for the logo
     */
    private String generateUniqueFileName(Long companyId, String extension) {
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("company_%d_logo_%s.%s", companyId, uuid, extension);
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1).toLowerCase();
    }

    /**
     * Ensure the logo directory exists
     */
    private void ensureLogoDirectoryExists() {
        try {
            Path logoDir = Paths.get(LOGO_DIRECTORY);
            if (!Files.exists(logoDir)) {
                Files.createDirectories(logoDir);
                System.out.println("✅ Created logo directory: " + logoDir.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Error creating logo directory: " + e.getMessage());
            throw new RuntimeException("Failed to create logo directory", e);
        }
    }

    /**
     * Get supported logo formats
     */
    public List<String> getSupportedFormats() {
        return SUPPORTED_FORMATS;
    }

    /**
     * Get maximum allowed file size
     */
    public long getMaxFileSize() {
        return MAX_FILE_SIZE;
    }
}