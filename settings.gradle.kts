pluginManagement {
    repositories {
        maven { url = uri(rootProject.buildFile.resolve("bootstrap"))}
        mavenLocal()
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

rootProject.name = "central-release-publishing"