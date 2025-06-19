package di

import com.intellij.openapi.project.Project
import com.intellij.openapi.editor.Document
import org.koin.dsl.module
import projectfiles.implementations.AppGradleImpl
import projectfiles.implementations.GradleFilesImpl
import projectfiles.interfaces.AppGradle
import projectfiles.interfaces.GradleFiles

val appModule = module {
    factory<GradleFiles> { (project: Project) -> GradleFilesImpl(project) }

    factory<AppGradle> { (fileToml: Document, fileModuleGradle: Document, fileAppGradle: Document) ->
        AppGradleImpl(fileToml, fileModuleGradle, fileAppGradle)
    }
}
