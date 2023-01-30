plugins {
    id("java")
}

group = "it.ytnoos.loadit"
version = "1.0-SNAPSHOT"

tasks.compileJava {
    options.release.set(8)
}

java {
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.0.0")
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
}