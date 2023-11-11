package me.tatarka.gradle

import assertk.Assert
import assertk.all
import assertk.assertions.support.expected
import assertk.assertions.support.show
import org.gradle.testkit.runner.GradleRunner
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists
import kotlin.io.path.writeText

fun run(projectDir: Path, task: String): String {
    val result = GradleRunner.create()
        .withProjectDir(projectDir.toFile())
        .withArguments(task)
        .build()

    return result.output
}

fun assemble(projectDir: Path): String {
    return run(projectDir, "assemble")
}

fun publish(projectDir: Path): String {
    return run(projectDir, "publish")
}

fun createSettings(projectDir: Path, name: String, contents: () -> String = { "" }) {
    val pluginRepo = Paths.get(".").resolve("build/repo").absolutePathString()
    projectDir.resolve("settings.gradle.kts").apply {
        writeText(
            """
            pluginManagement {
                repositories {
                    maven {
                        name = "plugin"
                        url = uri("$pluginRepo")
                    }
                    mavenCentral()
                    google()
                    gradlePluginPortal()
                }
                resolutionStrategy {
                    eachPlugin {
                        if (target.id.id == "me.tatarka.gradle.central-release-publishing") {
                            useModule("me.tatarka.gradle:central-release-publishing:0.1.0")
                        }
                    }
                }
            }      
            dependencyResolutionManagement {
                repositories {
                    mavenCentral()
                    google()
                }
            }
            rootProject.name = "$name"
            ${contents()}
            """.trimIndent()
        )
    }
}

fun createBuild(projectDir: Path, contents: () -> String) {
    projectDir.resolve("build.gradle.kts").apply {
        parent.createDirectories()
        writeText(contents())
    }
}

fun createKotlinSource(projectDir: Path, packageName: String, fileName: String, contents: () -> String) {
    projectDir
        .resolve("src/main/kotlin")
        .resolve(packageName.replace('.', '/'))
        .resolve("$fileName.kt").apply {
            parent.createDirectories()
            writeText(contents())
        }
}

fun testPublishRootFix(): String {
    return """
    // disable trying to initialize sonatype
    tasks.named("initializeSonatypeStagingRepository") {
        enabled = false 
    }
    """.trimIndent()
}

fun testPublish(to: String): String {
    return """
    // add local publish path
    publishing {
        repositories {
            maven { 
                name = "local"
                url = uri(rootProject.layout.buildDirectory.dir("$to")) 
            }
        }
    }
    // disable tyring to publish to sonatype
    tasks.withType<PublishToMavenRepository> {
        onlyIf { repository.name == "local" }
    }
    """.trimIndent()
}

fun Assert<Path>.containsAllArtifacts(
    groupId: String,
    artifactId: String,
    version: String,
    packaging: String = "jar"
) = given { repoPath ->
    val artifactPath = repoPath
        .resolve(groupId.replace('.', '/'))
        .resolve(artifactId)
        .resolve(version)
    if (artifactPath.notExists()) {
        expected("${show(artifactPath)} to exist")
    }

    all {
        val artifactName = "$artifactId-$version"
        for (suffix in arrayOf(".pom", ".$packaging", "-javadoc.jar", "-sources.jar")) {
            val artifact = artifactPath.resolve(artifactName + suffix)
            if (artifact.notExists()) {
                expected("${show(artifact)} to exist")
            }
        }
    }
}