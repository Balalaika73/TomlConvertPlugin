package entries

data class VersionEntry (
    val versionName: String,
    val versionNumber: String
) {
    fun convertToToml() : String =
        "$versionName = \"$versionNumber\""
}