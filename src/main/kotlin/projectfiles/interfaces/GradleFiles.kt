package projectfiles.interfaces

import com.intellij.openapi.editor.Document
import com.intellij.openapi.vfs.VirtualFile

interface GradleFiles {
    fun findFileInProject(fileName: String): Document
    fun readFileContent(file: Document): String
    fun removeLineFromFile(file: VirtualFile, lineToRemove: String)
}