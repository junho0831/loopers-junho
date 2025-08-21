dependencies {
    // add-ons
    implementation(project(":modules:jpa"))
    implementation(project(":supports:jackson"))
    implementation(project(":supports:logging"))
    implementation(project(":supports:monitoring"))

    // web
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${project.properties["springDocOpenApiVersion"]}")
    
    // cache (Redis)
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    
    // resilience4j
    implementation("io.github.resilience4j:resilience4j-spring-boot3")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    
    // feign client
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    // querydsl
//    annotationProcessor("com.querydsl:querydsl-apt::jakarta")
//    annotationProcessor("jakarta.persistence:jakarta.persistence-api")
//    annotationProcessor("jakarta.annotation:jakarta.annotation-api")

    // test-fixtures
    testImplementation(testFixtures(project(":modules:jpa")))
    
    // mock web server for external API testing
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    
    // awaitility for async testing
    testImplementation("org.awaitility:awaitility:4.2.0")
}
