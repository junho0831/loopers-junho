dependencies {
    // 추가 모듈
    implementation(project(":modules:jpa"))
    implementation(project(":modules:kafka"))
    implementation(project(":supports:jackson"))
    implementation(project(":supports:logging"))
    implementation(project(":supports:monitoring"))

    // 웹
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${project.properties["springDocOpenApiVersion"]}")
    
    // 캐시 (Redis)
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    
    // 회복력 제어 (resilience4j)
    implementation("io.github.resilience4j:resilience4j-spring-boot3")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    
    // HTTP 클라이언트 (feign)
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    // 쿼리 DSL
//    annotationProcessor("com.querydsl:querydsl-apt::jakarta")
//    annotationProcessor("jakarta.persistence:jakarta.persistence-api")
//    annotationProcessor("jakarta.annotation:jakarta.annotation-api")

    // 테스트 픽스처
    testImplementation(testFixtures(project(":modules:jpa")))
    
    // 외부 API 테스트용 목 웹서버
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    
    // 비동기 테스트용 대기 유틸리티
    testImplementation("org.awaitility:awaitility:4.2.0")
}
