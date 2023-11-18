package me.tatarka.gradle

import com.android.build.api.variant.LibraryAndroidComponentsExtension
import dev.adamko.dokkatoo.DokkatooExtension
import dev.adamko.dokkatoo.formats.DokkatooJavadocPlugin
import io.github.gradlenexus.publishplugin.NexusPublishExtension
import io.github.gradlenexus.publishplugin.NexusPublishPlugin
import me.tatarka.gradle.PublicationType.Android
import me.tatarka.gradle.PublicationType.KotlinJvm
import me.tatarka.gradle.PublicationType.KotlinMultiplatform
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.maybeCreate
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin

const val PublishLocalForTest = "me.tatarka.gradle.publishLocalForTest"

class CentralReleasePublishingPlugin : Plugin<Project> {
    override fun apply(project: Project) {

        val publishLocalForTest = project.findProperty(PublishLocalForTest) == "true"

        val extension = project.extensions
            .create<CentralReleasePublishingExtension>("centralReleasePublishing")

        if (project == project.rootProject) {
            rootProject(project, publishLocalForTest)
        } else {
            project.version = project.rootProject.version
            project.group = project.rootProject.group
        }

        project.plugins.withId("org.jetbrains.kotlin.multiplatform") {
            setupPublishing(project, extension, publishLocalForTest, KotlinMultiplatform)
        }

        project.plugins.withId("org.jetbrains.kotlin.jvm") {
            setupPublishing(project, extension, publishLocalForTest, KotlinJvm)
        }

        project.plugins.withId("com.android.library") {
            setupPublishing(project, extension, publishLocalForTest, Android)
        }
    }

    private fun rootProject(project: Project, publishLocalForTest: Boolean) {
        project.afterEvaluate {
            if (project.version == Project.DEFAULT_VERSION) {
                throw InvalidUserDataException("project.version must be set in the project's root build.gradle file")
            }
            if (project.group.toString().isEmpty()) {
                throw InvalidUserDataException("project.group must be set in the project's root build.gradle file")
            }
        }
        if (!publishLocalForTest) {
            project.plugins.apply(NexusPublishPlugin::class)
            project.extensions.configure<NexusPublishExtension> {
                repositories {
                    sonatype()
                }
            }
        }
    }

    private fun setupPublishing(
        project: Project,
        extension: CentralReleasePublishingExtension,
        publishLocalForTest: Boolean,
        type: PublicationType
    ) {
        project.plugins.apply(MavenPublishPlugin::class)
        project.plugins.apply(SigningPlugin::class)

        if (type == KotlinJvm || type == KotlinMultiplatform) {
            project.plugins.apply(DokkatooJavadocPlugin::class)
        }

        val signing = project.extensions.findByType<SigningExtension>()!!
        signing.isRequired = project.findProperty("signing.keyId") != null

        project.extensions.configure<PublishingExtension> {

            if (publishLocalForTest) {
                repositories {
                    maven {
                        name = "local"
                        url = project.uri(project.rootProject.layout.buildDirectory.dir("repo"))
                    }
                }
            }

            when (type) {
                KotlinJvm -> {
                    project.extensions.configure<JavaPluginExtension> {
                        withSourcesJar()
                    }
                    val dokkatoo = project.extensions.findByType<DokkatooExtension>()!!
                    val dokkaJar = project.tasks.register<Jar>("dokkaJar") {
                        archiveClassifier.set("javadoc")
                        from(dokkatoo.dokkatooPublications.named("javadoc").map { it.outputDir })
                    }
                    if (project.pluginManager.hasPlugin("java-gradle-plugin")) {
                        publications.maybeCreate<MavenPublication>("pluginMaven").apply {
                            artifact(dokkaJar)
                            applyAndValidatePom(project, extension, pom)
                        }
                    } else {
                        publications.create<MavenPublication>("lib") {
                            from(project.components["java"])
                            artifact(dokkaJar)
                            applyAndValidatePom(project, extension, pom)
                        }
                    }
                }

                KotlinMultiplatform -> {
                    val dokkatoo = project.extensions.findByType<DokkatooExtension>()!!
                    val dokkaCommonJar = project.tasks.register<Jar>("dokkaCommonJar") {
                        archiveClassifier.set("javadoc")
                        from(dokkatoo.dokkatooPublications.named("javadoc").map { it.outputDir })
                    }

                    publications.all {
                        if (this is MavenPublication) {
                            artifact(dokkaCommonJar)
                            applyAndValidatePom(project, extension, pom)
                        }
                    }
                }

                Android -> {
                    val androidComponents = project.extensions.getByType(LibraryAndroidComponentsExtension::class.java)
                    androidComponents.finalizeDsl {
                        it.publishing {
                            singleVariant("release") {
                                withSourcesJar()
                                withJavadocJar()
                            }
                        }
                    }

                    project.components.all {
                        publications.create<MavenPublication>(name) {
                            from(this@all)
                            applyAndValidatePom(project, extension, pom)
                        }
                    }
                }
            }
            publications.all { signing.sign(this) }

            // TODO: remove after https://youtrack.jetbrains.com/issue/KT-46466 is fixed
            project.tasks.withType<AbstractPublishToMaven>().configureEach {
                dependsOn(project.tasks.withType<Sign>())
            }
        }
    }

    private fun applyAndValidatePom(project: Project, extension: CentralReleasePublishingExtension, pom: MavenPom) {
        project.afterEvaluate {
            val rootExtension = project.rootProject.extensions.findByType<CentralReleasePublishingExtension>()
            rootExtension?.apply(project.name, pom)
            extension.apply(project.name, pom)

            tasks.named("publish").configure {
                doFirst {
                    validatePom(pom)
                }
            }
        }
    }

    private fun validatePom(pom: MavenPom) {
        if (pom.name.orNull.isNullOrBlank()) {
            throw InvalidPomException("missing name")
        }
        if (pom.description.orNull.isNullOrBlank()) {
            throw InvalidPomException("missing description")
        }
        if (pom.url.orNull.isNullOrBlank()) {
            throw InvalidPomException("missing url")
        }
    }
}

class InvalidPomException(message: String) : InvalidUserDataException("invalid pom: $message")

private enum class PublicationType {
    KotlinJvm, KotlinMultiplatform, Android
}