plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    signing
    kotlin("jvm") version embeddedKotlinVersion
}

group = "me.tatarka.gradle"
version = libs.versions.version.get()

dependencies {
    implementation(libs.nexus.publish)
    implementation(libs.dokka)
    compileOnly(libs.android)

    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.assertk)
}

tasks.test {
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

publishing {
    publications {
        create<MavenPublication>("publish") {
            groupId = "me.tatarka.gradle"
            artifactId = "central-release-publishing"
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "plugin"
            url = uri(layout.buildDirectory.dir("repo"))
        }
    }
}

tasks.test.configure {
    dependsOn("publish")
}