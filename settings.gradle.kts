pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Sendra"

include(
    ":app",
    ":core",
    ":domain",
    ":data",
    ":discovery",
    ":connection",
    ":transfer",
    ":ui",
    ":platform"
)
