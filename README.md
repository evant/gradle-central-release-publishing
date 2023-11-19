# Gradle central-release-publishing

An opinionated gradle plugin to manage publishing to maven central. Applying this plugin will:

- configure publishing to sonatype snapshot and release repositories
- configure artifact signing
- validate the pom files have all the required values

It currently supports the following project types:

- org.jetbrains.kotlin.jvm (including gradle plugins)
- org.jetbrains.kotlin.multiplatform
- com.android.library

## Usage

### Single Project Setup

Apply the plugin in your `build.gradle`

```kotlin
import me.tatarka.gradle.publishing.*

plugins {
    id("me.tatarka.gradle.publishing.central-release-publishing") version "0.1.0"
}
```

Set your group and version

```kotlin
group = "com.example"
version = "1.0.0"
```

Configure the default pom values

```kotlin
centralReleasePublishing {
    defaults {
        pom {
            description = "My Project description"
            // helper to set up with GitHub urls
            github("username", "project-name", "My Name")
            licences {
                // helper for common licences
                apache2()
            }
        }
    }
}
```

### Multi-Project Setup

Apply the plugin to your root `build.gradle` file, you may optionally set up default pom values that are common across
subprojects.

```kotlin
import me.tatarka.gradle.publishing.*

plugins {
    id("me.tatarka.gradle.publishing.central-release-publishing") version "0.1.0"
}

group = "com.example"
version = "1.0.0"

centralReleasePublishing {
    defaults {
        pom {
            description = "My Project description"
            // helper to set up with GitHub urls
            github("username", "project-name", "My Name")
            licences {
                // helper for common licences
                apache2()
            }
        }
    }
}
```

Then in each subproject `build.gradle` you want to publish, apply the plugin there as well.

```kotlin
plugins {
    id("me.tatarka.gradle.publishing.central-release-publishing")
}
```

By default, values will be taken from your root project. You may to declare a `centralReleasePublishing` in your
subproject to override any.

### Configuration

You can easily declare a version as a snapshot. This will append `-SNAPSHOT` to the version and publish to the snapshot
sonatype repo. In a multi-project setup, you only need to set this in the root, subprojects will inherit it as a default.

```kotlin
centralReleasePublishing {
    snapshot = System.getenv("CI_TAG") == null
}
```

You can configure any of the publications that are created. Note, the plugin does nothing special to make sure your
configuration happens at a particular time, some publications may require you to wrap you logic in `afterEvaluate`. One
known case of this is configuring the pom file for the
[gradle plugin marker artifact](https://discuss.gradle.org/t/how-can-i-customize-the-pom-of-the-plugin-marker-artifacts/33053).

```kotlin
centralReleasePublishing {
    publications {
        // configure the publication, adding artifacts, configuring the pom file, etc.
    }
}
```

This plugin uses the [Gradle Nexus Publish Plugin](https://github.com/gradle-nexus/publish-plugin) to publish, you can
configure it directly in your root project if you need to.

```kotlin
nexusPublishing {
    repositories {
        sonatype {  //only for users registered in Sonatype after 24 Feb 2021
            nexusUrl = uri("https://s01.oss.sonatype.org/service/local/")
            snapshotRepositoryUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        }
    }
}
```