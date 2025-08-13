package ui

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.*

class ListDialog(
    private val libraries: List<Pair<Int, String>>,
    private val plugins: List<Pair<Int, String>>
): DialogWrapper(null) {
    val pluginStates = mutableMapOf<String, Boolean>().apply {
        plugins.forEach { put(it.second, true) }
    }
    val libraryStates = mutableMapOf<String, Boolean>().apply {
        libraries.forEach { put(it.second, true) }
    }

    private val pluginCheckboxes = mutableMapOf<String, Cell<JBCheckBox>>()
    private val libraryCheckboxes = mutableMapOf<String, Cell<JBCheckBox>>()

    lateinit var pluginsGroupCheckbox: Cell<JBCheckBox>
    lateinit var librariesGroupCheckbox: Cell<JBCheckBox>

    fun getSelectedPlugins(): List<Pair<Int, String>> {
        return plugins.filter { pluginStates[it.second] == true }
    }

    fun getSelectedLibraries(): List<Pair<Int, String>> {
        return libraries.filter { libraryStates[it.second] == true }
    }

    init {
        init()
        title = "What to transform"
    }

    override fun createCenterPanel() = panel {
        group("Plugins") {
            row {
                pluginsGroupCheckbox = checkBox("Plugins")
                    .applyToComponent { isSelected = pluginStates.all { it.value } }
                    .onChanged { cb ->
                        val newState = cb.isSelected
                        // Обновляем состояния в модели
                        pluginStates.keys.forEach { pluginStates[it] = newState }
                        // Обновляем все дочерние чекбоксы в UI
                        pluginCheckboxes.values.forEach { it.component.isSelected = newState }
                    }
            }
            indent {
                plugins.forEach { plugin ->
                    row {
                        pluginCheckboxes[plugin.second] = checkBox(plugin.second)
                            .applyToComponent { isSelected = pluginStates[plugin.second] ?: false }
                            .onChanged { cb ->
                                pluginStates[plugin.second] = cb.isSelected
                                // Обновляем состояние главного чекбокса
                                pluginsGroupCheckbox.component.isSelected = pluginStates.all { it.value }
                            }
                    }
                }
            }
        }

        group("Libraries") {
            row {
                librariesGroupCheckbox = checkBox("Libraries")
                    .applyToComponent { isSelected = libraryStates.all { it.value } }
                    .onChanged { cb ->
                        val newState = cb.isSelected
                        // Обновляем состояния в модели
                        libraryStates.keys.forEach { libraryStates[it] = newState }
                        // Обновляем все дочерние чекбоксы в UI
                        libraryCheckboxes.values.forEach { it.component.isSelected = newState }
                    }
            }
            indent {
                libraries.forEach { library ->
                    row {
                        libraryCheckboxes[library.second] = checkBox(library.second)
                            .applyToComponent { isSelected = libraryStates[library.second] ?: false }
                            .onChanged { cb ->
                                libraryStates[library.second] = cb.isSelected
                                librariesGroupCheckbox.component.isSelected = libraryStates.all { it.value }
                            }
                    }
                }
            }
        }
    }
}