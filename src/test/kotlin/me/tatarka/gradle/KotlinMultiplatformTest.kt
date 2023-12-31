package me.tatarka.gradle

import assertk.assertThat
import me.tatarka.gradle.util.assemble
import me.tatarka.gradle.util.containsAllArtifacts
import me.tatarka.gradle.util.createBuild
import me.tatarka.gradle.util.createSettings
import me.tatarka.gradle.util.publish
import me.tatarka.gradle.util.publishDir
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.CleanupMode
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class KotlinMultiplatformTest {

    @TempDir(cleanup = CleanupMode.ON_SUCCESS)
    lateinit var projectDir: Path

    @Test
    fun simple_kotlin_project() {
        createSettings(projectDir, "my-project")

        createBuild(projectDir) {
            """
            import me.tatarka.gradle.publishing.*
            
            plugins {
                kotlin("multiplatform") version "1.9.10"
                id("me.tatarka.gradle.publishing.central-release-publishing")
            }
            
            group = "com.example"
            version = "1.0.0"
            
            kotlin {
                js { nodejs() }
                jvm()
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

        assemble(projectDir)
        publish(projectDir)

        val publishDir = publishDir(projectDir)

        assertThat(publishDir).containsAllArtifacts("com.example", "my-project", "1.0.0")
        assertThat(publishDir).containsAllArtifacts("com.example", "my-project-js", "1.0.0", packaging = "klib")
        assertThat(publishDir).containsAllArtifacts("com.example", "my-project-jvm", "1.0.0")
    }

    @Test
    fun multi_module_kotlin_project() {
        createSettings(projectDir, "my-project") {
            """
            include(":project-1")
            include(":project-2")
            """.trimIndent()
        }

        createBuild(projectDir, publish = false) {
            """
            import me.tatarka.gradle.publishing.*
            
            plugins {
                id("me.tatarka.gradle.publishing.central-release-publishing")
            }
            
            group = "com.example"
            version = "1.0.0"
            
            centralReleasePublishing {
                defaults {
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

        val project1Dir = projectDir.resolve("project-1")
        val project2Dir = projectDir.resolve("project-2")

        createBuild(project1Dir) {
            """
            plugins {
                kotlin("multiplatform") version "1.9.10"
                id("me.tatarka.gradle.publishing.central-release-publishing")
            }
            
            kotlin {
                js { nodejs() }
                jvm()
            }
            """.trimIndent()
        }

        createBuild(project2Dir) {
            """
            plugins {
                kotlin("multiplatform") version "1.9.10"
                id("me.tatarka.gradle.publishing.central-release-publishing")
            }
            
            kotlin {
                js { nodejs() }
                jvm()
            }
            """.trimIndent()
        }

        assemble(projectDir)
        publish(projectDir)

        val publishDir = publishDir(projectDir)

        assertThat(publishDir).containsAllArtifacts("com.example", "project-1", "1.0.0")
        assertThat(publishDir).containsAllArtifacts("com.example", "project-1-js", "1.0.0", packaging = "klib")
        assertThat(publishDir).containsAllArtifacts("com.example", "project-1-jvm", "1.0.0")

        assertThat(publishDir).containsAllArtifacts("com.example", "project-2", "1.0.0")
        assertThat(publishDir).containsAllArtifacts("com.example", "project-2-js", "1.0.0", packaging = "klib")
        assertThat(publishDir).containsAllArtifacts("com.example", "project-2-jvm", "1.0.0")
    }
}