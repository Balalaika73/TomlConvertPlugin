import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class ModuleChanges(
    private val fileToml: VirtualFile,
    private val fileModuleProject: VirtualFile,
    private val project: Project
) {
    private val myAction = MyAction()
    fun convertModuleToToml(line: String) {
        val implParts = line.split(':')
        val groupImpl = implParts[0].split('"')[1]
        val nameImpl = implParts[1]
        val versImpl = implParts[2].dropLast(2)

        var versName = groupImpl.split('.').last() + "Version"
        val resString = "$nameImpl = {group = \"$groupImpl\", name = \"$nameImpl\", version.ref = \"$versName\" }"

        versName = "$versName = \"$versImpl\""

        val implId = nameImpl.replace('-', '.')

        writeModuleToToml(resString, versName, implId)
        myAction.removeLine(fileModuleProject, line)
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
}