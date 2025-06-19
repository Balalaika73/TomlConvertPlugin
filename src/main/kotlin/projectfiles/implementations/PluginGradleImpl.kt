package projectfiles.implementations

import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import entries.PluginEntry
import entries.VersionEntry
import projectfiles.interfaces.PluginGradle
import com.intellij.openapi.ui.Messages

class PluginGradleImpl(
    private val fileToml: Document,
    private val fileModuleGradle: Document,
    private val fileAppGradle: Document,
    private val project: Project
): PluginGradle {
    override fun getPluginId(line: String): String {
        val regex = Regex("""id\s*\(\s*["']([^"']+)["']\s*\)""")
        var pluginName = regex.find(line)?.groups?.get(1)?.value ?: "Unknown"
        return pluginName
    }

    override fun getPluginVersion(line: String): String {
        val regex = Regex("""version\s*['"]([^'"]+)['"]""")
        val version = regex.find(line)?.groupValues?.get(1)
            ?: throw IllegalArgumentException("Не удалось найти версию плагина в строке: $line")
        return version
    }

    override fun createPluginEntry(line: String): PluginEntry {
        return try {
            val id = getPluginId(line)
            val versionNumb = getPluginVersion(line)
            val name = createPluginName(id)
            val versionName = "${id.substring(id.lastIndexOf('.') + 1)}Version"
            val pluginVersion = VersionEntry(versionName, versionNumb)
            PluginEntry(name, id, pluginVersion)
        } catch (e: Exception) {
            throw IllegalArgumentException("Ошибка создания PluginEntry из строки '$line': ${e.message}", e)
        }
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

    override fun writePluginToProjectGradle(pluginEntry: PluginEntry, lineIndex: Int) {
        val aliasLine = pluginEntry.convertToAliasLine()
        try {
            val startOffset = fileAppGradle.getLineStartOffset(lineIndex)
            val endOffset = fileAppGradle.getLineEndOffset(lineIndex)

            fileAppGradle.replaceString(startOffset, endOffset, aliasLine)
        } catch (e: Exception) {
            Messages.showErrorDialog(
                project,
                "Ошибка при замене строки: ${e.message}",
                "Ошибка"
            )
        }
    }

    override fun writePluginToModuleGradle(pluginEntry: PluginEntry) {
        val text = fileModuleGradle.text
        val aliasLine = pluginEntry.convertToModuleAliasLine()
        val pluginId = pluginEntry.pluginId

        // Находим блок plugins
        val pluginsStart = text.indexOf("plugins")
        if (pluginsStart == -1) return

        val blockStart = text.indexOf("{", pluginsStart)
        if (blockStart == -1) return

        val blockEnd = text.indexOf("}", blockStart)
        if (blockEnd == -1) return

        val pluginPattern = Regex("""id\s*\(["']${Regex.escape(pluginId)}["']\)[^\n]*""")
        val matchResult = pluginPattern.find(text, blockStart)

        if (matchResult != null) {
            val start = matchResult.range.first
            val end = matchResult.range.last + 1
            fileModuleGradle.replaceString(start, end, aliasLine)
        } else {
            if (!text.contains(aliasLine)) {
                val withIndent = "\t$aliasLine\n"
                fileModuleGradle.insertString(blockEnd, withIndent)
            }
        }
    }
}