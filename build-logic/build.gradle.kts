plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    kotlin("jvm") version embeddedKotlinVersion
}

dependencies {
    if (!project.hasProperty("stage0")) {
        implementation(libs.self) {
            if (System.getenv("CIRCLE_TAG") == null) {
                val currentVersion = versionConstraint.requiredVersion
                version {
                    require("${currentVersion}-SNAPSHOT")
                }
            }
        }
    }
}

gradlePlugin {
    plugins {
        create("bootstrap") {
            id = "me.tatarka.gradle.publishing.bootstrap"
            implementationClass = "me.tatarka.gradle.publishing.BootstrapPlugin"
        }
    }
}
