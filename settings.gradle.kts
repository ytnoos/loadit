pluginManagement {
    includeBuild("build-logic")
}

rootProject.name = "loadit"

include(
    "api",
    "bukkit-common",
    "bukkit",
    "bukkit-legacy",
    "bukkit-modern"
)