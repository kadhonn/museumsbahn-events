plugins {
    kotlin("jvm") version "1.9.22" apply false
    kotlin("kapt") version "1.9.22"
    kotlin("plugin.allopen") version "1.9.22" apply false
    kotlin("plugin.spring") version "1.9.22" apply false
    id("org.springframework.boot") version "3.2.0" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
}

repositories {
    mavenCentral()
    mavenLocal()
}

allprojects {
    group = "at.museumrailwayevents"
    version = "0.1.0"
}