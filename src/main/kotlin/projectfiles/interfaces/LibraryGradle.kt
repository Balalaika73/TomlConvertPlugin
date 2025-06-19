package projectfiles.interfaces

import entries.LibraryEntry
import entries.PluginEntry

interface LibraryGradle {
    fun getDependencyType(line: String): String
    fun getLibraryString(line: String): String
    fun getLibraryGroup(line: String): String
    fun getLibraryName(line: String): String
    fun getLibraryVersion(line: String): String
    fun createLibraryEntry(line: String): LibraryEntry
    fun writeLibraryToToml(libraryEntry: LibraryEntry)
    fun writeLibraryToModuleGradle(libraryEntry: LibraryEntry, lineIndex: Int)
}