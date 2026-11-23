package eu.innuto.iconribbon

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.io.File
import javax.imageio.ImageIO

/**
 * Body of [GenerateIconRibbonTask], run in a headless worker process.
 *
 * - Raster icons (png/webp/jpg) get the ribbons painted straight into the bitmap.
 * - Adaptive icons get a new foreground: a layer-list of the original foreground plus one overlay
 *   bitmap carrying all ribbons. Background and monochrome layers are kept as they are.
 */
internal abstract class GenerateIconRibbonWork : WorkAction<GenerateIconRibbonWork.Parameters> {

    interface Parameters : WorkParameters {
        val iconNames: ListProperty<String>
        val ribbons: ListProperty<RibbonSpec>

        /** Absolute paths, highest priority first. */
        val resourceDirectories: ListProperty<String>
        val outputDirectory: DirectoryProperty
    }

    private val logger = Logging.getLogger(GenerateIconRibbonWork::class.java)

    override fun execute() {
        // Registers the WebP reader; the worker JVM is ours, so touching ImageIO's global registry is fine.
        ImageIO.scanForPlugins()

        val output = parameters.outputDirectory.get().asFile
        output.deleteRecursively()
        output.mkdirs()

        val iconNames = parameters.iconNames.get()
        val styles = parameters.ribbons.get().map(RibbonSpec::toStyle)
        val icons = findIcons(parameters.resourceDirectories.get().map(::File), iconNames.toSet())
        if (icons.isEmpty()) {
            logger.warn("icon-ribbon: no launcher icons named $iconNames found, nothing to decorate.")
            return
        }

        var overlayWritten = false
        icons.forEach { icon ->
            val target = File(output, "${icon.parentFile.name}/${icon.nameWithoutExtension}")
            when (icon.extension.lowercase()) {
                in RASTER_EXTENSIONS -> decorateRaster(icon, File(target.path + ".png"), styles)
                "xml" -> {
                    val adaptiveIcon = AdaptiveIcon.parseOrNull(icon)
                    if (adaptiveIcon == null) {
                        logger.warn("icon-ribbon: ${icon.path} is not an adaptive icon, skipped.")
                        return@forEach
                    }
                    val foregroundName = foregroundDrawableName(icon)
                    File(target.path + ".xml").writeParent(adaptiveIcon.withForeground("@drawable/$foregroundName"))
                    File(output, "drawable/$foregroundName.xml")
                        .writeParent(adaptiveIcon.foregroundLayerList(overlay = "@drawable/$OVERLAY_NAME"))
                    if (!overlayWritten) {
                        val overlay = RibbonRenderer.renderAdaptiveOverlay(styles)
                        File(output, "drawable-nodpi/$OVERLAY_NAME.png").also { it.parentFile.mkdirs() }
                            .let { ImageIO.write(overlay, "png", it) }
                        overlayWritten = true
                    }
                }
                else -> logger.warn("icon-ribbon: unsupported icon format ${icon.path}, skipped.")
            }
        }
    }

    /**
     * For each (folder, name) pair keeps the file from the highest-priority res directory, mirroring
     * how AGP's resource merger resolves overrides (e.g. `src/staging/res` over `src/main/res`).
     */
    private fun findIcons(resourceDirectories: List<File>, names: Set<String>): Collection<File> {
        val winners = LinkedHashMap<String, File>()
        resourceDirectories.filter { it.isDirectory }.forEach { resourceDirectory ->
            resourceDirectory.listFiles { file ->
                file.isDirectory && (file.name.startsWith("mipmap") || file.name.startsWith("drawable"))
            }
                .orEmpty()
                .forEach { folder ->
                    folder.listFiles { file -> file.isFile && file.nameWithoutExtension in names }
                        .orEmpty()
                        .forEach { winners.putIfAbsent("${folder.name}/${it.nameWithoutExtension}", it) }
                }
        }
        return winners.values
    }

    private fun decorateRaster(source: File, target: File, styles: List<RibbonStyle>) {
        val image = ImageIO.read(source)
        if (image == null) {
            logger.warn("icon-ribbon: could not decode ${source.path}, skipped.")
            return
        }
        target.parentFile.mkdirs()
        ImageIO.write(RibbonRenderer.decorate(image, styles), "png", target)
    }

    private fun File.writeParent(content: String) {
        parentFile.mkdirs()
        writeText(content)
    }

    companion object {
        private val RASTER_EXTENSIONS = setOf("png", "webp", "jpg", "jpeg")
        private const val FOREGROUND_SUFFIX = "_ribbon_foreground"
        private const val OVERLAY_NAME = "icon_ribbon_overlay"

        /**
         * Name of the generated foreground layer-list for [icon]. It includes the source folder, since the
         * same icon can exist as an adaptive icon in several folders (`mipmap-anydpi-v26`, `mipmap-anydpi`,
         * `mipmap-night-anydpi-v26`, ...) with different foregrounds, and all layer-lists land in `drawable/`.
         */
        fun foregroundDrawableName(icon: File): String {
            // Resource names allow only [a-z0-9_]; qualifiers can contain '-', '+' and upper case (b+sr+Latn).
            val folder = icon.parentFile.name.lowercase().replace(Regex("[^a-z0-9_]"), "_")
            return "${icon.nameWithoutExtension}${FOREGROUND_SUFFIX}_$folder"
        }
    }
}
