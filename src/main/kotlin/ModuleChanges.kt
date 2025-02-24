import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class ModuleChanges(
    private val fileToml: VirtualFile,
    private val fileModuleProject: VirtualFile,
    private val project: Project
) {
    fun convertModuleToToml(line: String, lineNumber: Int) {
        val implParts = line.split(':')
        val groupImpl = implParts[0].split('"')[1]
        val nameImpl = implParts[1]
        val versImpl = implParts[2].dropLast(2)

        var versName = groupImpl.split('.').last() + "Version"
        val resString = "$nameImpl = {group = \"$groupImpl\", name = \"$nameImpl\", version.ref = \"$versName\" }"

        versName = "$versName = \"$versImpl\""

        val implId = nameImpl.replace('-', '.')

        writeModuleToToml(lineNumber, resString, versName, implId)
    }

    private fun writeModuleToToml(lineNumber : Int, tomlEntry: String, versionLine: String, pluginId: String) {
        val tomlDocument = fileToml?.let { FileDocumentManager.getInstance().getDocument(it) }
        val moduleDocument = fileModuleProject?.let { FileDocumentManager.getInstance().getDocument(it) }

        WriteCommandAction.runWriteCommandAction(project) {
            tomlDocument?.let { doc ->
                val versionLineToAdd = "\n$versionLine"
                val tomlEntryToAdd = "\n$tomlEntry"

                val versIndex = doc.text.indexOf("[libraries]")
                if (versIndex != -1 && !doc.text.contains(versionLine)) {
                    doc.insertString(versIndex-1, versionLineToAdd)
                }

                val librariesIndex = doc.text.indexOf("[plugins]")
                if (librariesIndex != -1 && !doc.text.contains(tomlEntry)) {
                    doc.insertString(librariesIndex-1, tomlEntryToAdd)
                }
            }

            moduleDocument?.let { doc ->
                val lines = doc.text.lines()

                if (lineNumber in 1..lines.size) {
                    val startOffset = doc.getLineStartOffset(lineNumber - 1)
                    val endOffset = doc.getLineEndOffset(lineNumber - 1)

                    val originalLine = lines[lineNumber - 1]
                    val aliasLine = if ("kapt(" in originalLine) {
                        "\tkapt(libs.$pluginId)"
                    } else {
                        "\timplementation(libs.$pluginId)"
                    }

                    if (!doc.text.contains(aliasLine))
                        doc.replaceString(startOffset, endOffset, aliasLine)
                    else
                        doc.replaceString(startOffset, endOffset, "")
                }
            }
        }
    }
}