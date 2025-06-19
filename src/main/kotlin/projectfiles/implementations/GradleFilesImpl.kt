package projectfiles.implementations

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findDocument
import com.intellij.psi.PsiManager
import projectfiles.interfaces.GradleFiles
import java.io.File

class GradleFilesImpl(
    private val project: Project
): GradleFiles {
    override fun findFileInProject(fileName: String): Document {
        val basePath = project.basePath
        val targetFile = File(basePath, fileName)
        return VfsUtil.findFileByIoFile(targetFile, true)!!.findDocument()!!
    }

    override fun readFileContent(file: Document): String {
        return file.text
    }

    override fun removeLineFromFile(file: VirtualFile, lineToRemove: String) {
        val document = FileDocumentManager.getInstance().getDocument(file)!!
        WriteCommandAction.runWriteCommandAction(project) {
            val updatedText = document.text
                .lines()
                .filter { it != lineToRemove }
                .joinToString("\n")

            document.setText(updatedText)
        }
    }
}