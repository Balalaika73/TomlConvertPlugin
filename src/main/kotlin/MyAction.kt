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
            .mapIndexed { index, line -> index to line }
            .filter { (_, line) -> line.contains("id(\"") }
            .mapNotNull { (index, line) ->
                // Извлекаем название плагина между кавычками
                val pluginName = line.substringAfter("id(\"").substringBefore('"')
                if (pluginName.isNotEmpty()) index to pluginName else null
            }
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

    fun processPluginsImplementation(pluginsList:  List<Pair<Int, String>>) {
            pluginsList.forEach { (index, line) ->

                    val pluginEntry = pluginGradle.createPluginEntry(line)

                    try {
                        WriteCommandAction.runWriteCommandAction(project){
                            pluginGradle.writePluginToToml(pluginEntry)
                            pluginGradle.writePluginToProjectGradle(pluginEntry, index+1)
                            pluginGradle.writePluginToModuleGradle(pluginEntry, index+1)
                        }
                    } catch (e: Exception) {
                        Messages.showInfoMessage(
                            project,
                            "Ошибка ${e.message}",
                            "Создание плагина"
                        )
                    }
            }
    }

    fun processLibrariesImplementation(libreriesList:  List<Pair<Int, String>>) {
        libreriesList.forEach { (index, line) ->
            val libraryEntry = libraryGradle.createLibraryEntry(line)

            try {
                WriteCommandAction.runWriteCommandAction(project) {
                    libraryGradle.writeLibraryToToml(libraryEntry)
                    libraryGradle.writeLibraryToModuleGradle(libraryEntry, index)
                }
            } catch (e: Exception) {
                Messages.showInfoMessage(
                    project,
                    "Ошибка ${e.message}",
                    "Создание библиотеки"
                )
            }
        }
    }
}