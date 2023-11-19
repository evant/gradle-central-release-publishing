@file:JvmName("PomExtensions")

package me.tatarka.gradle.publishing

import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPomLicenseSpec
import org.w3c.dom.Element
import org.w3c.dom.NodeList

/**
 * Sets `<url>` and `<scm>` values to a GitHub url pointing to the given user and project names.
 *
 * @param user the GitHub user
 * @param project the GitHub project
 */
fun MavenPom.github(user: String, project: String) {
    val githubUrl = "https://github.com/$user/${project}"
    url.set(githubUrl)
    val gitUrl = "${githubUrl}.git"
    scm {
        connection.set(gitUrl)
        developerConnection.set(gitUrl)
        url.set(gitUrl)
    }
}

/**
 * Sets `<url>` and `<scm>` values to a GitHub url pointing to the given user and project names.
 * Also adds `<developer>` with the given developer name.
 *
 * @param user the GitHub user
 * @param project the GitHub project
 * @param developer the name of the developer
 */
fun MavenPom.github(user: String, project: String, developer: String) {
    github(user, project)
    developers {
        developer {
            id.set(user)
            name.set(developer)
        }
    }
}

/**
 * Adds an Apache 2.0 license.
 * ```
 * <license>
 *   <name>The Apache License, Version 2.0</name>
 *   <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
 * </license>
 * ```
 */
fun MavenPomLicenseSpec.apache2() {
    license {
        name.set("The Apache License, Version 2.0")
        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
    }
}

/**
 * Adds an MIT license.
 *
 * ```
 * <license>
 *   <name>MIT License</name>
 *   <url>http://www.opensource.org/licenses/mit-license.php</url>
 * </license>
 * ```
 */
fun MavenPomLicenseSpec.mit() {
    license {
        name.set("MIT License")
        url.set("http://www.opensource.org/licenses/mit-license.php")
    }
}

internal inline fun NodeList.forEachElement(body: (Element) -> Unit) {
    for (i in 0 until length) {
        val item = item(i)
        if (item is Element) {
            body(item)
        }
    }
}