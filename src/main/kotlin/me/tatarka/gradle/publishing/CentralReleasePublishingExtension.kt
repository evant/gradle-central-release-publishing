package me.tatarka.gradle.publishing

import org.gradle.api.Action
import org.gradle.api.DomainObjectCollection
import org.gradle.api.NamedDomainObjectSet
import org.gradle.api.XmlProvider
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPomCiManagement
import org.gradle.api.publish.maven.MavenPomContributor
import org.gradle.api.publish.maven.MavenPomContributorSpec
import org.gradle.api.publish.maven.MavenPomDeveloper
import org.gradle.api.publish.maven.MavenPomDeveloperSpec
import org.gradle.api.publish.maven.MavenPomDistributionManagement
import org.gradle.api.publish.maven.MavenPomIssueManagement
import org.gradle.api.publish.maven.MavenPomLicense
import org.gradle.api.publish.maven.MavenPomLicenseSpec
import org.gradle.api.publish.maven.MavenPomMailingList
import org.gradle.api.publish.maven.MavenPomMailingListSpec
import org.gradle.api.publish.maven.MavenPomOrganization
import org.gradle.api.publish.maven.MavenPomRelocation
import org.gradle.api.publish.maven.MavenPomScm
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Nested
import org.gradle.kotlin.dsl.domainObjectContainer
import org.gradle.kotlin.dsl.newInstance
import javax.inject.Inject

abstract class CentralReleasePublishingExtension {

    /**
     * If true, appends '-SNAPSHOT' to the version name and publishes to the sonatype snapshot repository.
     */
    abstract val snapshot: Property<Boolean>

    /**
     * Configure default values, if done in the root project these will be picked up by subprojects.
     */
    @get:Nested
    abstract val defaults: PublicationDefaults

    /**
     * Configure default values, if done in the root project these will be picked up by subprojects.
     */
    fun defaults(action: Action<PublicationDefaults>) {
        action.execute(defaults)
    }

    /**
     * Configure publications, this will be populated with each maven publication that will be published in the current
     * project.
     */
    lateinit var publications: NamedDomainObjectSet<MavenPublication>
        internal set

    /**
     * Configure publications, this will be populated with each maven publication that will be published in the current
     * project.
     */
    fun publications(action: Action<MavenPublication>) {
        publications.configureEach(action)
    }
}

abstract class PublicationDefaults {
    /**
     * Configure default pom values, if done in the root project these will be picked up by subprojects.
     */
    @get:Nested
    abstract val pom: MavenPomDefaults

    /**
     * Configure default pom values, if done in the root project these will be picked up by subprojects.
     */
    fun pom(action: Action<MavenPomDefaults>) {
        action.execute(pom)
    }
}

abstract class MavenPomDefaults : MavenPom {

    override fun getPackaging(): String? {
        return null
    }

    override fun setPackaging(packaging: String?) {
        throw NotImplementedError("You can't set packaging defaults")
    }

    @get:Nested
    abstract val organization: MavenPomOrganization

    override fun organization(action: Action<in MavenPomOrganization>) {
        action.execute(organization)
    }

    @get:Nested
    abstract val distributionManagement: MavenPomDistributionManagementDefaults

    override fun distributionManagement(action: Action<in MavenPomDistributionManagement>) {
        action.execute(distributionManagement)
    }

    @get:Nested
    abstract val licenses: MavenPomLicenseSpecDefaults

    override fun licenses(action: Action<in MavenPomLicenseSpec>) {
        action.execute(licenses)
    }

    @get:Nested
    abstract val developers: MavenPomDeveloperSpecDefaults

    override fun developers(action: Action<in MavenPomDeveloperSpec>) {
        action.execute(developers)
    }

    @get:Nested
    abstract val contributors: MavenPomContributorSpecDefaults

    override fun contributors(action: Action<in MavenPomContributorSpec>) {
        action.execute(contributors)
    }

    @get:Nested
    abstract val scm: MavenPomScm

    override fun scm(action: Action<in MavenPomScm>) {
        action.execute(scm)
    }

    @get:Nested
    abstract val mailingLists: MavenPomMailingListSpecDefaults

    override fun mailingLists(action: Action<in MavenPomMailingListSpec>) {
        action.execute(mailingLists)
    }

    @get:Nested
    abstract val issueManagement: MavenPomIssueManagement

    override fun issueManagement(action: Action<in MavenPomIssueManagement>) {
        action.execute(issueManagement)
    }

    @get:Nested
    abstract val ciManagement: MavenPomCiManagement

    override fun ciManagement(action: Action<in MavenPomCiManagement>) {
        action.execute(ciManagement)
    }

    override fun withXml(action: Action<in XmlProvider>) {
        throw NotImplementedError("You can't set withXml defaults")
    }
}

abstract class MavenPomLicenseSpecDefaults(
    private val objects: ObjectFactory,
    private val items: DomainObjectCollection<MavenPomLicense>,
) : DomainObjectCollection<MavenPomLicense> by items, MavenPomLicenseSpec {

    @Inject
    constructor(objects: ObjectFactory) : this(objects, objects.domainObjectContainer(MavenPomLicense::class))

    override fun license(action: Action<in MavenPomLicense>) {
        items.add(objects.newInstance(MavenPomLicense::class).apply { action.execute(this) })
    }
}

abstract class MavenPomContributorSpecDefaults(
    private val objects: ObjectFactory,
    private val items: DomainObjectCollection<MavenPomContributor>,
) : DomainObjectCollection<MavenPomContributor> by items, MavenPomContributorSpec {

    @Inject
    constructor(objects: ObjectFactory) : this(objects, objects.domainObjectContainer(MavenPomContributor::class))

    override fun contributor(action: Action<in MavenPomContributor>) {
        items.add(objects.newInstance(MavenPomContributor::class).apply { action.execute(this) })
    }
}

abstract class MavenPomDeveloperSpecDefaults(
    private val objects: ObjectFactory,
    private val items: DomainObjectCollection<MavenPomDeveloper>,
) : DomainObjectCollection<MavenPomDeveloper> by items, MavenPomDeveloperSpec {

    @Inject
    constructor(objects: ObjectFactory) : this(objects, objects.domainObjectContainer(MavenPomDeveloper::class))

    override fun developer(action: Action<in MavenPomDeveloper>) {
        items.add(objects.newInstance(MavenPomDeveloper::class).apply { action.execute(this) })
    }
}

abstract class MavenPomDistributionManagementDefaults : MavenPomDistributionManagement {

    @get:Nested
    abstract val relocation: MavenPomRelocation

    override fun relocation(action: Action<in MavenPomRelocation>) {
        action.execute(relocation)
    }
}

abstract class MavenPomMailingListSpecDefaults(
    private val objects: ObjectFactory,
    private val items: DomainObjectCollection<MavenPomMailingList>,
) : DomainObjectCollection<MavenPomMailingList> by items, MavenPomMailingListSpec {

    @Inject
    constructor(objects: ObjectFactory) : this(objects, objects.domainObjectContainer(MavenPomMailingList::class))

    override fun mailingList(action: Action<in MavenPomMailingList>) {
        items.add(objects.newInstance(MavenPomMailingList::class).apply { action.execute(this) })
    }
}

internal fun mergePomDefaults(pom: MavenPom, defaults: MavenPomDefaults, projectName: String) {
    pom.name.convention(defaults.name.orElse(projectName))
    pom.description.convention(defaults.description)
    pom.url.convention(defaults.url)
    pom.inceptionYear.convention(defaults.inceptionYear)
    defaults.licenses.all(object : Action<MavenPomLicense> {
        override fun execute(default: MavenPomLicense) {
            pom.licenses {
                license {
                    name.set(default.name)
                    url.set(default.url)
                    distribution.set(default.distribution)
                    comments.set(default.comments)
                }
            }
        }
    })
    if (defaults.organization.name.isPresent || defaults.organization.url.isPresent) {
        pom.organization {
            name.convention(defaults.organization.name)
            url.convention(defaults.organization.url)
        }
    }
    defaults.developers.all(object : Action<MavenPomDeveloper> {
        override fun execute(default: MavenPomDeveloper) {
            pom.developers {
                developer {
                    id.set(default.id)
                    name.set(default.name)
                    email.set(default.email)
                    url.set(default.url)
                    organization.set(default.organization)
                    organizationUrl.set(default.organizationUrl)
                    roles.set(default.roles)
                    timezone.set(default.timezone)
                    properties.set(default.properties)
                }
            }
        }
    })
    defaults.contributors.all(object : Action<MavenPomContributor> {
        override fun execute(default: MavenPomContributor) {
            pom.contributors {
                contributor {
                    name.set(default.name)
                    email.set(default.email)
                    url.set(default.url)
                    organization.set(default.organization)
                    organizationUrl.set(default.organizationUrl)
                    roles.set(default.roles)
                    timezone.set(default.timezone)
                    properties.set(default.properties)
                }
            }
        }
    })
    if (defaults.scm.connection.isPresent || defaults.scm.developerConnection.isPresent || defaults.scm.url.isPresent ||
        defaults.scm.tag.isPresent
    ) {
        pom.scm {
            connection.convention(defaults.scm.connection)
            developerConnection.convention(defaults.scm.developerConnection)
            url.convention(defaults.scm.url)
            tag.convention(defaults.scm.tag)
        }
    }
    if (defaults.issueManagement.system.isPresent || defaults.issueManagement.url.isPresent) {
        pom.issueManagement {
            system.convention(defaults.issueManagement.system)
            url.convention(defaults.issueManagement.url)
        }
    }
    if (defaults.ciManagement.system.isPresent || defaults.ciManagement.url.isPresent) {
        pom.ciManagement {
            system.convention(defaults.ciManagement.system)
            url.convention(defaults.ciManagement.url)
        }
    }
    if (defaults.distributionManagement.downloadUrl.isPresent ||
        defaults.distributionManagement.relocation.artifactId.isPresent ||
        defaults.distributionManagement.relocation.groupId.isPresent ||
        defaults.distributionManagement.relocation.version.isPresent ||
        defaults.distributionManagement.relocation.message.isPresent
    ) {
        pom.distributionManagement {
            downloadUrl.convention(defaults.distributionManagement.downloadUrl)
            if (
                defaults.distributionManagement.relocation.artifactId.isPresent ||
                defaults.distributionManagement.relocation.groupId.isPresent ||
                defaults.distributionManagement.relocation.version.isPresent ||
                defaults.distributionManagement.relocation.message.isPresent
            ) {
                relocation {
                    artifactId.convention(defaults.distributionManagement.relocation.artifactId)
                    groupId.convention(defaults.distributionManagement.relocation.groupId)
                    version.convention(defaults.distributionManagement.relocation.version)
                    message.convention(defaults.distributionManagement.relocation.message)
                }
            }
        }
    }
    defaults.mailingLists.all(object : Action<MavenPomMailingList> {
        override fun execute(default: MavenPomMailingList) {
            pom.mailingLists {
                mailingList {
                    name.set(default.name)
                    subscribe.set(default.subscribe)
                    unsubscribe.set(default.unsubscribe)
                    post.set(default.post)
                    archive.set(default.archive)
                    otherArchives.set(default.otherArchives)
                }
            }
        }
    })
}