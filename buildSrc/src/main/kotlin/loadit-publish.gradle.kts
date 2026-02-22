plugins {
    id("loadit-conventions")

    `maven-publish`
}

val buildVersion: String by project.extra

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("loadit") {
            from(components["java"])

            version = buildVersion
        }
    }

    repositories {
        maven {
            name = "coralmc"
            val base = "https://repo.coralmc.it"
            val releasesRepoUrl = "$base/releases/"
            setUrl(releasesRepoUrl)
        }
    }
}