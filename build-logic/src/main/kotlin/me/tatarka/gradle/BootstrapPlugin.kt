package me.tatarka.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

class BootstrapPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        if (project.hasProperty("stage0")) {
            project.plugins.apply(MavenPublishPlugin::class)
        } else {
            project.plugins.apply("me.tatarka.gradle.central-release-publishing")
        }
        project.extensions.configure<PublishingExtension> {
            repositories {
                maven {
                    name = "bootstrap"
                    url = project.uri(project.layout.buildDirectory.dir("bootstrap"))
                }
            }
        }
    }
}