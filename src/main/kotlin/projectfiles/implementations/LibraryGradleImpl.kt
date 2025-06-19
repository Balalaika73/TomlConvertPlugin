package projectfiles.implementations

import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import entries.LibraryEntry
import entries.VersionEntry
import projectfiles.interfaces.LibraryGradle

class LibraryGradleImpl(
    private val fileToml: Document,
    private val fileModuleGradle: Document,
    private val project: Project
): LibraryGradle {
    override fun getDependencyType(line: String): String {
        val regex = Regex("""^\s*(implementation|testImplementation|androidTestImplementation|runtimeOnly|debugImplementation|ksp)\b""")
        return regex.find(line)?.groups?.get(1)?.value ?: "implementation"
    }

    override fun getLibraryString(line: String): String {
        val regex = Regex(
            """\b(?:implementation|testImplementation|androidTestImplementation|runtimeOnly|debugImplementation|ksp|api|compileOnly|annotationProcessor)\s*\(\s*["']([^"']+)["']\s*\)"""
        )
        return regex.find(line)?.groups?.get(1)?.value ?: "Unknown"
    }

    override fun getLibraryGroup(line: String): String {
        val libraryParts = line.split(':')
        val libraryGroup = libraryParts[0]
        return libraryGroup
    }

    override fun getLibraryName(line: String): String {
        val libraryParts = line.split(':')
        val libraryName = libraryParts[1]
        return libraryName
    }

    override fun getLibraryVersion(line: String): String {
        val libraryParts = line.split(':')
        val versionNumb = libraryParts.last()
        return versionNumb
    }

    override fun createLibraryEntry(line: String): LibraryEntry {
        val dependencyType = getDependencyType(line)
        val libraryString = getLibraryString(line)
        val libraryGroup = getLibraryGroup(libraryString)
        val libraryName = getLibraryName(libraryString)
        val versionNumb = getLibraryVersion(libraryString)

        val firstPartOfVersionName = versionNameToCamelCase(libraryName)
        val versionName = "${firstPartOfVersionName}Version"

        val libraryVersion = VersionEntry(versionName, versionNumb)
        val libraryTomlName = createLibraryName(libraryGroup, libraryName)
        val libraryEntry = LibraryEntry(
            libraryTomlName,
            libraryGroup,
            libraryName,
            libraryVersion,
            dependencyType)
        return libraryEntry
    }

    private fun versionNameToCamelCase(input: String): String {
        return input.split('-')
            .mapIndexed { index, part ->
                if (index == 0) part
                else part.replaceFirstChar { it.uppercaseChar() }
            }
            .joinToString("")
    }

    private fun createLibraryName(libraryGroup: String, libraryName: String): String {
        val groupParts = libraryGroup.split('.')
        val nameParts = libraryName.split('-')
        return if (groupParts.size == 1)
            groupParts[0]
        else {
            "${groupParts[0]}-${groupParts.last()}-${nameParts.last()}"
        }
    }

    override fun writeLibraryToToml(libraryEntry: LibraryEntry) {
        val libraryTomlEntry = libraryEntry.convertToToml()
        val libraryTomlVersion = libraryEntry.libraryVersion.convertToToml()

        //insert plugin version in versions section
        val librariesIndex = fileToml.text.indexOf("[libraries]")
        if (librariesIndex != -1 && !fileToml.text.contains(libraryTomlVersion)) {
            val linesBefore = fileToml.text.substring(0, librariesIndex).trimEnd()
            val insertPosition = linesBefore.length
            fileToml.insertString(insertPosition, "\n$libraryTomlVersion\n")
        }

        //insert plugin entry in libraries section
        val pluginsIndex = fileToml.text.indexOf("[plugins]")
        if (pluginsIndex != -1 && !fileToml.text.contains(libraryTomlEntry)) {
            val linesBefore = fileToml.text.substring(0, pluginsIndex).trimEnd()
            val insertPosition = linesBefore.length
            fileToml.insertString(insertPosition, "\n$libraryTomlEntry\n")
        }
    }

    override fun writeLibraryToModuleGradle(libraryEntry: LibraryEntry, lineIndex: Int) {
        val implLine = libraryEntry.convertToModuleImplLine()
        val newLine = "$implLine"

        try {
            val startOffset = fileModuleGradle.getLineStartOffset(lineIndex)
            val endOffset = fileModuleGradle.getLineEndOffset(lineIndex)

            fileModuleGradle.replaceString(startOffset, endOffset, newLine)

            Messages.showInfoMessage(
                project,
                "Заменено на строке #${lineIndex + 1}:\n$newLine",
                "Успех"
            )
        } catch (e: Exception) {
            Messages.showErrorDialog(
                project,
                "Ошибка при замене строки: ${e.message}",
                "Ошибка"
            )
        }
    }

}