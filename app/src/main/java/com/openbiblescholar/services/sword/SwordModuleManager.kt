package com.openbiblescholar.services.sword

import android.content.Context
import com.openbiblescholar.data.db.dao.SwordModuleDao
import com.openbiblescholar.data.db.dao.VerseDao
import com.openbiblescholar.data.db.entity.SwordModuleEntity
import com.openbiblescholar.data.db.entity.VerseEntity
import com.openbiblescholar.data.model.ModuleType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

data class DownloadProgress(
    val moduleName: String = "",
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = 0,
    val isComplete: Boolean = false,
    val error: String? = null
) {
    val percent: Int get() = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt() else 0
}

@Singleton
class SwordModuleManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val moduleDao: SwordModuleDao,
    private val verseDao: VerseDao
) {
    private val _downloadProgress = MutableStateFlow(DownloadProgress())
    val downloadProgress: StateFlow<DownloadProgress> = _downloadProgress.asStateFlow()

    private val modulesDir: File get() = File(context.filesDir, "sword_modules").also { it.mkdirs() }
    private val client = OkHttpClient()

    // ── Catalog ───────────────────────────────────────────────────────────────

    val availableModules: List<SwordModuleEntity> = buildList {
        // Public Domain Bible Translations
        add(SwordModuleEntity("KJV","King James Version","BIBLE","en","1.5","Public Domain",false,8.5f,8.5f,true,"https://crosswire.org/ftpmirror/pub/sword/packages/rawzip/KJV.zip"))
        add(SwordModuleEntity("ASV","American Standard Version (1901)","BIBLE","en","1.2","Public Domain",false,7.8f,7.8f,true,"https://crosswire.org/ftpmirror/pub/sword/packages/rawzip/ASV.zip"))
        add(SwordModuleEntity("WEB","World English Bible","BIBLE","en","2.0","Public Domain",false,8.2f,8.2f,true,"https://crosswire.org/ftpmirror/pub/sword/packages/rawzip/WEB.zip"))
        add(SwordModuleEntity("YLT","Young's Literal Translation","BIBLE","en","1.3","Public Domain",false,7.5f,7.5f,true,"https://crosswire.org/ftpmirror/pub/sword/packages/rawzip/YLT.zip"))
        add(SwordModuleEntity("Darby","Darby Bible","BIBLE","en","1.1","Public Domain",false,7.4f,7.4f,true,"https://crosswire.org/ftpmirror/pub/sword/packages/rawzip/Darby.zip"))
        add(SwordModuleEntity("Webster","Webster's Bible (1833)","BIBLE","en","1.0","Public Domain",false,7.5f,7.5f,true,"https://crosswire.org/ftpmirror/pub/sword/packages/rawzip/Webster.zip"))
        add(SwordModuleEntity("KJVA","KJV with Apocrypha","BIBLE","en","1.5","Public Domain",false,9.0f,9.0f,true,"https://crosswire.org/ftpmirror/pub/sword/packages/rawzip/KJVA.zip"))
        add(SwordModuleEntity("LXX","Septuagint (Greek OT)","BIBLE","grc","1.5","Public Domain",false,10.2f,10.2f,true,"https://crosswire.org/ftpmirror/pub/sword/packages/rawzip/LXX.zip"))
        add(SwordModuleEntity("SBLGNT","SBL Greek New Testament","BIBLE","grc","1.0","Free for non-commercial",false,5.2f,5.2f,false,"https://crosswire.org/ftpmirror/pub/sword/packages/rawzip/SBLGNT.zip"))
        add(SwordModuleEntity("BHS","BHS Hebrew OT (with Strong's)","BIBLE","hbo","1.1","Public Domain",false,12.0f,12.0f,true,"https://crosswire.org/ftpmirror/pub/sword/packages/rawzip/BHS.zip"))
        add(SwordModuleEntity("StrongsGreek","Strong's Greek Lexicon","DICTIONARY","grc","1.2","Public Domain",false,3.5f,3.5f,true,"https://crosswire.org/ftpmirror/pub/sword/packages/rawzip/StrongsGreek.zip"))
        add(SwordModuleEntity("StrongsHebrew","Strong's Hebrew Lexicon","DICTIONARY","hbo","1.2","Public Domain",false,4.0f,4.0f,true,"https://crosswire.org/ftpmirror/pub/sword/packages/rawzip/StrongsHebrew.zip"))
        add(SwordModuleEntity("MHC","Matthew Henry's Commentary","COMMENTARY","en","1.5","Public Domain",false,42.0f,42.0f,true,"https://crosswire.org/ftpmirror/pub/sword/packages/rawzip/MHC.zip"))
        add(SwordModuleEntity("MHCc","Matthew Henry Concise Commentary","COMMENTARY","en","1.1","Public Domain",false,18.0f,18.0f,true,"https://crosswire.org/ftpmirror/pub/sword/packages/rawzip/MHCc.zip"))
        add(SwordModuleEntity("Jamieson","Jamieson-Fausset-Brown Commentary","COMMENTARY","en","1.3","Public Domain",false,38.0f,38.0f,true,"https://crosswire.org/ftpmirror/pub/sword/packages/rawzip/JFB.zip"))
        add(SwordModuleEntity("Clarke","Adam Clarke's Commentary","COMMENTARY","en","1.4","Public Domain",false,52.0f,52.0f,true,"https://crosswire.org/ftpmirror/pub/sword/packages/rawzip/Clarke.zip"))
        add(SwordModuleEntity("Wesley","John Wesley's Notes","COMMENTARY","en","1.1","Public Domain",false,14.0f,14.0f,true,"https://crosswire.org/ftpmirror/pub/sword/packages/rawzip/Wesley.zip"))
        add(SwordModuleEntity("EarlyFathers","Ante-Nicene Fathers (38 volumes)","CHURCH_FATHERS","en","1.0","Public Domain",false,180.0f,180.0f,true,"https://crosswire.org/ftpmirror/pub/sword/packages/rawzip/EarlyFathers.zip"))
        add(SwordModuleEntity("NiceneFathers","Nicene & Post-Nicene Fathers","CHURCH_FATHERS","en","1.0","Public Domain",false,220.0f,220.0f,true,"https://crosswire.org/ftpmirror/pub/sword/packages/rawzip/NiceneFathers.zip"))
        add(SwordModuleEntity("Easton","Easton's Bible Dictionary","DICTIONARY","en","1.3","Public Domain",false,5.5f,5.5f,true,"https://crosswire.org/ftpmirror/pub/sword/packages/rawzip/Easton.zip"))
        add(SwordModuleEntity("Smith","Smith's Bible Dictionary","DICTIONARY","en","1.2","Public Domain",false,4.8f,4.8f,true,"https://crosswire.org/ftpmirror/pub/sword/packages/rawzip/SmithBD.zip"))
        add(SwordModuleEntity("ISBE","International Standard Bible Encyclopedia","DICTIONARY","en","1.0","Public Domain",false,28.0f,28.0f,true,"https://crosswire.org/ftpmirror/pub/sword/packages/rawzip/ISBE.zip"))
    }

    // ── Download ───────────────────────────────────────────────────────────────

    suspend fun downloadModule(moduleName: String): Result<Unit> = withContext(Dispatchers.IO) {
        val module = availableModules.find { it.name == moduleName }
            ?: return@withContext Result.failure(Exception("Module not found: $moduleName"))

        try {
            _downloadProgress.value = DownloadProgress(moduleName)

            val request = Request.Builder().url(module.repoUrl).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                _downloadProgress.value = DownloadProgress(moduleName, error = "Download failed: ${response.code}")
                return@withContext Result.failure(Exception("HTTP ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty response"))
            val totalBytes = body.contentLength()
            val zipFile = File(modulesDir, "$moduleName.zip")
            var downloaded = 0L

            FileOutputStream(zipFile).use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        _downloadProgress.value = DownloadProgress(moduleName, downloaded, totalBytes)
                    }
                }
            }

            // Unzip
            val moduleDir = File(modulesDir, moduleName).also { it.mkdirs() }
            ZipFile(zipFile).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    val outFile = File(moduleDir, entry.name)
                    if (entry.isDirectory) { outFile.mkdirs() }
                    else {
                        outFile.parentFile?.mkdirs()
                        zip.getInputStream(entry).use { input ->
                            FileOutputStream(outFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }
            zipFile.delete()

            // Mark as installed in DB
            moduleDao.upsert(module.copy(isDownloaded = true, installedAt = System.currentTimeMillis()))
            _downloadProgress.value = DownloadProgress(moduleName, downloaded, downloaded, true)
            Result.success(Unit)

        } catch (e: Exception) {
            _downloadProgress.value = DownloadProgress(moduleName, error = e.message)
            Result.failure(e)
        }
    }

    suspend fun deleteModule(moduleName: String) = withContext(Dispatchers.IO) {
        File(modulesDir, moduleName).deleteRecursively()
        val module = moduleDao.getModule(moduleName)
        if (module != null) moduleDao.upsert(module.copy(isDownloaded = false, installedAt = null))
        verseDao.deleteTranslation(moduleName)
    }

    fun isInstalled(moduleName: String): Boolean =
        File(modulesDir, moduleName).exists()

    fun getModuleFile(moduleName: String): File = File(modulesDir, moduleName)

    suspend fun seedDefaultModules() = withContext(Dispatchers.IO) {
        // Insert catalog into DB so it appears in library even before downloading
        moduleDao.upsertAll(availableModules)
    }
}
