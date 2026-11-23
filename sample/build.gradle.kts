import eu.innuto.iconribbon.RibbonPosition
import eu.innuto.iconribbon.iconRibbon

plugins {
    id("com.android.application")
    id("eu.innuto.iconribbon")
}

android {
    namespace = "eu.innuto.iconribbon.sample"
    compileSdk = 35

    defaultConfig {
        applicationId = "eu.innuto.iconribbon.sample"
        minSdk = 24
        targetSdk = 35
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        // Stacks with the flavor ribbons: devDebug shows DEV + DEBUG, devRelease only DEV.
        debug {
            iconRibbon {

            }
        }
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            applicationIdSuffix = ".dev"
            iconRibbon {
                backgroundColor = "#2E7D32"
                position = RibbonPosition.BOTTOM
            }
        }
        create("staging") {
            applicationIdSuffix = ".staging"
            iconRibbon {
                text = "STAGE"
                backgroundColor = "#1565C0"
                position = RibbonPosition.BOTTOM
            }
        }
        create("prod")
    }
}
