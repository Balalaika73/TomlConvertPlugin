package di

import com.intellij.openapi.project.Project
import com.intellij.openapi.editor.Document
import org.koin.dsl.module
import projectfiles.implementations.PluginGradleImpl
import projectfiles.implementations.GradleFilesImpl
import projectfiles.interfaces.PluginGradle
import projectfiles.interfaces.GradleFiles

val appModule = module {
    factory<GradleFiles> { (project: Project) -> GradleFilesImpl(project) }

    factory<PluginGradle> { (fileToml: Document, fileModuleGradle: Document, fileAppGradle: Document) ->
        PluginGradleImpl(fileToml, fileModuleGradle, fileAppGradle)
    }
}
