package me.tatarka.gradle.util

import assertk.Assert
import assertk.all
import assertk.assertions.support.expected
import assertk.assertions.support.show
import org.gradle.testkit.runner.GradleRunner
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.bufferedReader
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists
import kotlin.io.path.writeText

private val VersionMatch = Regex("^\\s*version\\s*=\\s*\"([^\"]+)\"\\s*$")

fun run(projectDir: Path, task: String, vararg args: String): String {
    val result = GradleRunner.create()
        .withProjectDir(projectDir.toFile())
        .withArguments(task, *args)
        .forwardOutput()
        .build()

    return result.output
}

fun assemble(projectDir: Path): String {
    return run(projectDir, "assemble")
}

fun publish(projectDir: Path): String {
    return run(projectDir, "publishAllPublicationsToLocalRepository")
}

fun createSettings(projectDir: Path, name: String, contents: () -> String = { "" }) {
    val pluginRepo = Paths.get(".").resolve("build/bootstrap").absolutePathString()
    var version = Paths.get(".").resolve("gradle/libs.versions.toml")
        .bufferedReader()
        .lineSequence()
        .mapNotNull { line ->
            VersionMatch.matchEntire(line)?.let { it.groups[1]?.value }
        }
        .first()

    if (System.getenv("CIRCLE_TAG") == null) {
        version += "-SNAPSHOT"
    }

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
                        if (target.id.id == "me.tatarka.gradle.publishing.central-release-publishing") {
                            useModule("me.tatarka.gradle.publishing:central-release-publishing:${version}")
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

fun publishDir(projectDir: Path): Path = projectDir.resolve("build/local")

fun createBuild(projectDir: Path, publish: Boolean = true, contents: () -> String) {
    projectDir.resolve("build.gradle.kts").apply {
        parent.createDirectories()
        if (publish) {
            writeText(
                """
                ${contents()}
                publishing {
                    repositories {
                        maven {
                            name = "local"
                            url = uri(rootProject.layout.buildDirectory.dir("local"))
                        }
                    }
                }
                """.trimIndent()
            )
        } else {
            writeText(contents())
        }
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
        val suffixes = if (packaging == "pom") {
            // don't need javadoc & sources
            arrayOf(".pom")
        } else {
            arrayOf(".pom", ".$packaging", "-javadoc.jar", "-sources.jar")
        }
        for (suffix in suffixes) {
            val artifact = artifactPath.resolve(artifactName + suffix)
            if (artifact.notExists()) {
                expected("${show(artifact)} to exist")
            }
        }
    }
}