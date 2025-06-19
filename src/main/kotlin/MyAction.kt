import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import org.koin.core.parameter.parametersOf
import org.koin.java.KoinJavaComponent.getKoin
import projectfiles.interfaces.PluginGradle
import projectfiles.interfaces.GradleFiles
import com.intellij.openapi.ui.Messages
import entries.PluginEntry
import projectfiles.interfaces.LibraryGradle
import ui.ListDialog

class MyAction(
): AnAction() {
    private lateinit var project: Project
    private lateinit var gradleFiles: GradleFiles
    private lateinit var fileToml: Document
    private lateinit var filePluginProject: Document
    private lateinit var fileModuleProject: Document
    private lateinit var libraryGradle: LibraryGradle
    private lateinit var pluginGradle: PluginGradle

    override fun actionPerformed(e: AnActionEvent) {
        project = e.project ?: return
        gradleFiles = getKoin().get { parametersOf(project) }

        fileToml = gradleFiles.findFileInProject("gradle/libs.versions.toml")
        filePluginProject = gradleFiles.findFileInProject("build.gradle.kts")
        fileModuleProject = gradleFiles.findFileInProject("app/build.gradle.kts")

        pluginGradle = getKoin().get<PluginGradle> {
            parametersOf(fileToml, fileModuleProject, filePluginProject, project)
        }

        libraryGradle = getKoin().get<LibraryGradle> {
            parametersOf(fileToml, fileModuleProject, project)
        }

        val pluginsList = getPluginsImplementations()
        val librariesList = getLibrariesImplementations()

        val dialog = ListDialog(librariesList, pluginsList)
        if (dialog.showAndGet()) {
            val selectedPlugins = dialog.getSelectedPlugins()
            val selectedLibraries = dialog.getSelectedLibraries()

            processPluginsImplementation(selectedPlugins)
            processLibrariesImplementation(selectedLibraries)
        }
    }

    private fun getPluginsImplementations(): List<Pair<Int, String>> {
        val fileContent = gradleFiles.readFileContent(filePluginProject) ?: return emptyList()

        return fileContent.lines()
            .mapIndexed { index, line -> index to line.trim() }
            .filter { (_, line) -> line.startsWith("id(") }
            .map { (index, line) -> index to line }
    }

    private fun getLibrariesImplementations(): List<Pair<Int, String>> {
        val fileContent = gradleFiles.readFileContent(fileModuleProject)

        return fileContent.lines()
            .mapIndexed { index, line -> index to line }
            .filter { (_, line) ->
                line.contains("implementation(\"") ||
                        line.contains("testImplementation(\"") ||
                        line.contains("androidTestImplementation(\"") ||
                        line.contains("runtimeOnly(\"") ||
                        line.contains("debugImplementation(\"") ||
                        line.contains("ksp(\"")
            }
    }

    private fun processPluginsImplementation(pluginsList: List<Pair<Int, String>>) {
        val errors = mutableListOf<String>()

        WriteCommandAction.runWriteCommandAction(project) {
            pluginsList.forEach { (index, line) ->
                val pluginEntry = try {
                    pluginGradle.createPluginEntry(line)
                } catch (e: Exception) {
                    errors.add("Ошибка создания плагина '$line': ${e.message}")
                    return@forEach
                }

                try {
                    pluginGradle.writePluginToToml(pluginEntry)
                    pluginGradle.writePluginToProjectGradle(pluginEntry, index)
                    pluginGradle.writePluginToModuleGradle(pluginEntry)
                } catch (e: Exception) {
                    errors.add("Ошибка записи плагина '$line': ${e.message}")
                }
            }
        }

        if (errors.isNotEmpty()) {
            showError(errors)
        }
    }

    private fun processLibrariesImplementation(libreriesList:  List<Pair<Int, String>>) {
        val errors = mutableListOf<String>()
        libreriesList.forEach { (index, line) ->
            val libraryEntry = try {
                libraryGradle.createLibraryEntry(line)
            } catch (e: Exception) {
                errors.add("Ошибка создания библиотеки '$line': ${e.message}")
                return@forEach
            }

            try {
                WriteCommandAction.runWriteCommandAction(project) {
                    libraryGradle.writeLibraryToToml(libraryEntry)
                    libraryGradle.writeLibraryToModuleGradle(libraryEntry, index)
                }
            } catch (e: Exception) {
                errors.add("Ошибка создания библиотеки \n${e.message}")
            }
        }
        if (errors.isNotEmpty()) {
            showError(errors)
        }
    }

    private fun showError(errors: MutableList<String>) {
        Messages.showErrorDialog(
            project,
            "Произошли ошибки:\n${errors.joinToString("\n")}",
            "Ошибки"
        )
    }
}