package eu.innuto.iconribbon

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * Ribbon settings attached to every product flavor and build type as `iconRibbon { }`:
 * ```
 * android {
 *     productFlavors {
 *         create("staging") {
 *             iconRibbon { text = "STAGING"; backgroundColor = "#1565C0" }
 *         }
 *     }
 *     buildTypes {
 *         debug { iconRibbon { } }      // "DEBUG", red, top-right
 *     }
 *     // stagingDebug: STAGING + DEBUG, stagingRelease: STAGING
 * }
 * ```
 * Ribbons stack: `stagingDebug` above gets both the flavor's and the build type's ribbon, which
 * must use different [position]s. Flavors and build types without `iconRibbon { }` add nothing.
 */
abstract class IconRibbonConfig {

    /** Name of the flavor / build type this belongs to, for error messages. */
    internal var ownerName: String = ""

    /** Defaults to the flavor / build type name in upper case. */
    abstract val text: Property<String>

    /** `#RRGGBB` or `#AARRGGBB`. Defaults to red. */
    abstract val backgroundColor: Property<String>

    /** Defaults to white. */
    abstract val textColor: Property<String>

    /** Defaults to [RibbonPosition.TOP_RIGHT]. */
    abstract val position: Property<RibbonPosition>

    /**
     * Replaces the module-wide [IconRibbonExtension.iconNames] for variants of this flavor / build type,
     * e.g. when a flavor ships its own `ic_launcher_dev`. If several in a variant set it, their names
     * are combined. Empty (the default) means "use the module-wide list".
     */
    abstract val iconNames: ListProperty<String>

    /** `false` suppresses every ribbon of the variant, e.g. to keep `prodDebug` clean despite a debug ribbon. */
    abstract val enabled: Property<Boolean>

    /** Fills in the defaults once the DSL is final; [ownerName] is the flavor / build type name. */
    internal fun applyConventions(ownerName: String) {
        this.ownerName = ownerName
        requireValidIconNames(iconNames.getOrElse(emptyList()), "'$ownerName'")
        // Groovy's `iconRibbon { text = ... }` bypasses the Kotlin accessor, so any setting turns it on.
        val anySet = text.isPresent || backgroundColor.isPresent || textColor.isPresent || position.isPresent ||
            iconNames.getOrElse(emptyList()).isNotEmpty()
        if (anySet) enabled.convention(true)
        text.convention(ownerName.uppercase())
        backgroundColor.convention(DEFAULT_BACKGROUND_COLOR)
        textColor.convention(DEFAULT_TEXT_COLOR)
        position.convention(RibbonPosition.TOP_RIGHT)
    }

    /** Resolved, serializable form used as task input; fails early on malformed colors. */
    internal fun toSpec(): RibbonSpec {
        val spec = RibbonSpec(text.get(), backgroundColor.get(), textColor.get(), position.get())
        runCatching { spec.toStyle() }.onFailure { error ->
            throw IllegalArgumentException("${error.message} (in iconRibbon of '$ownerName')", error)
        }
        return spec
    }

    internal companion object {
        const val DSL_NAME = "iconRibbon"
        const val DEFAULT_BACKGROUND_COLOR = "#D32F2F"
        const val DEFAULT_TEXT_COLOR = "#FFFFFF"
    }
}
