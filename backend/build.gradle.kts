plugins {
    java
    id("org.springframework.boot") version "4.1.2"
}
group = "com.santacecilia"
version = "0.3.0"
java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }
repositories { mavenCentral() }
dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.1.2"))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    runtimeOnly("com.mysql:mysql-connector-j")
    runtimeOnly("org.flywaydb:flyway-mysql")
}
tasks.processResources {
    from("../database/migrations") { include("V*.sql"); into("db/migration") }
}
tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8"; options.release = 21 }
tasks.bootJar { archiveFileName = "santa-cecilia-api.jar" }
