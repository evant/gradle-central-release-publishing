package me.tatarka.gradle

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
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

class AndroidTest {

    @TempDir(cleanup = CleanupMode.ON_SUCCESS)
    lateinit var projectDir: Path

    @Test
    fun simple_android_project() {
        createSettings(projectDir, "my-project")

        createBuild(projectDir) {
            """
            import org.jetbrains.kotlin.gradle.dsl.JvmTarget
            import me.tatarka.gradle.publishing.*
                
            plugins {
                id("com.android.library") version "8.1.0"
                kotlin("android") version "1.9.20"
                id("me.tatarka.gradle.publishing.central-release-publishing")
            }
            
            group = "com.example"
            version = "1.0.0"
            
            android {
                namespace = "com.example"
                compileSdk = 34
            }
            
            kotlin {
                compilerOptions {
                    jvmTarget = JvmTarget.JVM_1_8
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

        createKotlinSource(projectDir, "com.example", "Lib") {
            """
            /**
            My Lib function
            */
            fun myLibFun() {
            }
            """.trimIndent()
        }

        copyLocalProperties(projectDir)

        assemble(projectDir)
        publish(projectDir)

        val publishDir = publishDir(projectDir)

        assertThat(publishDir).containsAllArtifacts("com.example", "my-project", "1.0.0", packaging = "aar")
    }

    @Test
    fun multi_module_android_project() {
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
                id("com.android.library") version "8.1.2" apply false
                kotlin("android") version "1.8.10" apply false
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
                id("com.android.library")
                kotlin("android")
                id("me.tatarka.gradle.publishing.central-release-publishing")
            }
            
            android {
                namespace = "com.example.project1"
                compileSdk = 34
            }
            """.trimIndent()
        }

        createBuild(project2Dir) {
            """
            plugins {
                id("com.android.library")
                kotlin("android")
                id("me.tatarka.gradle.publishing.central-release-publishing")
            }
            
            android {
                namespace = "com.example.project2"
                compileSdk = 34
            }
            """.trimIndent()
        }

        copyLocalProperties(projectDir)

        assemble(projectDir)
        publish(projectDir)

        val publishDir = publishDir(projectDir)

        assertThat(publishDir).containsAllArtifacts("com.example", "project-1", "1.0.0", packaging = "aar")
        assertThat(publishDir).containsAllArtifacts("com.example", "project-2", "1.0.0", packaging = "aar")
    }
}

private fun copyLocalProperties(projectDir: Path) {
    val localProperties = Paths.get("local.properties")
    if (localProperties.exists()) {
        Files.copy(localProperties, projectDir.resolve("local.properties"))
    }
}