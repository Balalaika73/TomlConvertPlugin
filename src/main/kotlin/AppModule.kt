import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.koin.dsl.module

val appModule = module {
    factory { (fileToml: VirtualFile, fileModuleProject: VirtualFile, project: Project) ->
        ModuleChanges(fileToml, fileModuleProject, project)
    }

    factory { (fileToml: VirtualFile, fileModuleProject: VirtualFile, filePluginProject: VirtualFile, project: Project) ->
        PluginChanges(fileToml, fileModuleProject, filePluginProject, project)
    }
}
