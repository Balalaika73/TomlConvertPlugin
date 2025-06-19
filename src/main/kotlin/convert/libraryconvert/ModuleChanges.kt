package convert.libraryconvert

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
/*
import entries.DependencyEntry
import entries.ModuleEntry

class ModuleChanges(
    private val fileToml: VirtualFile,
    private val fileModuleProject: VirtualFile,
    private val project: Project
) {
    fun convertModuleToToml(line: String, lineNumber: Int): Pair<DependencyEntry, ModuleEntry>? {
        val moduleContent = line.substringAfter('(').substringBeforeLast(')')
        val (group, name, version) = moduleContent.split(':').takeIf { it.size ==3 }
            ?: throw IllegalArgumentException("Invalid module format in line $lineNumber: '$line'")

        val cleanGroup = group.removeSurrounding("\"")
        val versionRef = cleanGroup.split('.').last() + "Version"

        val dependencyEntry = DependencyEntry(
            moduleName = name,
            group = cleanGroup,
            versionRef = versionRef
        )

        val versionEntry = ModuleEntry(
            versionRef = versionRef,
            version = version.removeSuffix("\"") // Handle trailing quote if present
        )

        return dependencyEntry to versionEntry
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
}*/
