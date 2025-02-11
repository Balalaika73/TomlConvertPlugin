import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.parameter.parametersOf
import org.koin.java.KoinJavaComponent.getKoin
import org.koin.java.KoinJavaComponent.inject
import java.io.File

class MyAction: AnAction() {

    private val pluginString = "id("
    private val kaptString = "kapt("
    private val moduleString = "implementation(\""
    private val runtimeOnlyString = "runtimeOnly(\""
    private var fileToml : VirtualFile? = null
    private var filePluginProject : VirtualFile? = null
    private var fileModuleProject : VirtualFile? = null
    private lateinit var project: Project

    companion object {
        init {
            startKoin {
                modules(appModule)
            }
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        project = e.project ?: return
        fileToml = findFileInProject("gradle/libs.versions.toml")
        filePluginProject = findFileInProject("build.gradle.kts")
        fileModuleProject = findFileInProject("app/build.gradle.kts")
        if (filePluginProject != null && fileToml != null && fileModuleProject != null) {
            val moduleChanges: ModuleChanges = getKoin().get(
                parameters = { parametersOf(fileToml!!, fileModuleProject!!, project) }
            )
            val pluginChanges: PluginChanges = getKoin().get(
                parameters = { parametersOf(fileToml!!, fileModuleProject!!, filePluginProject!!, project) }
            )
            // read files
            checkFile(e, filePluginProject!!,
                {it.contains(pluginString) && !it.contains("kapt")},
                { pluginChanges.convertProjectPluginToToml(it)})
            checkFile(e, fileModuleProject!!,
                {it.contains(pluginString)&& !it.contains("kapt")},
                { removeLine(fileModuleProject, it)})
            checkFile(e, fileModuleProject!!,
                {it.contains(moduleString) ||
                        it.contains(runtimeOnlyString) ||
                        it.contains(kaptString)
                },
                { moduleChanges.convertModuleToToml(it)})
        } else {
            showError(e, "File not found")
        }
    }

    private fun findFileInProject(fileName: String): VirtualFile? {
        val basePath = project.basePath ?: return null
        val targetFile = File(basePath, fileName)
        return VfsUtil.findFileByIoFile(targetFile, true)
    }

    fun removeLine(file: VirtualFile?, lineToRemove: String) {
        file?.let {
            FileDocumentManager.getInstance().getDocument(it)
        }?.let { doc ->
            WriteCommandAction.runWriteCommandAction(project) {
                val updatedText = doc.text.lines().filter { it != lineToRemove }.joinToString("\n")
                doc.setText(updatedText)
            }
        }
    }

    private fun checkFile(e: AnActionEvent, file: VirtualFile, filterCondition: (String) -> Boolean, transform: (String) -> Unit) {
        val content = readFileContent(file)

        if (!content.isNullOrEmpty()) {
            content.lines()
                .filter(filterCondition)
                .forEach(transform)
        } else {
            showError(e, "File is empty or cannot be read.")
        }
    }

    private fun readFileContent(file: VirtualFile): String? {
        val psiFile = PsiManager.getInstance(project).findFile(file)
        return psiFile?.text
    }

    private fun showError(event: AnActionEvent, e: String){
        Messages.showMessageDialog(
            event.project,
            "Error: $e",
            "Error",
            Messages.getInformationIcon());
    }
}