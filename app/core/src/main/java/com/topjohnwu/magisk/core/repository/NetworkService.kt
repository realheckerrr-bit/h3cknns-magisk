package com.topjohnwu.magisk.core.repository

import com.topjohnwu.magisk.core.BuildConfig
import com.topjohnwu.magisk.core.Const
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.core.data.GithubApiServices
import com.topjohnwu.magisk.core.data.RawUrl
import com.topjohnwu.magisk.core.ktx.dateFormat
import com.topjohnwu.magisk.core.model.Release
import com.topjohnwu.magisk.core.model.ReleaseAssets
import com.topjohnwu.magisk.core.model.UpdateInfo
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException

class NetworkService(
    private val raw: RawUrl,
    private val api: GithubApiServices,
) {
    /**
     * Fetch manager updates only from the h3cknn repository.
     * Magisk core releases are fetched separately by [fetchUpdate].
     */
    suspend fun fetchUpdate() = safe {
        findAppRelease().asAppInfo()
    }

    /** Fetch release notes for a Magisk core version from the official Magisk repo. */
    suspend fun fetchUpdate(version: Int) = safe {
        findMagiskRelease { it.versionCode == version }.asInfo()
    }

    private suspend fun findAppRelease(): Release? {
        return findRelease(
            owner = Const.Url.APP_GITHUB_OWNER,
            repo = Const.Url.APP_GITHUB_REPO,
            releaseFilter = {
                parseAppVersion(it.tag) != null &&
                    it.assets.any { asset -> asset.name == "app-release.apk" }
            },
            predicate = { true },
        )
    }

    private suspend fun findMagiskRelease(predicate: (Release) -> Boolean): Release? {
        return findRelease(
            owner = "topjohnwu",
            repo = "Magisk",
            releaseFilter = {
                it.tag.isNotEmpty() && (it.tag[0] == 'v' || it.tag.startsWith("canary"))
            },
            predicate = predicate,
        )
    }

    // Keep going through all release pages until we find a match.
    private suspend fun findRelease(
        owner: String,
        repo: String,
        releaseFilter: (Release) -> Boolean,
        predicate: (Release) -> Boolean,
    ): Release? {
        var page = 1
        while (true) {
            val response = api.fetchReleases(owner = owner, repo = repo, page = page)
            val releases = response.body() ?: throw HttpException(response)
            releases.removeAll { !releaseFilter(it) }
            releases.sortByDescending { it.createdTime }
            releases.find(predicate)?.let { return it }
            if (response.headers()["link"]?.contains("rel=\"next\"", ignoreCase = true) == true) {
                page += 1
            } else {
                return null
            }
        }
    }

    private inline fun Release?.asInfo(
        selector: (ReleaseAssets) -> Boolean = {
            // Default selector picks the non-debug APK
            it.name.run { endsWith(".apk") && !contains("debug") }
        }): UpdateInfo {
        return if (this == null) UpdateInfo()
        else if (tag[0] == 'v') asPublicInfo(selector)
        else asCanaryInfo(selector)
    }

    private fun Release?.asAppInfo(): UpdateInfo {
        val release = this ?: return UpdateInfo()
        val version = parseAppVersion(release.tag) ?: return UpdateInfo()
        val assetName = if (BuildConfig.DEBUG) "app-debug.apk" else "app-release.apk"
        val asset = release.assets.find { it.name == assetName }
            ?: release.assets.find { it.name == "app-release.apk" }
            ?: return UpdateInfo()
        val date = dateFormat.format(release.createdTime)
        return UpdateInfo(
            version = version.name,
            versionCode = version.code,
            link = asset.url,
            note = "## $date ${release.name}\n\n${release.body}"
        )
    }

    private inline fun Release.asPublicInfo(selector: (ReleaseAssets) -> Boolean): UpdateInfo {
        val version = tag.drop(1)
        val date = dateFormat.format(createdTime)
        return UpdateInfo(
            version = version,
            versionCode = versionCode,
            link = assets.find(selector)!!.url,
            note = "## $date $name\n\n$body"
        )
    }

    private inline fun Release.asCanaryInfo(selector: (ReleaseAssets) -> Boolean): UpdateInfo {
        return UpdateInfo(
            version = name.substring(8, 16),
            versionCode = versionCode,
            link = assets.find(selector)!!.url,
            note = "## $name\n\n$body"
        )
    }

    private data class AppVersion(
        val name: String,
        val code: Int,
    )

    private fun parseAppVersion(tag: String): AppVersion? {
        val match = APP_TAG_PATTERN.matchEntire(tag) ?: return null
        val major = match.groupValues[1].toInt()
        val minor = match.groupValues[2].toInt()
        val patch = match.groupValues[3].toInt()
        // Keep APK version codes above the old Magisk-based manager builds.
        val code = (APP_VERSION_CODE_BASE + major) * 10_000 + minor * 100 + patch
        return AppVersion("$major.$minor.$patch", code)
    }

    private inline fun <T> safe(factory: () -> T): T? {
        return try {
            if (Info.isConnected.value == true)
                factory()
            else
                null
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }

    private inline fun <T> wrap(factory: () -> T): T {
        return try {
            factory()
        } catch (e: HttpException) {
            throw IOException(e)
        }
    }

    // Fetch files
    suspend fun fetchFile(url: String) = wrap { raw.fetchFile(url) }
    suspend fun fetchFileBytes(url: String) = wrap { raw.fetchFile(url).bytes() }
    suspend fun fetchString(url: String) = wrap { raw.fetchString(url) }
    suspend fun fetchModuleJson(url: String) = wrap { raw.fetchModuleJson(url) }

    private companion object {
        val APP_TAG_PATTERN = Regex("^h3cknn-v(\\d+)\\.(\\d+)\\.(\\d+)$")
        const val APP_VERSION_CODE_BASE = 4
    }
}
