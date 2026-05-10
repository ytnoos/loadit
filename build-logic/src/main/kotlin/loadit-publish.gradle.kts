plugins {
    id("loadit-conventions")

    `maven-publish`
}

// Modules without any public types (pure implementation modules) opt out of Javadoc generation
// by setting `loadit.publishJavadoc` to false in their build script. Sources are always published.
val publishJavadoc = (findProperty("loadit.publishJavadoc") as String?)?.toBoolean() ?: true

java {
    if (publishJavadoc) withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("loadit") {
            from(components["java"])

            version = project.version.toString()
        }
    }

    repositories {
        maven {
            name = "coralmc"
            val base = "https://repo.coralmc.it"
            val releasesRepoUrl = "$base/releases/"
            val snapshotsRepoUrl = "$base/snapshots/"
            setUrl(if (project.version.toString().contains("-dev.")) snapshotsRepoUrl else releasesRepoUrl)
        }
    }
}