package io.github.gdict.platform

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import io.github.gdict.core.FileSystemAccess
import io.github.gdict.core.GdictLogger.Companion.get as log

class AndroidFileSystemAccess(private val context: Context) : FileSystemAccess {

    override fun selectDictionaryFiles(): List<String>? {
        return null
    }

    override fun selectDictionaryDirectory(): String? {
        return null
    }

    override fun listFilesInDirectory(dirPath: String): List<String> {
        return emptyList()
    }

    fun resolveFileName(uri: Uri): String {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) return it.getString(nameIndex)
            }
        }
        return uri.lastPathSegment ?: "unknown"
    }

    fun copyUriToInternal(uri: Uri, targetDir: java.io.File): java.io.File? {
        return try {
            val fileName = resolveFileName(uri)
            val targetFile = java.io.File(targetDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (targetFile.exists() && targetFile.length() > 0) targetFile else null
        } catch (e: Exception) {
            log().e("AndroidFS", "copyUriToInternal failed: ${e.message}", e)
            null
        }
    }

    fun listFilesInTreeUri(treeUri: Uri): List<Uri> {
        val results = mutableListOf<Uri>()
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
        val cursor = context.contentResolver.query(childrenUri, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID), null, null, null)
        cursor?.use {
            while (it.moveToNext()) {
                val docId = it.getString(0)
                val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                results.add(childUri)
            }
        }
        return results
    }
}
