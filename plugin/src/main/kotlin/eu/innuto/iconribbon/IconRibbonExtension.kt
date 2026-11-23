package eu.innuto.iconribbon

import org.gradle.api.provider.ListProperty

/**
 * Module-wide settings, at the top level of the build script:
 * ```
 * iconRibbon {
 *     iconNames = listOf("ic_launcher", "ic_launcher_round", "ic_launcher_alt")
 * }
 * ```
 * Ribbons themselves are configured per flavor / build type, see [IconRibbonConfig].
 */
abstract class IconRibbonExtension {

    /**
     * Resource names (no folder, no extension) of the launcher icons to decorate.
     * Defaults to `ic_launcher` and `ic_launcher_round`. A flavor / build type can replace it with
     * [IconRibbonConfig.iconNames].
     */
    abstract val iconNames: ListProperty<String>

    internal companion object {
        val DEFAULT_ICON_NAMES = listOf("ic_launcher", "ic_launcher_round")
    }
}

private val RESOURCE_NAME = Regex("[a-z0-9_]+")

/** Fails on values that can't be a resource name, e.g. `ic_launcher.png` or `@mipmap/ic_launcher`. */
internal fun requireValidIconNames(names: List<String>, owner: String) {
    val invalid = names.filterNot { RESOURCE_NAME.matches(it) }
    require(invalid.isEmpty()) {
        "icon-ribbon: iconNames of $owner must be plain resource names like 'ic_launcher' " +
            "(no folder, extension or '@mipmap/'), was ${invalid.joinToString { "'$it'" }}"
    }
}
