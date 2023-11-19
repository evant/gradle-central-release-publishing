import me.tatarka.gradle.publishing.github
import me.tatarka.gradle.publishing.apache2

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    id("me.tatarka.gradle.publishing.bootstrap")
}

group = libs.self.get().group
version = libs.versions.version.get()

dependencies {
    implementation(libs.nexus.publish)
    implementation(libs.dokkatoo)
    compileOnly(libs.android)

    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.assertk)
}

centralReleasePublishing {
    snapshot = System.getenv("CIRCLE_TAG") == null

    defaults {
        pom {
            description = "An opinionated gradle plugin to manage publishing to maven central"
            github("evant", "gradle-central-release-publishing", "Eva Tatarka")
            licenses {
                apache2()
            }
        }
    }
}

val bootstrap = tasks.register("bootstrap") {
    dependsOn("publishAllPublicationsToBootstrapRepository")
}

tasks.test {
    dependsOn(bootstrap)
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        create("central-release-management") {
            id = "me.tatarka.gradle.publishing.central-release-publishing"
            implementationClass = "me.tatarka.gradle.publishing.CentralReleasePublishingPlugin"
        }
    }
}