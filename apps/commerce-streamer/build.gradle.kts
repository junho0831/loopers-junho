plugins {}

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
    
    // 캐시 (Redis)
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // (removed kapt/querydsl-apt; not used in Java-only streamer)

    // 테스트 픽스처
    testImplementation(testFixtures(project(":modules:jpa")))
    
    // 카프카 테스트
    testImplementation("org.springframework.kafka:spring-kafka-test")
    
    // 비동기 테스트용 대기 유틸리티
    testImplementation("org.awaitility:awaitility:4.2.0")
}
