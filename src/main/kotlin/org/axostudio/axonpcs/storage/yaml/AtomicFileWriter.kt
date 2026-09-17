package org.axostudio.axonpcs.storage.yaml

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object AtomicFileWriter {
    fun writeAtomically(targetFile: File, writeAction: (File) -> Unit) {
        val parentDir = targetFile.parentFile
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs()
        }

        val tempFile = File(parentDir, "${targetFile.name}.tmp")
        try {
            writeAction(tempFile)
            Files.move(
                tempFile.toPath(),
                targetFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (e: Exception) {
            // Fallback non-atomic rename if ATOMIC_MOVE fails on some filesystem
            try {
                Files.move(
                    tempFile.toPath(),
                    targetFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            } catch (t: Throwable) {
                tempFile.delete()
                throw IOException("Could not write file atomically to $targetFile", t)
            }
        }
    }
}
