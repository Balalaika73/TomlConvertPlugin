import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import java.io.File

class MyAction: AnAction() {
    private var fileToml : VirtualFile? = null
    private var filePluginProject : VirtualFile? = null
    private var fileModuleProject : VirtualFile? = null
    private lateinit var project: Project

    override fun actionPerformed(e: AnActionEvent) {
        project = e.project ?: return
        fileToml = findFileInProject("gradle/libs.versions.toml")
        filePluginProject = findFileInProject("build.gradle.kts")
        fileModuleProject = findFileInProject("app/build.gradle.kts")
        if (filePluginProject != null && fileToml != null && fileModuleProject != null) {
            // read files
            checkFile(e, filePluginProject!!,
                {it.contains("id(")},
                { convertProjectPluginToToml(it)})
            checkFile(e, fileModuleProject!!,
                {it.contains("id(")},
                { removeLine(fileModuleProject, it)})
            checkFile(e, fileModuleProject!!,
                {it.contains("implementation(\"")},
                { convertModuleToToml(it)})
        } else {
            showError(e, "File not found")
        }
    }

    private fun findFileInProject(fileName: String): VirtualFile? {
        val basePath = project.basePath ?: return null
        val targetFile = File(basePath, fileName)
        return VfsUtil.findFileByIoFile(targetFile, true)
    }

    private fun convertModuleToToml(line: String) {
        val implParts = line.split(':')
        val groupImpl = implParts[0].split('"')[1]
        val nameImpl = implParts[1]
        val versImpl = implParts[2].dropLast(2)

        var versName = groupImpl.split('.').last() + "Version"
        val resString = "$nameImpl = {group = \"$groupImpl\", name = \"$nameImpl\", version.ref = \"$versName\" }"

        versName = "$versName = \"$versImpl\""

        val implId = nameImpl.replace('-', '.')

        writeModuleToToml(resString, versName, implId)
        removeLine(fileModuleProject, line)
    }

    private fun writePluginToToml(tomlEntry: String, versionLine: String, pluginId: String) {
        val tomlDocument = fileToml?.let { FileDocumentManager.getInstance().getDocument(it) }
        val pluginDocument = filePluginProject?.let { FileDocumentManager.getInstance().getDocument(it) }
        val moduleDocument = fileModuleProject?.let { FileDocumentManager.getInstance().getDocument(it) }

        WriteCommandAction.runWriteCommandAction(project) {
            tomlDocument!!.let { doc ->
                val librariesIndex = doc.text.indexOf("[libraries]")
                if (librariesIndex != -1) {
                    doc.insertString(librariesIndex, "\n$versionLine\n")
                    doc.insertString(doc.textLength, "\n$tomlEntry")
                }
            }

            pluginDocument!!.let { doc ->
                val pluginsBlockEnd = doc.text.lastIndexOf("}")
                if (pluginsBlockEnd != -1) {
                    val aliasLine = "\n\talias(libs.plugins.$pluginId) apply false"
                    doc.insertString(pluginsBlockEnd - 1, aliasLine)
                }
            }

            moduleDocument!!.let { doc ->
                val pluginsBlockEnd = doc.text.indexOf("}")
                if (pluginsBlockEnd != -1) {
                    val aliasLine = "\n\talias(libs.plugins.$pluginId)"
                    doc.insertString(pluginsBlockEnd - 1, aliasLine)
                }
            }
        }
    }

    private fun writeModuleToToml(tomlEntry: String, versionLine: String, pluginId: String) {
        val tomlDocument = fileToml?.let { FileDocumentManager.getInstance().getDocument(it) }
        val moduleDocument = fileModuleProject?.let { FileDocumentManager.getInstance().getDocument(it) }

        WriteCommandAction.runWriteCommandAction(project) {
            tomlDocument?.let { doc ->
                val versionLineToAdd = "\n$versionLine\n"
                val tomlEntryToAdd = "\n$tomlEntry"

                val versIndex = doc.text.indexOf("[libraries]")
                if (versIndex != -1 && !doc.text.contains(versionLine)) {
                    doc.insertString(versIndex, versionLineToAdd)
                }

                val librariesIndex = doc.text.indexOf("[plugins]")
                if (librariesIndex != -1 && !doc.text.contains(tomlEntry)) {
                    doc.insertString(librariesIndex-1, tomlEntryToAdd)
                }
            }


            moduleDocument?.let { doc ->
                val pluginsBlockEnd = doc.text.lastIndexOf("}")
                if (pluginsBlockEnd != -1) {
                    val aliasLine = "\n\timplementation(libs.$pluginId)"
                    doc.insertString(pluginsBlockEnd - 1, aliasLine)
                }
            }
        }
    }

    private fun removeLine(file: VirtualFile?, lineToRemove: String) {
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

    private fun convertProjectPluginToToml(line: String) {
        val pluginRegex = """id\("([^"]+)""".toRegex()
        val versionRegex = """version\s+"([^"]+)"""".toRegex()

        var pluginId = pluginRegex.find(line)?.groups?.get(1)?.value ?: "Unknown"

        val startDotIndex = pluginId.indexOf('.', pluginId.indexOf('.') + 2)
        val lastDotIndex = pluginId.lastIndexOf('.')

        // form plugin name b vers name
        val pluginName = pluginId.substring(startDotIndex + 1).replace('.', '-')
        val pluginVersionName = "${pluginId.substring(lastDotIndex + 1)}Version"
        val tomlEntry = "$pluginName = { id = \"$pluginId\", version.ref = \"$pluginVersionName\" }"
        pluginId = pluginId.substring(startDotIndex + 1)

        // take version value
        val versionValue = versionRegex.find(line)?.groups?.get(1)?.value
        val versionLine = versionValue?.let { "$pluginVersionName = \"$it\"" }

        if (versionLine != null) {
            writePluginToToml(tomlEntry, versionLine, pluginId)
            removeLine(filePluginProject, line)
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