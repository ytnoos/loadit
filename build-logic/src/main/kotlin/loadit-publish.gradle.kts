plugins {
    id("loadit-conventions")

    `maven-publish`
}

java {
    withJavadocJar()
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