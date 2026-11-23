import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "2.1.10"
    id("com.gradle.plugin-publish") version "1.3.1"
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
    implementation("com.twelvemonkeys.imageio:imageio-webp:3.12.0")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    systemProperty("java.awt.headless", "true")
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
