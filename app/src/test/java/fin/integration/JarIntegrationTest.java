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

package fin.integration;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JAR-First Integration Test
 *
 * Tests the actual JAR file (production artifact) to ensure JAR-first consistency.
 * This test builds the JAR, starts it, tests API endpoints, then stops it.
 *
 * This represents true integration testing in a JAR-first architecture.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JarIntegrationTest {

    private static Process jarProcess;
    private static final int API_PORT = 8080;
    private static final String JAR_PATH = "app/build/libs/app.jar";
    private static final String BASE_URL = "http://localhost:" + API_PORT + "/api/v1";

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void setUpJar() throws Exception {
        System.out.println("🔨 Building JAR for integration testing...");

        // Build the JAR
        ProcessBuilder buildPb = new ProcessBuilder("./gradlew", "build", "-x", "test", "--no-daemon");
        buildPb.inheritIO();
        Process buildProcess = buildPb.start();
        boolean buildSuccess = buildProcess.waitFor(5, TimeUnit.MINUTES);

        assertTrue(buildSuccess, "JAR build should succeed");
        assertTrue(new File(JAR_PATH).exists(), "JAR file should exist at " + JAR_PATH);

        System.out.println("✅ JAR built successfully");

        // Set up test database schema before starting JAR
        System.out.println("🗄️ Setting up test database schema...");
        try {
            fin.TestConfiguration.setupTestDatabase();
            System.out.println("✅ Test database schema initialized");
        } catch (Exception e) {
            System.err.println("❌ Failed to set up test database: " + e.getMessage());
            throw e;
        }

        // Set environment variables for the JAR
        ProcessBuilder pb = new ProcessBuilder("java", "-Dfin.license.autoconfirm=true", "-jar", JAR_PATH, "api");
        pb.environment().put("TEST_DATABASE_URL", System.getProperty("TEST_DATABASE_URL", "jdbc:postgresql://localhost:5432/drimacc_test"));
        pb.environment().put("TEST_DATABASE_USER", System.getProperty("TEST_DATABASE_USER", "postgres"));
        pb.environment().put("TEST_DATABASE_PASSWORD", System.getProperty("TEST_DATABASE_PASSWORD", "postgres"));
        pb.environment().put("TEST_MODE", "integration");

        // Redirect output to files for debugging
        File outputFile = tempDir.resolve("jar-output.log").toFile();
        File errorFile = tempDir.resolve("jar-error.log").toFile();
        pb.redirectOutput(outputFile);
        pb.redirectError(errorFile);

        System.out.println("🚀 Starting JAR in background...");
        jarProcess = pb.start();

        // Wait for JAR to start up
        System.out.println("⏳ Waiting for JAR to start on port " + API_PORT + "...");
        boolean jarStarted = waitForJarStartup(30); // Wait up to 30 seconds

        if (!jarStarted) {
            // Print JAR logs for debugging
            System.out.println("❌ JAR failed to start. Output:");
            try (BufferedReader reader = new BufferedReader(new FileReader(outputFile))) {
                reader.lines().forEach(System.out::println);
            }
            System.out.println("❌ JAR Error output:");
            try (BufferedReader reader = new BufferedReader(new FileReader(errorFile))) {
                reader.lines().forEach(System.out::println);
            }
        }

        assertTrue(jarStarted, "JAR should start successfully on port " + API_PORT);
        System.out.println("✅ JAR started successfully on port " + API_PORT);
    }

    @AfterAll
    static void tearDownJar() {
        if (jarProcess != null && jarProcess.isAlive()) {
            System.out.println("🛑 Stopping JAR process...");
            jarProcess.destroy();

            try {
                // Wait up to 10 seconds for graceful shutdown
                if (!jarProcess.waitFor(10, TimeUnit.SECONDS)) {
                    System.out.println("⚠️ JAR didn't stop gracefully, forcing termination...");
                    jarProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                System.out.println("⚠️ Interrupted while waiting for JAR to stop");
                jarProcess.destroyForcibly();
            }

            System.out.println("✅ JAR stopped");
        }

        // Clean up test database
        System.out.println("🧹 Cleaning up test database...");
        try {
            fin.TestConfiguration.cleanupTestDatabase();
            System.out.println("✅ Test database cleaned up");
        } catch (Exception e) {
            System.err.println("⚠️ Failed to clean up test database: " + e.getMessage());
        }
    }

    private static boolean waitForJarStartup(int timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        long timeoutMillis = timeoutSeconds * 1000L;

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            try {
                URL url = new URL(BASE_URL + "/health");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(1000);
                conn.setReadTimeout(1000);
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    conn.disconnect();
                    return true;
                }
                conn.disconnect();
            } catch (Exception e) {
                // JAR not ready yet, continue waiting
            }

            try {
                Thread.sleep(1000); // Wait 1 second before checking again
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return false; // Timeout reached
    }

    private String makeHttpRequest(String endpoint, String method) throws Exception {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int responseCode = conn.getResponseCode();
        assertEquals(200, responseCode, "API endpoint " + endpoint + " should return 200");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            conn.disconnect();
            return response.toString();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Health endpoint should respond")
    void testHealthEndpoint() throws Exception {
        System.out.println("🩺 Testing health endpoint...");
        String response = makeHttpRequest("/health", "GET");

        assertNotNull(response, "Health response should not be null");
        assertTrue(response.contains("success") || response.contains("ok") || response.contains("healthy") || response.contains("OK"),
                  "Health response should indicate success");

        System.out.println("✅ Health endpoint working: " + response);
    }

    @Test
    @Order(2)
    @DisplayName("Companies endpoint should respond")
    void testCompaniesEndpoint() throws Exception {
        System.out.println("🏢 Testing companies endpoint...");
        String response = makeHttpRequest("/companies", "GET");

        assertNotNull(response, "Companies response should not be null");
        // Response should be valid JSON (basic check)
        assertTrue(response.trim().startsWith("{") || response.trim().startsWith("["),
                  "Companies response should be valid JSON");

        System.out.println("✅ Companies endpoint working");
    }

    @Test
    @Order(3)
    @DisplayName("JAR should handle multiple concurrent requests")
    void testConcurrentRequests() throws Exception {
        System.out.println("🔄 Testing concurrent requests...");

        // Make multiple concurrent requests to test JAR stability
        Thread[] threads = new Thread[5];
        boolean[] results = new boolean[threads.length];

        for (int i = 0; i < threads.length; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    makeHttpRequest("/health", "GET");
                    results[index] = true;
                } catch (Exception e) {
                    results[index] = false;
                    System.err.println("Concurrent request " + index + " failed: " + e.getMessage());
                }
            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join(5000); // 5 second timeout per thread
        }

        // Check results
        for (int i = 0; i < results.length; i++) {
            assertTrue(results[i], "Concurrent request " + i + " should succeed");
        }

        System.out.println("✅ JAR handled concurrent requests successfully");
    }

    @Test
    @Order(4)
    @DisplayName("JAR should maintain database connection")
    void testDatabaseConnectivity() throws Exception {
        System.out.println("🗄️ Testing database connectivity through JAR...");

        // Test an endpoint that requires database access
        // If this succeeds, it means the JAR has proper database connectivity
        String response = makeHttpRequest("/companies", "GET");

        assertNotNull(response, "Database-dependent endpoint should respond");
        // If we get here without exceptions, database connectivity is working

        System.out.println("✅ JAR maintains database connectivity");
    }
}