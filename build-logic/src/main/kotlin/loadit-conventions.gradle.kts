plugins {
    java
    `java-library`

    id("net.linguica.maven-settings")
    id("com.gradleup.shadow")
}

group = "it.ytnoos.loadit"
version = run {
    val describe = try {
        providers.exec { commandLine("git", "describe", "--tags", "--long", "--match", "v*") }.standardOutput.asText.get().trim()
    } catch (_: Exception) {
        throw GradleException(
            "No version tag found. Run: git fetch --tags\n" +
                    "If this is a fresh repo, create an initial tag: ./scripts/release.sh <version>"
        )
    }

    val match = Regex("""^v(.+)-(\d+)-g(.+)$""").matchEntire(describe) ?: throw GradleException("Unexpected version format: $describe")
    val base = match.groupValues[1]
    val commits = match.groupValues[2].toInt()
    val hash = match.groupValues[3]

    if (commits == 0) base else "$base-dev.$commits.$hash"
}

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