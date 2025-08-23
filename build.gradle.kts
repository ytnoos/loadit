plugins {
    id("java")
    `maven-publish`
}

group = "it.ytnoos.loadit"
version = "1.2.2-SNAPSHOT"

tasks.compileJava {
    options.release.set(8)
}

java {
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")

    implementation("org.jspecify:jspecify:1.0.0")
}

publishing {
    publications {
        create<MavenPublication>("loadit") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            url = uri("https://repo.coralmc.it/releases")
            credentials {
                username = providers.gradleProperty("mavenUser").get()
                password = providers.gradleProperty("mavenPassword").get()
            }
        }
    }
}
