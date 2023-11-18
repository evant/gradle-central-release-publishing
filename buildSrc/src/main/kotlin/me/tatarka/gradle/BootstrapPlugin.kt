package me.tatarka.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.extra

class BootstrapPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val bootstrapped = project.rootProject.layout.buildDirectory.dir("bootstrap").get().asFile.exists()
        project.extra["bootstrapped"] = bootstrapped
        if (bootstrapped) {
            project.plugins.apply("me.tatarka.gradle.central-release-publishing")
        } else {
            project.plugins.apply(MavenPublishPlugin::class)
        }
        project.extensions.configure<PublishingExtension> {
            repositories {
                maven {
                    name = "bootstrap"
                    url = project.uri(project.rootProject.layout.buildDirectory.dir("bootstrap"))
                }
            }
        }
    }
}