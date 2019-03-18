import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.11"
}

group = "jb-test-backend"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    compile(kotlin("stdlib-jdk8"))

    // gson: json <-> pojo converter
    compile("com.google.code.gson:gson:2.7")

    // javalin: minimalistic web server
    compile("io.javalin:javalin:2.4.0")

    // javafaker: realistic name generator lib
    compile("com.github.javafaker:javafaker:0.17.2")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}