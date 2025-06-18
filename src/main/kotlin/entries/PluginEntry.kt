package entries

data class PluginEntry (
    val pluginName: String,
    val pluginId: String,
    val pluginVersion: VersionEntry
) {
    fun convertToToml(): String =
        "$pluginName = { id = \"$pluginId\", version.ref = \"${pluginVersion.versionName}\" }"
}