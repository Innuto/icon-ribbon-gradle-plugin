package eu.innuto.iconribbon

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

/** Entry point, referenced by `implementationClass` in `plugin/build.gradle.kts`. */
@Suppress("unused") // Instantiated by Gradle by name; the IDE can't see that reference.
class IconRibbonPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create(IconRibbonConfig.DSL_NAME, IconRibbonExtension::class.java)
        extension.iconNames.convention(IconRibbonExtension.DEFAULT_ICON_NAMES)

        project.plugins.withId("com.android.application") {
            val android = project.extensions.getByType(ApplicationExtension::class.java)
            val androidComponents = project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)

            // Runs for existing and future flavors / build types, before their `create(...) { }` block,
            // so `iconRibbon { }` is available inside it.
            android.productFlavors.configureEach { it.extensions.create(IconRibbonConfig.DSL_NAME, IconRibbonConfig::class.java) }
            android.buildTypes.configureEach { it.extensions.create(IconRibbonConfig.DSL_NAME, IconRibbonConfig::class.java) }

            val moduleIconNames by lazy {
                extension.iconNames.get().also { requireValidIconNames(it, "the top-level iconRibbon { } block") }
            }
            val flavorConfigs = mutableMapOf<String, IconRibbonConfig>()
            val buildTypeConfigs = mutableMapOf<String, IconRibbonConfig>()
            androidComponents.finalizeDsl { dsl ->
                dsl.productFlavors.forEach { flavor ->
                    flavorConfigs[flavor.name] = flavor.iconRibbonConfig.apply { applyConventions(flavor.name) }
                }
                dsl.buildTypes.forEach { buildType ->
                    buildTypeConfigs[buildType.name] = buildType.iconRibbonConfig.apply { applyConventions(buildType.name) }
                }
            }

            androidComponents.onVariants { variant ->
                // Flavor ribbons (in dimension order) first, the build type's last so it is drawn on top.
                val chain = variant.productFlavors.map { (_, flavor) -> flavorConfigs.getValue(flavor) } +
                    listOfNotNull(variant.buildType?.let(buildTypeConfigs::getValue))
                val configured = chain.filter { it.enabled.isPresent }
                // Any explicit `enabled = false` in the chain keeps the whole variant clean.
                if (configured.isNotEmpty() && configured.all { it.enabled.get() }) {
                    // Flavor / build type lists replace the module-wide one; several of them are combined.
                    val iconNames = configured.flatMap { it.iconNames.getOrElse(emptyList()) }.distinct()
                        .ifEmpty { moduleIconNames }
                    registerFor(project, configured, iconNames, variant)
                }
            }
        }
    }

    private fun registerFor(
        project: Project,
        ribbons: List<IconRibbonConfig>,
        iconNames: List<String>,
        variant: ApplicationVariant,
    ) {
        ribbons.groupBy { it.position.get() }.forEach { (position, sharing) ->
            if (sharing.size > 1) {
                throw GradleException(
                    "icon-ribbon: ${variant.name} would draw ${sharing.joinToString(" and ") { "'${it.ownerName}'" }} " +
                        "on top of each other at $position. Give one of them a different `position`.",
                )
            }
        }
        val specs = ribbons.map { it.toSpec() }

        val resources = variant.sources.res ?: return
        val taskName = "generate${variant.name.replaceFirstChar(Char::uppercase)}IconRibbon"
        val task = project.tasks.register(taskName, GenerateIconRibbonTask::class.java) { task ->
            task.description = "Draws the ${specs.joinToString { it.text }} ribbon(s) over the launcher icons of ${variant.name}."
            task.iconNames.set(iconNames)
            task.ribbons.set(specs)
            // Only the hand-written res folders: `all` would include our own output and form a cycle.
            val resourceDirectories = resources.static.map { layers -> layers.flatten() }
            task.resourceDirectories.set(resourceDirectories)
            // Some of these folders are generated (e.g. resValues); `resourceDirectories` is @Internal,
            // so wire their producer tasks explicitly.
            task.dependsOn(resourceDirectories)
        }
        resources.addGeneratedSourceDirectory(task, GenerateIconRibbonTask::outputDirectory)
    }
}
