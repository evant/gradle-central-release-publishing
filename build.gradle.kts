plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    id("me.tatarka.gradle.bootstrap")
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

if (!project.hasProperty("stage0")) {
    apply(from = "publish.gradle")
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
            id = "me.tatarka.gradle.central-release-publishing"
            implementationClass = "me.tatarka.gradle.CentralReleasePublishingPlugin"
        }
    }
}