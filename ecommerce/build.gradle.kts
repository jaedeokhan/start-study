plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    id("jacoco")
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

allprojects {
    group = property("app.group").toString()
}

dependencyManagement {
    imports {
        mavenBom(libs.spring.cloud.dependencies.get().toString())
    }
}

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.springdoc.openapi)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation("org.springframework.boot:spring-boot-starter-validation")
    runtimeOnly("com.mysql:mysql-connector-j")
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    annotationProcessor(libs.spring.boot.configuration.processor)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:mysql")
}

// about source and compilation
java {
    sourceCompatibility = JavaVersion.VERSION_17
}

with(extensions.getByType(JacocoPluginExtension::class.java)) {
    toolVersion = "0.8.7"
}

// bundling tasks
tasks.getByName("bootJar") {
    enabled = true
}
tasks.getByName("jar") {
    enabled = false
}
// test tasks
tasks.test {
    ignoreFailures = true
    useJUnitPlatform()
}

// OpenAPI yaml 생성 태스크
tasks.register("generateOpenApiYaml") {
    group = "documentation"
    description = "Generate OpenAPI YAML specification"

    doLast {
        println("OpenAPI YAML 파일을 생성하려면 다음 단계를 수행하세요:")
        println("1. 애플리케이션 실행: ./gradlew bootRun")
        println("2. 다른 터미널에서 YAML 다운로드:")
        println("   curl http://localhost:8080/v3/api-docs.yaml -o src/main/resources/static/openapi.yaml")
        println("3. 생성된 파일 확인: src/main/resources/static/openapi.yaml")
    }
}
