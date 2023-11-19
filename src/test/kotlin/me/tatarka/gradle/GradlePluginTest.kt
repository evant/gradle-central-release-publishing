package me.tatarka.gradle

import assertk.all
import assertk.assertThat
import me.tatarka.gradle.util.assemble
import me.tatarka.gradle.util.containsAllArtifacts
import me.tatarka.gradle.util.createBuild
import me.tatarka.gradle.util.createKotlinSource
import me.tatarka.gradle.util.createSettings
import me.tatarka.gradle.util.publish
import me.tatarka.gradle.util.publishDir
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.CleanupMode
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class GradlePluginTest {

    @TempDir(cleanup = CleanupMode.ON_SUCCESS)
    lateinit var projectDir: Path

    @Test
    fun simple_gradle_plugin() {
        createSettings(projectDir, "my-project")

        createBuild(projectDir) {
            """
            import me.tatarka.gradle.publishing.*
                
            plugins {
                `java-gradle-plugin`
                `kotlin-dsl`
                id("me.tatarka.gradle.publishing.central-release-publishing")
            }
            
            group = "com.example"
            version = "1.0.0"
            
            gradlePlugin {
                plugins {
                    create("example") {
                        id = "com.example.my-project"
                        implementationClass = "com.example.ExamplePlugin"
                    }
                }
            }
            
            centralReleasePublishing {
                publications {
                    pom {
                        description = "A description"
                        github("evant", "my-project", "Eva Tatarka")
                        licenses {
                            apache2()
                        }
                    }
                }
            }
            """.trimIndent()
        }

        createKotlinSource(projectDir, "com.example", "ExamplePlugin") {
            """
            import org.gradle.api.Plugin
            import org.gradle.api.Project
            
            class ExamplePlugin : Plugin<Project> {
                override fun apply(project: Project) {}
            }
            """.trimIndent()
        }

        assemble(projectDir)
        publish(projectDir)

        val publishDir = publishDir(projectDir)

        assertThat(publishDir).all {
            containsAllArtifacts("com.example", "my-project", "1.0.0")
            containsAllArtifacts("com.example.my-project", "com.example.my-project.gradle.plugin", "1.0.0", packaging = "pom")
        }
    }
}