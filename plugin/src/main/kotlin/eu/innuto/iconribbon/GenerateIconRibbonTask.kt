package eu.innuto.iconribbon

import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

/**
 * Writes ribbon-decorated copies of the launcher icons into a generated res folder that AGP merges
 * on top of the handwritten ones, so the originals are never modified. The work itself is done by
 * [GenerateIconRibbonWork] in a separate worker process.
 */
@CacheableTask
abstract class GenerateIconRibbonTask : DefaultTask() {

    @get:Input
    abstract val iconNames: ListProperty<String>

    /** Drawn in order, so later ribbons end up on top. */
    @get:Input
    abstract val ribbons: ListProperty<RibbonSpec>

    /** Res folders, highest priority first (as AGP reports them). Tracked through [resourceLayers]. */
    @get:Internal
    abstract val resourceDirectories: ListProperty<Directory>

    /**
     * The launcher icon files (see [iconFilePatterns]), fingerprinted per res folder in priority order.
     *
     * One flat file collection isn't enough: relative paths don't say which folder a file came from,
     * so swapping `main` and `staging` versions of `mipmap-xxxhdpi/ic_launcher.webp` would look
     * unchanged although a different file now wins. Unrelated drawables aren't tracked, and adaptive
     * foregrounds are referenced by name, never read.
     */
    @get:Nested
    val resourceLayers: List<ResourceLayer>
        get() {
            val patterns = iconFilePatterns(iconNames.get())
            return resourceDirectories.get().map { directory ->
                ResourceLayer(directory.asFileTree.matching { it.include(patterns) })
            }
        }

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Inject
    internal abstract val workerExecutor: WorkerExecutor

    @TaskAction
    fun generate() {
        // AWT needs `java.awt.headless`, which is JVM-wide: set it in a worker process of our own
        // instead of flipping it in the shared Gradle daemon (where it would also come too late if
        // something had already initialized AWT).
        val queue = workerExecutor.processIsolation { spec ->
            spec.forkOptions.systemProperty("java.awt.headless", "true")
        }
        queue.submit(GenerateIconRibbonWork::class.java) { parameters ->
            parameters.iconNames.set(iconNames)
            parameters.ribbons.set(ribbons)
            parameters.resourceDirectories.set(resourceDirectories.map { directories -> directories.map { it.asFile.path } })
            parameters.outputDirectory.set(outputDirectory)
        }
    }

    /** Icon files of one res folder; see [resourceLayers]. */
    class ResourceLayer(
        @get:InputFiles
        @get:PathSensitive(PathSensitivity.RELATIVE)
        val iconFiles: FileTree,
    )

    private companion object {
        /** Include patterns for the files [GenerateIconRibbonWork] can pick: direct children of mipmap / drawable folders. */
        fun iconFilePatterns(names: Collection<String>): List<String> =
            names.flatMap { name -> listOf("mipmap*/$name.*", "drawable*/$name.*") }
    }
}
