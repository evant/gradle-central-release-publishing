package me.tatarka.gradle.publishing

import com.android.build.api.variant.LibraryAndroidComponentsExtension
import dev.adamko.dokkatoo.DokkatooExtension
import dev.adamko.dokkatoo.formats.DokkatooHtmlPlugin
import io.github.gradlenexus.publishplugin.NexusPublishExtension
import io.github.gradlenexus.publishplugin.NexusPublishPlugin
import me.tatarka.gradle.publishing.PublicationType.Android
import me.tatarka.gradle.publishing.PublicationType.KotlinJvm
import me.tatarka.gradle.publishing.PublicationType.KotlinMultiplatform
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.publish.maven.tasks.GenerateMavenPom
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.maybeCreate
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.namedDomainObjectSet
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin

class CentralReleasePublishingPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions
            .create<CentralReleasePublishingExtension>("centralReleasePublishing")
        // default to empty publication in case none get set
        extension.publications = project.objects.namedDomainObjectSet(MavenPublication::class)

        val rootExtension = project.rootProject.extensions.findByType<CentralReleasePublishingExtension>()
        // defaults start with root defaults
        if (rootExtension != null && rootExtension !== extension) {
            mergePomDefaults(extension.defaults.pom, rootExtension.defaults.pom, project.name)
        }

        if (project == project.rootProject) {
            rootProject(project)
        } else {
            // default group and version to root values for convenience
            project.group = project.rootProject.group
            project.version = project.rootProject.version
        }

        project.plugins.apply(MavenPublishPlugin::class)

        project.plugins.withId("org.jetbrains.kotlin.multiplatform") {
            setupPublishing(project, extension, KotlinMultiplatform)
        }

        project.plugins.withId("org.jetbrains.kotlin.jvm") {
            setupPublishing(project, extension, KotlinJvm)
        }

        project.plugins.withId("com.android.library") {
            setupPublishing(project, extension, Android)
        }
    }

    private fun rootProject(project: Project) {
        project.plugins.apply(NexusPublishPlugin::class)
        project.extensions.configure<NexusPublishExtension> {
            repositories {
                sonatype()
            }
        }
    }

    private fun setupPublishing(
        project: Project,
        extension: CentralReleasePublishingExtension,
        type: PublicationType
    ) {
        project.plugins.apply(SigningPlugin::class)

        if (type == KotlinJvm || type == KotlinMultiplatform) {
            project.plugins.apply(DokkatooHtmlPlugin::class)
        }

        val signing = project.extensions.findByType<SigningExtension>()!!
        signing.isRequired = project.findProperty("signing.keyId") != null

        project.extensions.configure<PublishingExtension> {
            extension.publications = publications.withType(MavenPublication::class.java)

            when (type) {
                KotlinJvm -> {
                    project.extensions.configure<JavaPluginExtension> {
                        withSourcesJar()
                    }
                    val dokkatoo = project.extensions.findByType<DokkatooExtension>()!!
                    val dokkaJar = project.tasks.register<Jar>("dokkaJar") {
                        archiveClassifier.set("javadoc")
                        from(dokkatoo.dokkatooPublications.named("html").map { it.outputDir })
                    }
                    if (project.pluginManager.hasPlugin("java-gradle-plugin")) {
                        publications.maybeCreate<MavenPublication>("pluginMaven").apply {
                            artifact(dokkaJar)
                        }
                    } else {
                        publications.create<MavenPublication>("lib") {
                            from(project.components["java"])
                            artifact(dokkaJar)
                        }
                    }
                }

                KotlinMultiplatform -> {
                    val dokkatoo = project.extensions.findByType<DokkatooExtension>()!!
                    val dokkaCommonJar = project.tasks.register<Jar>("dokkaCommonJar") {
                        archiveClassifier.set("javadoc")
                        from(dokkatoo.dokkatooPublications.named("html").map { it.outputDir })
                    }

                    extension.publications.configureEach {
                        artifact(dokkaCommonJar)
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

                    project.components.configureEach {
                        if (name == "release") {
                            publications.create<MavenPublication>(name) {
                                from(this@configureEach)
                            }
                        }
                    }
                }
            }
            extension.publications.configureEach {
                signing.sign(this)
                finalizeAndValidatePom(
                    project = project,
                    publication = this,
                    snapshot = extension.snapshot,
                    pomDefaults = extension.defaults.pom,
                    pom = pom
                )
            }

            // TODO: remove after https://youtrack.jetbrains.com/issue/KT-46466 is fixed
            project.tasks.withType<AbstractPublishToMaven>().configureEach {
                dependsOn(project.tasks.withType<Sign>())
            }
        }
    }

    private fun finalizeAndValidatePom(
        project: Project,
        publication: MavenPublication,
        snapshot: Property<Boolean>,
        pomDefaults: MavenPomDefaults,
        pom: MavenPom
    ) {
        mergePomDefaults(pom, pomDefaults, project.name)

        // ideally we'd just set a property but version isn't one :(
        project.afterEvaluate {
            if (snapshot.getOrElse(false) && publication.version != Project.DEFAULT_VERSION) {
                publication.version += "-SNAPSHOT"
            }
        }

        val taskPublicationName = publication.name.capitalized()
        val validatePom =
            project.tasks.create<ValidatePomForMavenCentral>("validatePomFileFor${taskPublicationName}Publication")
        project.tasks.named<GenerateMavenPom>("generatePomFileFor${taskPublicationName}Publication") {
            validatePom.pomFile.set(destination)
            finalizedBy(validatePom)
        }
    }
}

private enum class PublicationType {
    KotlinJvm, KotlinMultiplatform, Android
}

