pluginManagement {
    repositories {
        maven {
            name = "bootstrap"
            url = uri(rootProject.projectDir.resolve("build/bootstrap"))
        }
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
includeBuild("build-logic")
rootProject.name = "central-release-publishing"