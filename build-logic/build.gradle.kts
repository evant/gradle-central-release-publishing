plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    kotlin("jvm") version embeddedKotlinVersion
}

dependencies {
    if (!project.hasProperty("stage0")) {
        implementation(libs.self)
    }
}

gradlePlugin {
    plugins {
        create("bootstrap") {
            id = "me.tatarka.gradle.bootstrap"
            implementationClass = "me.tatarka.gradle.BootstrapPlugin"
        }
    }
}
