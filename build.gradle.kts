plugins {
    kotlin("jvm") version "1.8.0"
    application
}

group = "top.ntutn"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://github.com/psiegman/mvn-repo/raw/master/releases")
}

dependencies {
    testImplementation(kotlin("test"))
    // https://mvnrepository.com/artifact/org.jsoup/jsoup
    implementation("org.jsoup:jsoup:1.15.3")
    implementation("nl.siegmann.epublib:epublib-core:3.1")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}

application {
    mainClass.set("MainKt")
}