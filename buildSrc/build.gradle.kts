plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    kotlin("jvm") version embeddedKotlinVersion
}

dependencies {
    if (file("../build/bootstrap").exists()) {
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
