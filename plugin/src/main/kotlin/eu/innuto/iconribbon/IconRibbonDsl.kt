package eu.innuto.iconribbon

import com.android.build.api.dsl.BuildType
import com.android.build.api.dsl.ProductFlavor
import org.gradle.api.Action
import org.gradle.api.plugins.ExtensionAware

/** Kotlin DSL accessor; Groovy scripts can use `iconRibbon { }` directly. */
fun ProductFlavor.iconRibbon(action: Action<IconRibbonConfig>) = configureIconRibbon(action)

/** Kotlin DSL accessor; Groovy scripts can use `iconRibbon { }` directly. */
fun BuildType.iconRibbon(action: Action<IconRibbonConfig>) = configureIconRibbon(action)

internal val ExtensionAware.iconRibbonConfig: IconRibbonConfig
    get() = extensions.getByType(IconRibbonConfig::class.java)

private fun ExtensionAware.configureIconRibbon(action: Action<IconRibbonConfig>) {
    val config = iconRibbonConfig
    // An empty `iconRibbon { }` block is enough to turn the ribbon on.
    config.enabled.convention(true)
    action.execute(config)
}
