import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.plugin.compatibility.compatibility
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "2.4.21"
    id("com.gradle.plugin-publish") version "2.2.1"
}

group = "eu.innuto"
// `version` comes from gradle.properties.

val repositoryUrl = "https://github.com/innuto/icon-ribbon-gradle-plugin"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(17)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        // Build scripts are compiled by the Kotlin embedded in the user's Gradle (1.8 in Gradle 8.0),
        // which can't read metadata from a newer Kotlin; keep the public `iconRibbon { }` accessors readable.
        apiVersion = KotlinVersion.KOTLIN_1_8
        languageVersion = KotlinVersion.KOTLIN_1_8
    }
}

dependencies {
    // Compiled against the public AGP API only; the host build supplies the real AGP at runtime.
    compileOnly("com.android.tools.build:gradle-api:8.10.1")
    // Lets ImageIO decode .webp launcher icons (writing is done as PNG).
    implementation("com.twelvemonkeys.imageio:imageio-webp:3.15.3")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    systemProperty("java.awt.headless", "true")
}

// Functional tests: real Android builds in temporary projects, run through Gradle TestKit.
val functionalTest: SourceSet by sourceSets.creating
gradlePlugin.testSourceSets(functionalTest)
configurations[functionalTest.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())
configurations[functionalTest.runtimeOnlyConfigurationName].extendsFrom(configurations.testRuntimeOnly.get())

// The AGP the test builds run with, e.g. `-PtestAgpVersion=8.4.2`. It goes onto the injected plugin
// classpath next to this plugin; applied separately, its classes would be invisible to ours.
val testAgpVersion = providers.gradleProperty("testAgpVersion").orElse("8.10.1")
val testAgp: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}
dependencies {
    testAgp(testAgpVersion.map { "com.android.tools.build:gradle:$it" })
}
tasks.pluginUnderTestMetadata {
    pluginClasspath.from(testAgp)
}

val androidSdk = providers.environmentVariable("ANDROID_HOME")
    .orElse(providers.environmentVariable("ANDROID_SDK_ROOT"))
    .orElse(
        providers.fileContents(layout.projectDirectory.file("../local.properties")).asText.map { text ->
            text.lines().first { it.startsWith("sdk.dir=") }.substringAfter("=").trim()
        },
    )

val functionalTestTask = tasks.register<Test>("functionalTest") {
    description = "Runs Android builds with the plugin applied, through Gradle TestKit."
    group = "verification"
    testClassesDirs = functionalTest.output.classesDirs
    classpath = functionalTest.runtimeClasspath
    useJUnitPlatform()
    systemProperty("java.awt.headless", "true")
    inputs.property("testAgpVersion", testAgpVersion)
    // Gradle version of the test builds, e.g. `-PtestGradleVersion=8.6`; defaults to this build's Gradle.
    providers.gradleProperty("testGradleVersion").orNull?.let {
        inputs.property("testGradleVersion", it)
        systemProperty("testGradleVersion", it)
    }
    // Lazy, and not an input: the SDK path differs between machines.
    jvmArgumentProviders.add(CommandLineArgumentProvider { listOf("-DandroidSdk=${androidSdk.get()}") })
    shouldRunAfter(tasks.test)
}
tasks.check {
    dependsOn(functionalTestTask)
}

gradlePlugin {
    website = repositoryUrl
    vcsUrl = repositoryUrl
    plugins {
        create("iconRibbon") {
            id = "eu.innuto.iconribbon"
            implementationClass = "eu.innuto.iconribbon.IconRibbonPlugin"
            displayName = "Icon Ribbon"
            description = "Draws a ribbon (e.g. DEBUG, STAGING) over Android launcher icons, " +
                "configured per product flavor and build type. Supports PNG/WebP and adaptive icons."
            tags = listOf("android", "launcher-icon", "icon", "ribbon", "banner", "flavor", "build-type")
            compatibility {
                features {
                    // Checked with the sample app (configuration cache stored and reused).
                    configurationCache = true
                }
            }
        }
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            licenses {
                license {
                    name = "MIT License"
                    url = "https://opensource.org/licenses/MIT"
                }
            }
        }
    }
}
