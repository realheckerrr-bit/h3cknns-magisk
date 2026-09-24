package com.topjohnwu.magisk.core.download

import android.content.Context
import android.net.Uri
import com.topjohnwu.magisk.StubApk
import com.topjohnwu.magisk.core.R
import com.topjohnwu.magisk.core.isRunningAsStub
import com.topjohnwu.magisk.core.ktx.cachedFile
import com.topjohnwu.magisk.core.ktx.copyAll
import com.topjohnwu.magisk.core.ktx.copyAndClose
import com.topjohnwu.magisk.core.ktx.withInOut
import com.topjohnwu.magisk.core.ktx.writeTo
import com.topjohnwu.magisk.core.tasks.AppMigration
import com.topjohnwu.magisk.core.utils.MediaStoreUtils.outputStream
import com.topjohnwu.magisk.utils.APKInstall
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale

class DownloadProcessor(notifier: DownloadNotifier) : DownloadNotifier by notifier {

    suspend fun handle(stream: InputStream, subject: Subject) {
        when (subject) {
            is Subject.App -> handleApp(stream, subject)
            is Subject.Module -> handleModule(stream, subject.file)
            else -> stream.copyAndClose(subject.file.outputStream())
        }
    }

    suspend fun handleApp(stream: InputStream, subject: Subject.App) {
        if (isRunningAsStub) {
            val updateApk = StubApk.update(context)
            try {
                // Download full APK to stub update path
                subject.file.outputStream().use { external ->
                    updateApk.outputStream().use { update ->
                        stream.use { input ->
                            input.copyAll(TeeOutputStream(external, update))
                        }
                    }
                }

                // Also upgrade stub
                notifyUpdate(subject.notifyId) {
                    it.setProgress(0, 0, true)
                        .setContentTitle(context.getString(R.string.hide_app_title))
                        .setContentText("")
                }

                // Extract stub
                val apk = context.cachedFile("stub.apk")
                ZipFile.Builder().setFile(updateApk).get().use { zf ->
                    apk.delete()
                    zf.getInputStream(zf.getEntry("assets/stub.apk")).writeTo(apk)
                }

                // Patch and install
                subject.intent = AppMigration.upgradeStub(context, apk)
                apk.delete()
            } catch (e: Exception) {
                // If any error occurred, do not let stub load the new APK
                updateApk.delete()
                throw e
            }
        } else {
            val session = APKInstall.startSession(context)
            var installerStream: OutputStream? = null
            try {
                val installer = session.openStream(context)
                installerStream = installer
                subject.file.outputStream().use { external ->
                    stream.use { input ->
                        input.copyAll(TeeOutputStream(external, installer))
                    }
                }
                installer.close()
                installerStream = null
                subject.intent = session.waitIntent()
                if (!session.isComplete) {
                    session.abandon(context)
                    throw IOException(context.getString(R.string.app_update_timeout))
                }
                session.failureMessage()?.takeIf { it.isNotBlank() }?.let {
                    throw IOException(context.appInstallFailure(it))
                }
            } catch (e: Exception) {
                if (!session.isComplete) session.abandon(context)
                throw e
            } finally {
                installerStream?.let { stream ->
                    try {
                        stream.close()
                    } catch (e: IOException) {
                        if (!session.isComplete) session.abandon(context)
                        throw e
                    }
                }
            }
        }
    }

    private fun Context.appInstallFailure(message: String): String {
        val normalized = message.lowercase(Locale.ROOT)
        return when {
            "signature" in normalized || "update_incompatible" in normalized ->
                getString(R.string.app_update_signature_mismatch)
            "version_downgrade" in normalized || "downgrade" in normalized ->
                getString(R.string.app_update_version_downgrade)
            "storage" in normalized || "no_space" in normalized ->
                getString(R.string.app_update_storage)
            "blocked" in normalized || "user_restricted" in normalized ->
                getString(R.string.app_update_blocked)
            else -> message
        }
    }

    suspend fun handleModule(src: InputStream, file: Uri) {
        val tmp = context.cachedFile("module.zip")
        try {
            // First download the entire zip into cache so we can process it
            src.writeTo(tmp)

            val input = ZipFile.Builder().setFile(tmp).get()
            val output = ZipArchiveOutputStream(file.outputStream())
            withInOut(input, output) { zin, zout ->
                zout.putArchiveEntry(ZipArchiveEntry("META-INF/"))
                zout.closeArchiveEntry()
                zout.putArchiveEntry(ZipArchiveEntry("META-INF/com/"))
                zout.closeArchiveEntry()
                zout.putArchiveEntry(ZipArchiveEntry("META-INF/com/google/"))
                zout.closeArchiveEntry()
                zout.putArchiveEntry(ZipArchiveEntry("META-INF/com/google/android/"))
                zout.closeArchiveEntry()

                zout.putArchiveEntry(ZipArchiveEntry("META-INF/com/google/android/update-binary"))
                context.assets.open("module_installer.sh").use { it.copyAll(zout) }
                zout.closeArchiveEntry()

                zout.putArchiveEntry(ZipArchiveEntry("META-INF/com/google/android/updater-script"))
                zout.write("#MAGISK\n".toByteArray())
                zout.closeArchiveEntry()

                // Then simply copy all entries to output
                zin.copyRawEntries(zout) { entry -> !entry.name.startsWith("META-INF") }
            }
        } finally {
            tmp.delete()
        }
    }

    private class TeeOutputStream(
        private val o1: OutputStream,
        private val o2: OutputStream
    ) : OutputStream() {
        override fun write(b: Int) {
            o1.write(b)
            o2.write(b)
        }
        override fun write(b: ByteArray?, off: Int, len: Int) {
            o1.write(b, off, len)
            o2.write(b, off, len)
        }
        override fun close() {
            o1.close()
            o2.close()
        }
    }
}
