plugins {
    java
}

dependencies {
    implementation(project(":modules:jpa"))
    implementation(project(":supports:logging"))
    implementation(project(":supports:monitoring"))

    implementation("org.springframework.boot:spring-boot-starter-batch")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter")

    runtimeOnly("com.mysql:mysql-connector-j")
}
