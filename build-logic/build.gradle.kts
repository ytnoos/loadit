plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()

    maven("https://repo.maven.apache.org/maven2/")
}

dependencies {
    implementation(libs.bundles.gradle.plugins)
}