package entries

data class LibraryEntry (
    val dependencyName: String,
    val libraryGroup: String,
    val libraryName: String,
    val libraryVersion: VersionEntry
) {
    fun convertToToml(): String =
        "$dependencyName = { group = \"$libraryGroup\", name = \"$libraryName\", version.ref = \"${libraryVersion.versionName}\" }"
}