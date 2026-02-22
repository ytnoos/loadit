plugins {
    java
    `java-library`

    id("net.linguica.maven-settings")
    id("com.gradleup.shadow")
}

group = "it.ytnoos.loadit"
version = project.extra["buildVersion"] as String

repositories {
    mavenCentral()

    maven {
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }

}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    withType<Javadoc> {
        options.encoding = "UTF-8"
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}