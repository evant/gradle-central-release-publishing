pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
    rulesMode.set(RulesMode.FAIL_ON_PROJECT_RULES)
}

rootProject.name = "gradle-central-release-publishing"