package me.tatarka.gradle.publishing

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create

/**
 * Does the bare-minimum to make the kotlin gradle script compile without the 'real' plugin applied.
 */
class StubPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(MavenPublishPlugin::class)
        val stub = project.extensions.create<StubCentralReleasePublishingExtension>("centralReleasePublishing")

        project.afterEvaluate {
            if (stub.snapshot.get()) {
                project.version = "${project.version}-SNAPSHOT"
            }
        }
    }
}

abstract class StubCentralReleasePublishingExtension {

    abstract val snapshot: Property<Boolean>

    fun publications(action: Action<StubMavenPublication>) {
        // ignore
    }
}

abstract class StubMavenPublication {
    abstract fun pom(action: Action<StubMavenPom>)
}

abstract class StubMavenPom {
    abstract val description: Property<String>

    abstract fun licenses(action: Action<StubMavenPomLicenseSpec>)
}

abstract class StubMavenPomLicenseSpec

fun StubMavenPom.github(user: String, project: String, developer: String) {
    // ignore
}

fun StubMavenPomLicenseSpec.apache2() {
    // ignore
}