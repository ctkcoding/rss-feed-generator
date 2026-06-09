plugins {
	java
	id("org.springframework.boot") version "4.0.6"
	id("io.spring.dependency-management") version "1.1.7"
	id("com.diffplug.spotless") version "8.6.0"
}

group = "com.ctkcoding"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

springBoot {
	mainClass.set("com.ctkcoding.rssgen.RssGenApplication")
}

dependencies {
	implementation("com.mpatric:mp3agic:0.9.1")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("com.rometools:rome:2.1.0")
    implementation("com.rometools:rome-modules:2.1.0")
    compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-test")
	testCompileOnly("org.projectlombok:lombok")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testAnnotationProcessor("org.projectlombok:lombok")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.register<JavaExec>("runDump") {
    mainClass = "com.ctkcoding.rssgen.service.RssServiceDump"
    classpath = sourceSets["main"].runtimeClasspath
}

spotless {
    java {
        googleJavaFormat("1.35.0")
        removeUnusedImports()
        trimTrailingWhitespace()
        leadingTabsToSpaces(4)
        endWithNewline()
        targetExclude(
             "src/main/java/com/ctkcoding/rssgen/service/WatcherService.java",
             "src/test/java/com/ctkcoding/rssgen/service/WatcherServiceTest.java"
         )
    }
}

tasks.named("test") {
    dependsOn("spotlessCheck")
}
