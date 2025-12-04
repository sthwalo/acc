/*
 * Copyright 2025 Sthwalo Nyoni
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
 *
 * FIN Financial Management System - Build Configuration
 * This generated file contains a sample Java application project to get you started.
 * For more details on building Java & JVM projects, please refer to https://docs.gradle.org/8.8/userguide/building_java_projects.html in the Gradle documentation.
 */

plugins {
    // Apply the java-library plugin to create a library that can be depended on by other modules
    `java-library`
    // Spring Boot plugin
    id("org.springframework.boot") version "3.5.8"
    id("io.spring.dependency-management") version "1.1.4"
    // Code quality plugins
    checkstyle
    id("com.github.spotbugs") version "5.2.5"
    // Database migration plugin
    id("org.flywaydb.flyway") version "10.0.1"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    // Spring Boot starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // Use JUnit Jupiter for testing.
    testImplementation(libs.junit.jupiter)
    testImplementation("org.mockito:mockito-core:5.5.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.5.0")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // This dependency is used by the application.
    implementation(libs.guava)
    
    // Database drivers
    runtimeOnly("org.postgresql:postgresql")  // PostgreSQL driver (managed by Spring Boot)
    implementation("com.zaxxer:HikariCP:5.0.1")         // Connection pooling
    
    // PDF libraries - OPEN SOURCE ONLY (no iText due to commercial licensing)
    implementation("org.apache.pdfbox:pdfbox:3.0.0")  // Latest stable version - for PDF reading/text extraction
    implementation("org.apache.pdfbox:fontbox:3.0.0")
    implementation("org.apache.pdfbox:xmpbox:3.0.0")
    implementation("org.apache.pdfbox:preflight:3.0.0")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    implementation("org.bouncycastle:bcmail-jdk15on:1.70")
    
    // JNA for Libharu PDF library - for PDF generation (payslips, reports, invoices)
    implementation("net.java.dev.jna:jna:5.13.0")
    
    // JavaMail API for email functionality
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("jakarta.mail:jakarta.mail-api:2.1.1")
    runtimeOnly("org.eclipse.angus:angus-mail:2.0.2")
    
    // Excel processing libraries
    implementation("org.apache.poi:poi:5.2.4")
    implementation("org.apache.poi:poi-ooxml:5.2.4")
    implementation("org.apache.poi:poi-scratchpad:5.2.4")
    
    // JWT support for Spring Security
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

    // Keep Gson for JSON processing (Spring Boot uses Jackson by default, but we can keep Gson for compatibility)
    implementation("com.google.code.gson:gson:2.10.1")

    // Jakarta EE dependencies for servlet API (needed for Spring Security filters)
    implementation("jakarta.servlet:jakarta.servlet-api:6.0.0")

    // Legacy SparkJava dependencies (temporary - will be removed after migration)
    implementation("com.sparkjava:spark-core:2.9.4")
    implementation("org.eclipse.jetty:jetty-server:9.4.53.v20231009")
    implementation("org.eclipse.jetty:jetty-webapp:9.4.53.v20231009")
    implementation("org.eclipse.jetty:jetty-security:9.4.53.v20231009")
    implementation("org.eclipse.jetty:jetty-servlet:9.4.53.v20231009")
    implementation("org.eclipse.jetty:jetty-util:9.4.53.v20231009")
    implementation("org.slf4j:slf4j-simple:2.0.9")
}

// Code quality configurations
checkstyle {
    toolVersion = "10.12.4"
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
}

spotbugs {
    toolVersion = "4.8.3"
    excludeFilter = rootProject.file("config/spotbugs/exclude.xml")
    ignoreFailures = true
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}




tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
    maxHeapSize = "2G"
    
    // Pass system properties to tests
    systemProperties = System.getProperties()
        .filter { it.key is String }
        .mapKeys { it.key as String }
        .mapValues { it.value as Any }
    
    // Pass environment variables to tests (especially for CI/CD)
    // Environment variables should be set via .env file or system environment
    val testDbUrl = System.getenv("TEST_DATABASE_URL") ?: System.getProperty("TEST_DATABASE_URL")
    val testDbUser = System.getenv("TEST_DATABASE_USER") ?: System.getProperty("TEST_DATABASE_USER")
    val testDbPassword = System.getenv("TEST_DATABASE_PASSWORD") ?: System.getProperty("TEST_DATABASE_PASSWORD")
    val testMode = System.getenv("TEST_MODE") ?: "true"

    if (testDbUrl != null) systemProperty("TEST_DATABASE_URL", testDbUrl)
    if (testDbUser != null) systemProperty("TEST_DATABASE_USER", testDbUser)
    if (testDbPassword != null) systemProperty("TEST_DATABASE_PASSWORD", testDbPassword)
    systemProperty("TEST_MODE", testMode)
    
    // Set working directory to project root so test.env can be found
    workingDir = rootProject.projectDir

    // Exclude integration tests from unit test run - they run separately in integrationTest task
    exclude("**/*IntegrationTest.class")
    exclude("**/integration/**")
}

// Integration Test Task - JAR-First Testing
tasks.register<Test>("integrationTest") {
    group = "verification"
    description = "Runs integration tests against the built JAR (JAR-first approach)"

    // Use JUnit Platform
    useJUnitPlatform()

    // Include only integration tests
    include("**/*IntegrationTest.class")
    include("**/integration/**")

    // Test against built JAR (JAR-first approach)
    dependsOn("build")

    // Configure test environment
    maxHeapSize = "2G"
    systemProperties = System.getProperties()
        .filter { it.key is String }
        .mapKeys { it.key as String }
        .mapValues { it.value as Any }

    // Pass environment variables
    val testDbUrl = System.getenv("TEST_DATABASE_URL") ?: System.getProperty("TEST_DATABASE_URL")
    val testDbUser = System.getenv("TEST_DATABASE_USER") ?: System.getProperty("TEST_DATABASE_USER")
    val testDbPassword = System.getenv("TEST_DATABASE_PASSWORD") ?: System.getProperty("TEST_DATABASE_PASSWORD")

    if (testDbUrl != null) systemProperty("TEST_DATABASE_URL", testDbUrl)
    if (testDbUser != null) systemProperty("TEST_DATABASE_USER", testDbUser)
    if (testDbPassword != null) systemProperty("TEST_DATABASE_PASSWORD", testDbPassword)

    systemProperty("TEST_MODE", "integration")
    workingDir = rootProject.projectDir

    // Make sure integration tests don't run with unit tests
    shouldRunAfter(tasks.named<Test>("test"))
}

tasks.jar {
    manifest {
        attributes(mapOf(
            "Main-Class" to "fin.ConsoleApplication"
        ))
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
    }
}

tasks.register<Jar>("fatJar") {
    dependsOn.addAll(listOf("compileJava", "processResources"))
    archiveClassifier.set("fat")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes(mapOf(
            "Main-Class" to "fin.ConsoleApplication"
        ))
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
    }
    with(tasks.jar.get())
}
