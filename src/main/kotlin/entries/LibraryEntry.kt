package entries

data class LibraryEntry (
    val dependencyName: String,
    val libraryGroup: String,
    val libraryName: String,
    val libraryVersion: VersionEntry,
    val dependencyType: String
) {
    fun convertToToml(): String =
        "$dependencyName = { group = \"$libraryGroup\", name = \"$libraryName\", version.ref = \"${libraryVersion.versionName}\" }"

    fun convertToModuleImplLine(): String {
        val moduleLibraryName = dependencyName.replace('-', '.')
        return "\t$dependencyType(libs.$moduleLibraryName)"
    }
}