package projectfiles.interfaces

import entries.PluginEntry

interface AppGradle {
    fun getPluginId(line: String): String
    fun getPluginVersion(line: String): String
    fun createPluginEntry(line: String): PluginEntry
    fun writePluginToToml(pluginEntry: PluginEntry)
    fun writePluginToAppGradle(pluginEntry: PluginEntry, lineIndex: Int)
    fun writePluginToModuleGradle(pluginEntry: PluginEntry, lineIndex: Int)
}