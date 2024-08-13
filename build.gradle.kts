plugins {
    kotlin("jvm") version "2.0.0"
}

group = "ke.bb"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    compileOnly("com.caoccao.javet:javet:3.1.4")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}