import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import org.koin.core.parameter.parametersOf
import org.koin.java.KoinJavaComponent.getKoin
import projectfiles.interfaces.PluginGradle
import projectfiles.interfaces.GradleFiles
import com.intellij.openapi.ui.Messages

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

        val content = gradleFiles.readFileContent(filePluginProject)
        if (!content.isNullOrEmpty()) {
            content.lines()
                .mapIndexed { index, line -> index to line } // сначала пронумеровали все строки
                .filter { (_, line) -> line.contains("id(") } // потом отфильтровали
                .forEach { (index, line) ->

                    val pluginEntry = pluginGradle.createPluginEntry(line)

                    try {
                        WriteCommandAction.runWriteCommandAction(project){
                            pluginGradle.writePluginToToml(pluginEntry)
                            pluginGradle.writePluginToAppGradle(pluginEntry, index+1)
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
    }
}