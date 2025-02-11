import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class PluginChanges(
    private val fileToml: VirtualFile,
    private val fileModuleProject: VirtualFile,
    private val filePluginProject: VirtualFile,
    private val project: Project,
) {
    private val myAction = MyAction()
    fun convertProjectPluginToToml(line: String) {
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
            myAction.removeLine(filePluginProject, line)
        }
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

}