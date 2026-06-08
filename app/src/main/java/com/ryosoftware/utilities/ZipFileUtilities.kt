package com.ryosoftware.utilities

import android.content.Context
import android.net.Uri
import java.io.*
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.zip.*

object ZipFileUtilities {

    private const val INPUT_STREAM_SIZE = 8192
    private const val DIGEST_FILENAME = ".digest"

    interface OnWorkingWithEntryFile {
        fun onEntryFileLocated(pathname: String, name: String): Boolean
        fun onEntryFileRestored(pathname: String, name: String): Boolean
    }

    fun getDigest(inputStream: InputStream): String? {
        try {
            val digestInputStream = DigestInputStream(
                inputStream,
                MessageDigest.getInstance("MD5")
            )
            val digest = digestInputStream.messageDigest.digest()
            val length = digest.size
            val digestReadable = StringBuilder(length * 2)
            for (i in 0 until length) {
                digestReadable.append(Character.forDigit((digest[i].toInt() and 0xf0) shr 4, 16))
                digestReadable.append(Character.forDigit(digest[i].toInt() and 0x0f, 16))
            }
            return digestReadable.toString()

        } catch (e: Exception) {
            LogUtilities.show(this, e)
        }
        return null
    }
    private fun getDigest(filename: String): String? = try { FileInputStream(filename).use { getDigest(it) } } catch (e: Exception) { null }

    private fun storeDigest(digests: Map<String, String>): File? = try {
        val file = File.createTempFile("zip-", "")
        ObjectOutputStream(FileOutputStream(file)).use { it.writeObject(digests) }
        file
    } catch (e: Exception) { LogUtilities.show(ZipFileUtilities::class.java, e); null }

    @Suppress("UNCHECKED_CAST")
    private fun loadDigestFromStream(inputStream: InputStream): Map<String, String>? = try {
        ObjectInputStream(inputStream).readObject() as Map<String, String>
    } catch (e: Exception) { null }

    private fun mapsEqual(m1: Map<String, String>?, m2: Map<String, String>?): Boolean {
        if (m1 == null) return m2 == null
        if (m2 == null) return false
        return m1.size == m2.size && m1.all { (k, v) -> m2[k] == v }
    }

    fun createZipFile(outputStream: OutputStream): ZipOutputStream? = try {
        ZipOutputStream(BufferedOutputStream(outputStream))
    } catch (e: Exception) { null }

    private fun addEntryFromStream(zipOut: ZipOutputStream, zipPath: String, input: InputStream): Boolean = try {
        zipOut.putNextEntry(ZipEntry(zipPath))
        val buf = ByteArray(INPUT_STREAM_SIZE); var count: Int
        while (input.read(buf).also { count = it } > 0) zipOut.write(buf, 0, count)
        zipOut.closeEntry(); true
    } catch (e: Exception) { false }

    fun createZip(outputStream: OutputStream, files: Map<String, File>, addDigest: Boolean): Boolean {
        val zipOut = createZipFile(outputStream) ?: return false
        val digests = mutableMapOf<String, String>()
        var allOk = true
        for ((path, file) in files) {
            try { FileInputStream(file).use { if (!addEntryFromStream(zipOut, path, it)) { allOk = false } else if (addDigest) digests[path] = getDigest(file.path) ?: "" } }
            catch (e: Exception) { allOk = false }
            if (!allOk) break
        }
        if (allOk && addDigest) {
            val df = storeDigest(digests)
            if (df != null) { try { FileInputStream(df).use { if (!addEntryFromStream(zipOut, DIGEST_FILENAME, it)) allOk = false } } catch (e: Exception) { allOk = false }; df.delete() }
        }
        try { zipOut.close() } catch (e: Exception) { allOk = false }
        return allOk
    }

    private fun checkTraversal(path: String, base: String) {
        if (!File(path).canonicalPath.startsWith(File(base).canonicalPath))
            throw SecurityException("Zip path transversal vulnerability detected...")
    }

    fun unpackZip(context: Context, uri: Uri, destPath: String, listener: OnWorkingWithEntryFile?): Boolean = try {
        context.contentResolver.openInputStream(uri)?.use { unpackZip(it, destPath, listener, false) } ?: false
    } catch (e: Exception) { LogUtilities.show(ZipFileUtilities::class.java, e); false }

    fun unpackZip(input: InputStream, destPath: String, listener: OnWorkingWithEntryFile?, requireDigest: Boolean): Boolean {
        val extracted = mutableListOf<String>(); var allOk = true
        try {
            ZipInputStream(BufferedInputStream(input)).use { zipIn ->
                var oldDigests: Map<String, String>? = null
                val newDigests = mutableMapOf<String, String>()
                var entry: ZipEntry?
                while (zipIn.nextEntry.also { entry = it } != null) {
                    val e = entry!!
                    if (!e.isDirectory) {
                        if (requireDigest && e.name == DIGEST_FILENAME) { oldDigests = loadDigestFromStream(zipIn) }
                        else {
                            val outPath = destPath + File.separator + e.name
                            checkTraversal(outPath, destPath)
                            val extract = listener == null || listener.onEntryFileLocated(outPath, e.name)
                            if (extract) {
                                if (extractEntry(zipIn, outPath)) {
                                    extracted.add(outPath)
                                    if (requireDigest) newDigests[e.name] = getDigest(outPath) ?: ""
                                    if (listener != null) allOk = listener.onEntryFileRestored(outPath, e.name)
                                } else allOk = false
                            }
                        }
                    }
                    zipIn.closeEntry();
                    if (!allOk) break
                }
                if (allOk && requireDigest && !mapsEqual(oldDigests, newDigests)) allOk = false
            }
        } catch (e: Exception) { LogUtilities.show(ZipFileUtilities::class.java, e); allOk = false }
        if (!allOk) extracted.forEach { File(it).delete() }
        return allOk
    }

    private fun extractEntry(zipIn: ZipInputStream, outPath: String): Boolean = try {
        val file = File(outPath); file.parentFile?.mkdirs()
        FileOutputStream(file).use { fos ->
            val buf = ByteArray(INPUT_STREAM_SIZE); var count: Int
            while (zipIn.read(buf).also { count = it } != -1) fos.write(buf, 0, count)
        }; true
    } catch (e: Exception) { LogUtilities.show(ZipFileUtilities::class.java, e); false }

    fun unpackZip(pathname: String, destPath: String, listener: OnWorkingWithEntryFile?): Boolean = try {
        FileInputStream(pathname).use { unpackZip(it, destPath, listener, false) }
    } catch (e: Exception) { false }

    fun isValid(file: File): Boolean = try { ZipFile(file).use { true } } catch (e: Exception) { false }
}
