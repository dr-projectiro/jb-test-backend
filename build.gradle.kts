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
    compile("com.fasterxml.jackson.core:jackson-databind:2.9.6")
    compile("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.4.1")

    // javafaker: realistic name generator lib
    compile("com.github.javafaker:javafaker:0.17.2")
}

tasks.withType<Jar> {
    baseName = "${project.name}"

    configurations["compileClasspath"].forEach { file: File ->
        from(zipTree(file.absoluteFile))
    }

    manifest {
        attributes["Main-Class"] = "MainKt"
    }
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}