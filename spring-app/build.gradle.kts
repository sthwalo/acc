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
    // Java plugin for compilation
    java
    // Spring Boot plugin for building JAR
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
    // Code quality plugins
    checkstyle
    id("com.github.spotbugs") version "5.2.5"
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

    // Removed dependency on legacy app module - spring-app is now standalone
    
    // Removed SparkJava dependencies - migrated to Spring Boot REST controllers
    
}

// Custom task to run JAR with libharu JVM arguments
tasks.register<Exec>("runWithLibharu") {
    group = "application"
    description = "Run the JAR with libharu library path for PDF generation"
    commandLine("java", "-Djna.library.path=/opt/homebrew/lib:/usr/local/lib", "-jar", tasks.bootJar.get().archiveFile.get().asFile.absolutePath)
    dependsOn(tasks.bootJar)
}

// Code quality configurations

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
}


