package me.tatarka.gradle

import org.gradle.api.Action
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPomLicenseSpec

abstract class CentralReleasePublishingExtension {

    private val pomActions = mutableListOf<Action<MavenPomConfiguration>>()

    fun pom(action: Action<MavenPomConfiguration>) {
        pomActions.add(action)
    }

    internal fun apply(name: String, pom: MavenPom) {
        val pomConfig = MavenPomConfigurationImpl(pom)
        for (pomAction in pomActions) {
            pomAction.execute(pomConfig)
        }
        if (!pom.name.isPresent) {
            pom.name.set(name)
        }
    }
}

interface MavenPomConfiguration : MavenPom {
    fun github(user: String, project: String)

    fun github(user: String, project: String, developer: String)

    fun MavenPomLicenseSpec.apache2()

    fun MavenPomLicenseSpec.mit()
}

private class MavenPomConfigurationImpl(private val pom: MavenPom) : MavenPomConfiguration, MavenPom by pom {

    override fun github(user: String, project: String) {
        val githubUrl = "https://github.com/$user/${project}"
        pom.url.set(githubUrl)
        val gitUrl = "${url}.git"
        pom.scm {
            connection.set(gitUrl)
            developerConnection.set(gitUrl)
            url.set(gitUrl)
        }
    }

    override fun github(user: String, project: String, developer: String) {
        github(user, project)
        pom.developers {
            developer {
                id.set(user)
                name.set(developer)
            }
        }
    }

    override fun MavenPomLicenseSpec.apache2() {
        license {
            name.set("The Apache License, Version 2.0")
            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
        }
    }

    override fun MavenPomLicenseSpec.mit() {
        license {
            name.set("MIT")
            url.set("http://www.opensource.org/licenses/mit-license.php")
        }
    }
}
