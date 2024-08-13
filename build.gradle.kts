plugins {
    kotlin("jvm") version "2.0.0"
    `maven-publish`
}

group = "ke.bb"
version = "1.0-SNAPSHOT"
publishing{
    publications{
        create<MavenPublication>("mavenJava"){
            groupId = "ke.bb"
            artifactId = "plugins"
            version = "0.1.2"
            from(components["kotlin"])
        }
    }
}
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