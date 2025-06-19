package entries
data class PluginEntry (
    val pluginName: String,
    val pluginId: String,
    val pluginVersion: VersionEntry
) {
    fun convertToToml(): String =
        "$pluginName = { id = \"$pluginId\", version.ref = \"${pluginVersion.versionName}\" }"

    fun convertToAliasLine(): String {
        val appPluginName = pluginName.replace('-', '.')
        return "\talias(libs.plugins.$appPluginName) apply false"
    }

    fun convertToModuleAliasLine(): String {
        val appPluginName = pluginName.replace('-', '.')
        return "alias(libs.plugins.$appPluginName)"
    }
}

