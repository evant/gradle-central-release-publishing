package me.tatarka.gradle.publishing

import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

class InvalidPomException(message: String) : InvalidUserDataException("invalid pom: $message")

abstract class ValidatePomForMavenCentral : DefaultTask() {

    init {
        group = "Publishing"
        description = "Validates a maven pom file has all the required fields to publish to maven central"
    }

    @get:InputFile
    abstract val pomFile: RegularFileProperty


    @TaskAction
    fun validate() {
        val projectElement = DocumentFactory.newDocumentBuilder().parse(pomFile.asFile.get()).documentElement
        projectElement.validateProject()
    }


    private companion object {
        @JvmStatic
        val DocumentFactory: DocumentBuilderFactory by lazy {
            DocumentBuilderFactory.newInstance().apply {
                setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true)
                isValidating = false
            }
        }

        const val GroupId = "groupId"
        const val ArtifactId = "artifactId"
        const val Version = "version"
        const val Name = "name"
        const val Description = "description"
        const val Url = "url"
        const val Licenses = "licenses"
        const val License = "license"
        const val Developers = "developers"
        const val Developer = "developer"
        const val Scm = "scm"
        const val Connection = "connection"
        const val DeveloperConnection = "developerConnection"

        @JvmStatic
        fun Element.validateProject() {
            val visited = mutableListOf<String>()
            childNodes.forEachElement { child ->
                when (val name = child.nodeName) {
                    GroupId, ArtifactId, Name, Description, Url -> {
                        child.requireText()
                        visited += name
                    }

                    Version -> {
                        child.requireText()
                        if (child.textContent == Project.DEFAULT_VERSION) {
                            throw InvalidPomException("version is not set")
                        }
                        visited += name
                    }

                    Licenses -> {
                        child.validateLicenses()
                        visited += name
                    }

                    Developers -> {
                        child.validateDevelopers()
                        visited += name
                    }

                    Scm -> {
                        child.validateScm()
                        visited += name
                    }
                }
            }
            visited.requireVisited(GroupId)
            visited.requireVisited(ArtifactId)
            visited.requireVisited(Version)
            visited.requireVisited(Name)
            visited.requireVisited(Description)
            visited.requireVisited(Url)
            visited.requireVisited(Licenses)
            visited.requireVisited(Developers)
            visited.requireVisited(Scm)
        }

        @JvmStatic
        fun Element.validateLicenses() {
            var licenseFound = false
            childNodes.forEachElement { child ->
                if (child.nodeName == License) {
                    licenseFound = true
                    child.validateLicense()
                }
            }
            if (!licenseFound) {
                throw InvalidPomException("license is missing")
            }
        }

        @JvmStatic
        fun Element.validateLicense() {
            val visited = mutableListOf<String>()
            childNodes.forEachElement { child ->
                when (val name = child.nodeName) {
                    Name, Url -> {
                        child.requireText(License)
                        visited += name
                    }
                }
            }
            visited.requireVisited(Name, License)
            visited.requireVisited(Url, License)
        }

        @JvmStatic
        fun Element.validateDevelopers() {
            var developerFound = false
            childNodes.forEachElement { child ->
                if (child.nodeName == Developer) {
                    developerFound = true
                    child.validateDeveloper()
                }
            }
            if (!developerFound) {
                throw InvalidPomException("developer is missing")
            }
        }

        @JvmStatic
        fun Element.validateDeveloper() {
            val visited = mutableListOf<String>()
            childNodes.forEachElement { child ->
                when (val name = child.nodeName) {
                    Name -> {
                        child.requireText(Developer)
                        visited += name
                    }
                }
            }
            visited.requireVisited(Name, Developer)
        }

        @JvmStatic
        fun Element.validateScm() {
            val visited = mutableListOf<String>()
            childNodes.forEachElement { child ->
                when (val name = child.nodeName) {
                    Connection, DeveloperConnection, Url -> {
                        child.requireText()
                        visited += name
                    }
                }
            }
            visited.requireVisited(Connection, Scm)
            visited.requireVisited(DeveloperConnection, Scm)
            visited.requireVisited(Url, Scm)
        }


        @Suppress("NOTHING_TO_INLINE")
        inline fun Element.requireText(namespace: String? = null) {
            if (textContent.isNullOrBlank()) {
                throw InvalidPomException(buildString {
                    if (namespace != null) {
                        append(namespace)
                        append('.')
                    }
                    append(nodeName)
                    append(" is blank")
                })
            }
        }

        @Suppress("NOTHING_TO_INLINE")
        inline fun List<String>.requireVisited(name: String, namespace: String? = null) {
            if (name !in this) {
                throw InvalidPomException(buildString {
                    if (namespace != null) {
                        append(namespace)
                        append('.')
                    }
                    append(name)
                    append(" is missing")
                })
            }
        }
    }
}