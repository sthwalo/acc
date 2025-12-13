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

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Spring REST Controller for serving static assets like company logos.
 */
@RestController
@RequestMapping("/api/v1")
public class AssetController {

    /**
     * Serve company logo files
     * Handles database paths like "./input/Limelight/logo.png" or "input/XG/XG.png"
     */
    @GetMapping("/logos")
    public ResponseEntity<Resource> getLogo(@RequestParam String path) {
        try {
            // The path parameter contains the full path from the database
            // e.g., "./input/Limelight/logo.png" or "input/XG/XG.png"
            String logoPathStr = path;

            // Normalize the path - remove leading "./" if present
            if (logoPathStr.startsWith("./")) {
                logoPathStr = logoPathStr.substring(2);
            }

            // Determine base path based on environment
            // Priority: 1. Environment variable, 2. System property, 3. Docker default, 4. Current directory
            String basePath = System.getenv("FIN_BASE_PATH");
            if (basePath == null) {
                basePath = System.getProperty("fin.base.path");
            }
            if (basePath == null) {
                // Check if running in Docker container
                if (new File("/app").exists() && new File("/app/logos").exists()) {
                    basePath = "/app";
                } else {
                    // Use current working directory as fallback
                    basePath = System.getProperty("user.dir");
                }
            }

            // Construct full path
            logoPathStr = basePath + "/" + logoPathStr;

            System.out.println("DEBUG: Attempting to serve logo from path: " + logoPathStr);

            Path logoPath = Paths.get(logoPathStr);

            // Check if file exists
            if (!Files.exists(logoPath) || !Files.isRegularFile(logoPath)) {
                System.out.println("DEBUG: File does not exist or is not a regular file: " + logoPathStr);
                return ResponseEntity.notFound().build();
            }

            System.out.println("DEBUG: File found, serving: " + logoPathStr);

            // Create resource
            Resource resource = new FileSystemResource(logoPath);

            // Determine content type
            String contentType = determineContentType(path);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + path.substring(path.lastIndexOf('/') + 1) + "\"")
                    .body(resource);

        } catch (Exception e) {
            System.out.println("DEBUG: Exception in getLogo: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Determine MIME type based on file extension
     */
    private String determineContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();

        return switch (extension) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "svg" -> "image/svg+xml";
            case "ico" -> "image/x-icon";
            default -> "application/octet-stream";
        };
    }
}