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
    // Apply the application plugin to add support for building a CLI application in Java.
    application
    // Code quality plugins
    checkstyle
    id("com.github.spotbugs") version "5.2.5"
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    // Use JUnit Jupiter for testing.
    testImplementation(libs.junit.jupiter)
    testImplementation("org.mockito:mockito-core:5.5.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.5.0")
    testImplementation("com.h2database:h2:2.2.224")  // H2 for testing

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // This dependency is used by the application.
    implementation(libs.guava)
    
    // Database drivers
    implementation("org.postgresql:postgresql:42.7.3")  // PostgreSQL driver
    implementation("com.zaxxer:HikariCP:5.0.1")         // Connection pooling
    
    // PDF libraries
    implementation("org.apache.pdfbox:pdfbox:3.0.0")  // Latest stable version
    implementation("org.apache.pdfbox:fontbox:3.0.0")
    implementation("org.apache.pdfbox:xmpbox:3.0.0")
    implementation("org.apache.pdfbox:preflight:3.0.0")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    implementation("org.bouncycastle:bcmail-jdk15on:1.70")
    implementation("com.itextpdf:itextpdf:5.5.13.3")
    
    // JNA for Libharu PDF library
    implementation("net.java.dev.jna:jna:5.13.0")
    
    // JavaMail API for email functionality
    implementation("com.sun.mail:javax.mail:1.6.2")
    
    // Excel processing libraries
    implementation("org.apache.poi:poi:5.2.4")
    implementation("org.apache.poi:poi-ooxml:5.2.4")
    implementation("org.apache.poi:poi-scratchpad:5.2.4")
    
    // REST API dependencies
    implementation("com.sparkjava:spark-core:2.9.4")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.slf4j:slf4j-simple:2.0.7")
}

// Code quality configurations
checkstyle {
    toolVersion = "10.12.4"
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
}

spotbugs {
    toolVersion = "4.8.3"
    excludeFilter = rootProject.file("config/spotbugs/exclude.xml")
    // Temporarily disabled due to EI_EXPOSE_REP issues in model classes
    // TODO: Re-enable after fixing model class encapsulation
    ignoreFailures = true
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

application {
    // Define the main class for the application.
    mainClass.set("fin.ConsoleApplication")
}

// Configure the run task to pass system properties
tasks.named<JavaExec>("run") {
    systemProperties = System.getProperties().toMap() as Map<String, Any>
    // Auto-confirm license for gradle run to avoid NoSuchElementException
    systemProperty("fin.license.autoconfirm", "true")
    
    // Check if API mode is requested
    if (project.hasProperty("api") || System.getProperty("api") != null) {
        mainClass.set("fin.ApiApplication")
        args("api")
    } else {
        mainClass.set("fin.ConsoleApplication")
    }
}

// Add separate task for running classification test
tasks.register<JavaExec>("runClassificationTest") {
    group = "application"
    description = "Run the ClassificationTest"
    classpath = sourceSets["main"].runtimeClasspath + sourceSets["test"].runtimeClasspath
    mainClass.set("fin.service.ClassificationTest")
    systemProperties = System.getProperties().toMap() as Map<String, Any>
    systemProperty("fin.license.autoconfirm", "true")
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
    maxHeapSize = "2G"
}

tasks.jar {
    manifest {
        attributes(mapOf(
            "Main-Class" to application.mainClass
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
            "Main-Class" to application.mainClass
        ))
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
    }
    with(tasks.jar.get())
}
