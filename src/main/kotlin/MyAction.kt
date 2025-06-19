import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import org.koin.core.parameter.parametersOf
import org.koin.java.KoinJavaComponent.getKoin
import projectfiles.interfaces.PluginGradle
import projectfiles.interfaces.GradleFiles
import com.intellij.openapi.ui.Messages
import projectfiles.interfaces.LibraryGradle

class MyAction(
): AnAction() {
    private lateinit var project: Project

    override fun actionPerformed(e: AnActionEvent) {
        project = e.project ?: return
        val gradleFiles: GradleFiles = getKoin().get { parametersOf(project) }

        val fileToml = gradleFiles.findFileInProject("gradle/libs.versions.toml")
        val filePluginProject = gradleFiles.findFileInProject("build.gradle.kts")
        val fileModuleProject = gradleFiles.findFileInProject("app/build.gradle.kts")

        val pluginGradle: PluginGradle = getKoin().get<PluginGradle> {
            parametersOf(fileToml, fileModuleProject, filePluginProject, project)
        }

        val libraryGradle: LibraryGradle = getKoin().get<LibraryGradle> {
            parametersOf(fileToml, fileModuleProject, project)
        }

        val filePluginProjectConvent = gradleFiles.readFileContent(filePluginProject)
        if (!filePluginProjectConvent.isNullOrEmpty()) {
            filePluginProjectConvent.lines()
                .mapIndexed { index, line -> index to line }
                .filter { (_, line) -> line.contains("id(") }
                .forEach { (index, line) ->

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

        val fileLibraryProjectContent = gradleFiles.readFileContent(fileModuleProject)
        if (!fileLibraryProjectContent.isNullOrEmpty()) {
            fileLibraryProjectContent.lines()
                .mapIndexed { index, line -> index to line }
                .filter { (_, line) ->
                    line.contains("implementation(\"") ||
                            line.contains("testImplementation(\"") ||
                            line.contains("androidTestImplementation(\"")
                }
                .forEach { (index, line) ->
                    val libraryEntry = libraryGradle.createLibraryEntry(line)
                    Messages.showInfoMessage(
                        project,
                        "Обработка строки #${index + 1}:\n$line",
                        "Найден id(...)"
                    )

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
}