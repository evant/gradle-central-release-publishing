pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        maven {
            name = "bootstrap"
            url = uri("../build/bootstrap")
        }
        mavenCentral()
        gradlePluginPortal()
    }
    rulesMode = RulesMode.FAIL_ON_PROJECT_RULES
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}