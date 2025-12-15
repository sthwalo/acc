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
    implementation("org.flywaydb:flyway-core:10.12.0")  // Database migration tool (pin to support Postgres 17)
    implementation("org.flywaydb:flyway-database-postgresql:10.12.0")  // PostgreSQL support

    // Lombok for reducing boilerplate code
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

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
    testRuntimeOnly("com.h2database:h2")      // H2 in-memory database for testing
    implementation("com.zaxxer:HikariCP:5.0.1")         // Connection pooling
    
    // PDF libraries - OPEN SOURCE ONLY (no iText due to commercial licensing)
    implementation("org.apache.pdfbox:pdfbox:3.0.0")  // Latest stable version - for PDF reading/text extraction
    implementation("org.apache.pdfbox:fontbox:3.0.0")
    implementation("org.apache.pdfbox:xmpbox:3.0.0")
    implementation("org.apache.pdfbox:preflight:3.0.0")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    implementation("org.bouncycastle:bcmail-jdk15on:1.70")
    
    // OCR library for image-based PDFs (Tesseract via Tess4J)
    implementation("net.sourceforge.tess4j:tess4j:5.9.0")  // Tesseract OCR wrapper for Java
    
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

    // PayPal Checkout SDK for payment processing
    implementation("com.paypal.sdk:checkout-sdk:1.0.2")

    // Bucket4j for simple in-memory rate limiting
    implementation("com.github.vladimir-bukhtoyarov:bucket4j-core:7.6.0")

    // Keep Gson for JSON processing (Spring Boot uses Jackson by default, but we can keep Gson for compatibility)
    implementation("com.google.code.gson:gson:2.10.1")

    // Jakarta EE dependencies for servlet API (needed for Spring Security filters)
    implementation("jakarta.servlet:jakarta.servlet-api:6.0.0")

    //Actuator for monitoring
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
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

// Configure bootJar to produce the correct filename for production
tasks.bootJar {
    archiveBaseName = "fin-spring"
}

// Disable the regular jar task since we only need the bootJar for Spring Boot applications
tasks.jar {
    enabled = false
}

// Task to analyze PDF column structure
tasks.register<JavaExec>("analyzeColumns") {
    group = "analysis"
    description = "Analyze PDF column structure for parser configuration"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "fin.util.PdfColumnAnalyzer"
    
    // Get PDF path and bank name from command line properties
    doFirst {
        val pdfPath = project.findProperty("pdfPath") as String? 
            ?: throw GradleException("Please specify -PpdfPath=<path-to-pdf>")
        val bankName = project.findProperty("bankName") as String? 
            ?: throw GradleException("Please specify -PbankName=<bank-name>")
        
        args = listOf(pdfPath, bankName)
    }
}

// Task to extract OCR text with coordinates
tasks.register<JavaExec>("extractOcrCoordinates") {
    group = "analysis"
    description = "Extract text from OCR-based PDF with X,Y coordinates"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "fin.util.OcrCoordinateExtractor"
    
    // Set JNA library path for Tesseract on macOS
    systemProperty("jna.library.path", "/opt/homebrew/lib")
    
    doFirst {
        val pdfPath = project.findProperty("pdfPath") as String? 
            ?: throw GradleException("Please specify -PpdfPath=<path-to-pdf>")
        val pageNum = project.findProperty("pageNum") as String? ?: "0"
        
        args = listOf(pdfPath, pageNum)
    }
}

tasks.register<JavaExec>("testAbsaParser") {
    group = "analysis"
    description = "Test Absa parser on OCR-extracted text file"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "fin.util.TestAbsaParser"
    
    doFirst {
        val textFile = project.findProperty("textFile") as String? 
            ?: throw GradleException("Please specify -PtextFile=<path-to-text-file>")
        
        args = listOf(textFile)
    }
}

tasks.register<JavaExec>("testFnbParser") {
    group = "analysis"
    description = "Test FNB parser on text file"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "fin.util.TestFnbParser"
    
    doFirst {
        val textFile = project.findProperty("textFile") as String? 
            ?: throw GradleException("Please specify -PtextFile=<path-to-text-file>")
        
        args = listOf(textFile)
    }
}


