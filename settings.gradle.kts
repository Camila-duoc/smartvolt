pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Mapbox publica sus artefactos solo en su propio repositorio.
        maven { url = uri("https://api.mapbox.com/downloads/v2/releases/maven") }
    }
}
rootProject.name = "SmartVoltAsistencia"
include(":app")
