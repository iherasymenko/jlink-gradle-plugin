# Overview

[![Build Status](https://img.shields.io/endpoint.svg?url=https%3A%2F%2Factions-badge.atrox.dev%2Fiherasymenko%2Fjlink-gradle-plugin%2Fbadge&style=flat)](https://actions-badge.atrox.dev/iherasymenko/jlink-gradle-plugin/goto)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/com.github.iherasymenko.jlink)](https://plugins.gradle.org/plugin/com.github.iherasymenko.jlink)

This plugin provides integration with the `jlink` tool. The plugin requires Gradle to be run with JDK 11+. The minimally supported Gradle version is 6.8.

Your application has to be _fully_ modularized in order to be used with this plugin. The plugin _does not_ do any nasty tricks to fool `jlink` into working with non-modularized applications. 

If you have third party dependencies that are not modularized, you should consider using [extra-java-module-info](https://github.com/gradlex-org/extra-java-module-info) plugin in order to elegantly add the `module-info.class` descriptors to those libraries. 

The examples below assume the Gradle version is 8.4 or higher and are written in Groovy DSL.

When applied together with `java` plugin, the plugin contributes the `image`, `imageRun`, and `imageModules` tasks to the project. 

The image that is built by the `image` task is meant to be used for development purposes only as it depends on the local JDK.

If you want to distribute your application, please refer to the "Cross Target Images" section of this document.

# Usage

## With `application` plugin

If used together with `application` plugin, the plugin will infer the `mainModule`, `mainClass`, 
`applicationDefaultJvmArgs`, and `applicationName` configuration properties from the `application` extension.

```groovy
plugins {
    id 'application'
    id 'com.github.iherasymenko.jlink' version '0.5'
}

application {
    mainClass = 'com.example.demo.DemoApplication'
    mainModule = 'demo.main'
}

```

## With `java` plugin

```groovy
plugins {
    id 'java'
    id 'com.github.iherasymenko.jlink' version '0.5'
}

jlinkApplication {
    mainClass = 'com.example.demo.DemoApplication'
    mainModule = 'demo.main'
}
```

## Optional configuration

```groovy
jlinkApplication {
    addOptions = ['-Xmx8G', '-Xms8G']
    compress = 'zip-9'
    noHeaderFiles = true
    noManPages = true
    stripDebug = true
    generateCdsArchive = true
    dedupLegalNoticesErrorIfNotSameContent = true
    disablePlugin = [
        "add-options"
    ]
    excludeFiles = ['/**/legal/**', '/**/man/**']
    excludeResources = ['/**/com/example/demo/DemoApplication.class']
    includeLocales = ['en-CA']
    stripJavaDebugAttributes = true
    stripNativeCommands = true
    addModules = ['java.net.http']
    launcher = [
        'another-demo-application': 'demo.main/com.example.demo.AnotherDemoApplication'
    ]
    limitModules = ['com.zaxxer.hikari']
    vm = 'server'
    endian = 'little'
    vendorVersion = 'My JRE version 99.9'
    vendorBugUrl = 'https://github.com/iherasymenko/jlink-gradle-plugin/issues?q=label:bug'
    vendorVmBugUrl = 'https://github.com/iherasymenko/jlink-gradle-plugin/issues?q=label:vmbug'
}

```

# Cross Target Images

The plugin supports building images for multiple platforms. If you want to distribute your application, you have to be explicit 
about the JDK you want to use as the base image. `jlink` supports building cross-platform images, but it requires the JDK to be of the same 
major version as `jlink`.

The plugin provides the `jlinkImages` extension that allows you to specify the JDKs you want to use for building the images.

The JDKs will be fetched from a repository (CDN) that you have to configure. 

Below is an example of how to configure the plugin to use [Azul Zulu OpenJDK](https://cdn.azul.com/zulu/bin/) public CDN as the repository.

`build.gradle`

```groovy
plugins {
    id 'application'
    id 'com.github.iherasymenko.jlink' version '0.5'
}

application {
    mainClass = 'com.example.demo.DemoApplication'
    mainModule = 'demo.main'
}

jlinkImages {
	linuxX64 {
		group = 'com.azul.cdn'
		jdkArchive = 'zulu21.30.15-ca-jdk21.0.1-linux_x64.zip'
	}
	windowsX64 {
		group = 'com.azul.cdn'
		jdkArchive = 'zulu21.30.15-ca-jdk21.0.1-win_x64.zip'
	}
	macOsX64 {
		group = 'com.azul.cdn'
		jdkArchive = 'zulu21.30.15-ca-jdk21.0.1-macosx_x64.tar.gz'
	}
}

```
`settings.gradle`

```groovy
dependencyResolutionManagement {
    repositories {
        ivy {
            url = uri('https://cdn.azul.com/zulu/bin/')
            patternLayout {
                artifact '[artifact].[ext]'
            }
            metadataSources {
                artifact()
            }
            content {
                includeGroup 'com.azul.cdn'
            }
        }
    }
}
```

The above configuration will create three tasks: `imageLinuxX64`, `imageWindowsX64`, and `imageMacOsX64` accordingly. 
These tasks will be attached to the `assemble` task as dependencies.

Or you can use the [Eclipse Temurin™](https://adoptium.net/temurin/releases/) OpenJDK GitHub releases as the repository:

`build.gradle`
```groovy
jlinkImages {
    linuxX64 {
        group = 'net.adoptium.cdn'
        jdkArchive = 'OpenJDK21U-jdk_x64_linux_hotspot_21.0.1_12.tar.gz'
    }
}
```

`settings.gradle`
```groovy
dependencyResolutionManagement {
    repositories {
        ivy {
            url = uri('https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.1%2B12/')
            patternLayout {
                artifact '[artifact].[ext]'
            }
            metadataSources {
                artifact()
            }
            content {
                includeGroup 'net.adoptium.cdn'
            }
        }
    }
}
```

Or you can even tell the plugin to use GitHub releases of [IBM Semeru OpenJDK](https://developer.ibm.com/languages/java/semeru-runtimes/downloads/) as the repository:

`build.gradle`
```groovy
jlinkImages {
	linuxX64Semuru {
		group = 'com.ibm.semuru.cdn'
		jdkArchive = 'ibm-semeru-open-jdk_x64_linux_17.0.8.1_1_openj9-0.40.0.tar.gz'
	}
}
```

`settings.gradle`

```groovy
dependencyResolutionManagement {
    repositories {
        ivy {
            url = uri('https://github.com/ibmruntimes/semeru17-binaries/releases/download/jdk-17.0.8.1%2B1_openj9-0.40.0/')
            patternLayout {
                artifact '[artifact].[ext]'
            }
            metadataSources {
                artifact()
            }
            content {
                includeGroup 'com.ibm.semuru.cdn'
            }
        }
    }
}
```

Because this plugin delegates downloading of the JDKs to Gradle, the JDK archives will be included in the [Dependency Verification](https://docs.gradle.org/8.4/userguide/dependency_verification.html) if your project has this feature enabled.