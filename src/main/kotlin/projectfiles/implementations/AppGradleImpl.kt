package projectfiles.implementations

import com.intellij.openapi.editor.Document
import entries.PluginEntry
import entries.VersionEntry
import projectfiles.interfaces.AppGradle

class AppGradleImpl(
    private val fileToml: Document,
    private val fileModuleGradle: Document,
    private val fileAppGradle: Document
): AppGradle {
    override fun getPluginId(line: String): String {
        val regex = Regex("""id\s*\(\s*["']([^"']+)["']\s*\)""")
        var pluginName = regex.find(line)?.groups?.get(1)?.value ?: "Unknown"
        return pluginName
    }

    override fun getPluginVersion(line: String): String {
        val regex = Regex("""version\s*['"]([^'"]+)['"]""")
        val version = regex.find(line)?.groupValues?.get(1)!!
        return version
    }

    override fun createPluginEntry(line: String): PluginEntry {
        val id = getPluginId(line)
        val versionNumb = getPluginVersion(line)
        val name = createPluginName(id)
        val versionName = "${id.substring(id.lastIndexOf('.') + 1)}Version"
        val pluginVersion = VersionEntry(versionName, versionNumb)
        val pluginEntry = PluginEntry(name, id, pluginVersion)

        return pluginEntry
    }

    private fun createPluginName(pluginId: String): String {
        val nameParts = pluginId.split('.')
        return if (nameParts.size < 5) //return last 2 words
            "${nameParts[nameParts.size - 2]}-${nameParts.last()}"
        else //return last 3 words
            "${nameParts[nameParts.size - 3]}-${nameParts[nameParts.size - 2]}-${nameParts.last()}"
    }

    override fun writePluginToToml(pluginEntry: PluginEntry) {
        val pluginTomlEntry = pluginEntry.convertToToml()
        val pluginTomlVersion = pluginEntry.pluginVersion.convertToToml()

        //insert plugin version in versions section
        val librariesIndex = fileToml.text.indexOf("[libraries]")
        if (librariesIndex != -1 && !fileToml.text.contains(pluginTomlVersion)) {
            val linesBefore = fileToml.text.substring(0, librariesIndex).trimEnd()
            val insertPosition = linesBefore.length
            fileToml.insertString(insertPosition, "\n$pluginTomlVersion\n")
        }

        //insert plugin entry in plugins section
        if (!fileToml.text.contains(pluginTomlEntry)) {
            val needsNewline = fileToml.text.lastOrNull()?.let { it != '\n' } ?: false
            val insertText = if (needsNewline) "\n$pluginTomlEntry" else pluginTomlEntry
            fileToml.insertString(fileToml.textLength, insertText)
        }
    }

    override fun writePluginToAppGradle(pluginEntry: PluginEntry, lineIndex: Int) {
        val aliasLine = pluginEntry.convertToAliasLine()
        val lines = fileAppGradle.text.lines()

        if (lineIndex in 1..lines.size) {
            val targetLine = lines[lineIndex - 1]

            val startOffset = fileAppGradle.getLineStartOffset(lineIndex - 1)
            val endOffset = fileAppGradle.getLineEndOffset(lineIndex - 1)

            if (targetLine != aliasLine && !fileAppGradle.text.contains(aliasLine)) {
                fileAppGradle.replaceString(startOffset, endOffset, aliasLine)
            } else if (targetLine == aliasLine) {
                fileAppGradle.replaceString(startOffset, endOffset, "")
            }
        }
    }

    override fun writePluginToModuleGradle(pluginEntry: PluginEntry, lineIndex: Int) {
        val text = fileModuleGradle.text
        val aliasLine = pluginEntry.convertToModuleAliasLine()

        val pluginsStart = text.indexOf("plugins")
        if (pluginsStart == -1) return

        val blockStart = text.indexOf("{", pluginsStart)
        if (blockStart == -1) return

        val blockEnd = text.indexOf("}", blockStart)
        if (blockEnd == -1) return

        if (text.contains(aliasLine)) return

        val insertPos = blockEnd
        val withIndent = "$aliasLine\n" // 4 пробела — типичный отступ

        fileModuleGradle.insertString(insertPos, withIndent)
    }
}