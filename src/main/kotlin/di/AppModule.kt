package di

import com.intellij.openapi.project.Project
import com.intellij.openapi.editor.Document
import org.koin.dsl.module
import projectfiles.implementations.PluginGradleImpl
import projectfiles.implementations.GradleFilesImpl
import projectfiles.implementations.LibraryGradleImpl
import projectfiles.interfaces.PluginGradle
import projectfiles.interfaces.GradleFiles
import projectfiles.interfaces.LibraryGradle

val appModule = module {
    factory<GradleFiles> { (project: Project) -> GradleFilesImpl(project) }

    factory<PluginGradle> { (fileToml: Document, fileModuleGradle: Document, fileAppGradle: Document, project: Project) ->
        PluginGradleImpl(fileToml, fileModuleGradle, fileAppGradle, project)
    }

    factory<LibraryGradle> { (fileToml: Document, fileModuleGradle: Document, project: Project) ->
        LibraryGradleImpl(fileToml, fileModuleGradle, project)
    }
}
