plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.spotless)
}

group = "com.xilespa"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.flyway)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)
    runtimeOnly(libs.postgresql)

    testImplementation(libs.spring.boot.starter.actuator.test)
    testImplementation(libs.spring.boot.starter.data.jpa.test)
    testImplementation(libs.spring.boot.starter.flyway.test)
    testImplementation(libs.spring.boot.starter.security.test)
    testImplementation(libs.spring.boot.starter.validation.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testRuntimeOnly(libs.junit.platform.launcher)
    testAnnotationProcessor(libs.mapstruct.processor)
}

tasks.withType<Test> {
    useJUnitPlatform()
    // Phiên bản image Postgres cho Testcontainers, khớp với môi trường thật (PostgreSQL 16).
    systemProperty("xile.test.postgres-image", "postgres:" + libs.versions.postgres.image.get())
    // Múi giờ nghiệp vụ (BR-02): tránh lỗi Windows dùng Asia/Saigon bị Postgres từ chối
    systemProperty("user.timezone", "Asia/Ho_Chi_Minh")
}

spotless {
    java {
        // Kiểu AOSP: thụt 4 khoảng, khớp .editorconfig.
        googleJavaFormat().aosp() // dùng phiên bản mặc định đi kèm Spotless
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
