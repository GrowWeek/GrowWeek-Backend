plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    id("org.springframework.boot") version "4.0.0"
    id("io.spring.dependency-management") version "1.1.7"
    jacoco
    kotlin("kapt") version "2.2.21"
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
    id("co.uzzu.dotenv.gradle") version "4.0.0"
}

group = "xyz.robinjoon"
version = "0.0.1-SNAPSHOT"
description = "GrowWeek-Backend"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

repositories {
    mavenCentral()
}

extra["springCloudVersion"] = "2025.1.0"

val kotestVersion = "6.0.7"
val mockkVersion = "1.14.6"
val jjwtVersion = "0.12.6" // JJWT 최신 안정 버전
val exposedVersion = "1.0.0-rc-4"

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-security") // Spring Security 7.x (Boot 4.0 호환)

    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Spring Cloud
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    // JWT - JJWT 라이브러리
    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

    // Database
    implementation("com.h2database:h2")
    implementation("org.postgresql:postgresql")

    // Annotation Processor
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Exposed ORM
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    // OpenAPI Documentation (Spring Boot 4 지원)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.0")
    implementation("com.github.therapi:therapi-runtime-javadoc:0.15.0")
    kapt("com.github.therapi:therapi-runtime-javadoc-scribe:0.15.0")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.security:spring-security-test") // Spring Security 테스트 지원
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
    testImplementation("io.kotest:kotest-extensions-spring:$kotestVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    // .env 파일의 환경변수를 테스트에 전달
    env.allVariables().forEach { (key, value) ->
        environment(key, value)
    }
}

// JaCoCo 설정
jacoco {
    toolVersion = "0.8.14"
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.70".toBigDecimal()
            }
        }
    }
}
