package com.topjohnwu.magisk.ui.module

import android.net.Uri
import com.topjohnwu.magisk.arch.AsyncLoadViewModel
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.core.di.ServiceLocator
import com.topjohnwu.magisk.core.download.Subject
import com.topjohnwu.magisk.core.utils.MediaStoreUtils
import com.topjohnwu.magisk.ui.flash.FlashUtils
import com.topjohnwu.magisk.view.Notifications
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import java.net.URI

private const val ALT_REPO_INDEX =
    "https://raw.githubusercontent.com/Magisk-Modules-Alt-Repo/json/main/modules.json"
private const val GOOGLERS_REPO_INDEX =
    "https://gr.dergoogler.com/gmr/json/modules.json"
private const val IZZY_REPO_INDEX =
    "https://apt.izzysoft.de/magisk/json/modules.json"
private const val RIKJ000_REPO_INDEX =
    "https://rikj000.github.io/Magisk-Modules-Rikj000-Repo/json/modules.json"
private const val FONT_REPO_INDEX =
    "https://codeberg.org/fruitsnack/magisk-font-repo/raw/branch/main/json/modules.json"

const val ALL_MODULE_REPOSITORIES = "all"

enum class ModuleRepositoryFormat {
    ALT_REPO,
    MMRL,
}

enum class ModuleSort {
    POPULAR,
    RECENT,
    NAME,
}

data class ModuleRepository(
    val id: String,
    val name: String,
    val indexUrl: String,
    val websiteUrl: String,
    val format: ModuleRepositoryFormat,
)

val MODULE_REPOSITORIES = listOf(
    ModuleRepository(
        id = "alt_repo",
        name = "Magisk Modules Alt-Repo",
        indexUrl = ALT_REPO_INDEX,
        websiteUrl = "https://github.com/Magisk-Modules-Alt-Repo/json",
        format = ModuleRepositoryFormat.ALT_REPO,
    ),
    ModuleRepository(
        id = "googlers",
        name = "Googlers Magisk Repo",
        indexUrl = GOOGLERS_REPO_INDEX,
        websiteUrl = "https://gr.dergoogler.com/gmr/",
        format = ModuleRepositoryFormat.MMRL,
    ),
    ModuleRepository(
        id = "izzy",
        name = "IzzyOnDroid Magisk Repo",
        indexUrl = IZZY_REPO_INDEX,
        websiteUrl = "https://apt.izzysoft.de/magisk/",
        format = ModuleRepositoryFormat.MMRL,
    ),
    ModuleRepository(
        id = "rikj000",
        name = "Magisk Modules - Rikj000's Repo",
        indexUrl = RIKJ000_REPO_INDEX,
        websiteUrl = "https://rikj000.github.io/Magisk-Modules-Rikj000-Repo/",
        format = ModuleRepositoryFormat.MMRL,
    ),
    ModuleRepository(
        id = "fonts",
        name = "Magisk Font Collection",
        indexUrl = FONT_REPO_INDEX,
        websiteUrl = "https://codeberg.org/fruitsnack/magisk-font-repo",
        format = ModuleRepositoryFormat.MMRL,
    ),
)

data class StoreModule(
    val id: String,
    val name: String,
    val repositoryId: String,
    val repositoryName: String,
    val stars: Int,
    val zipUrl: String,
    val notesUrl: String,
    val propUrl: String,
    val lastUpdate: Long,
    val description: String = "",
    val author: String = "",
    val version: String = "",
    val versionCode: Int = -1,
    val readmeUrl: String = "",
    val sourceUrl: String = "",
    val iconUrls: List<String> = emptyList(),
)

data class StoreModuleDetails(
    val module: StoreModule,
    val description: String,
    val readme: String,
    val iconUrls: List<String>,
    val screenshotUrls: List<String>,
)

data class ModuleStoreState(
    val loading: Boolean = true,
    val query: String = "",
    val selectedRepositoryId: String = ALL_MODULE_REPOSITORIES,
    val sort: ModuleSort = ModuleSort.POPULAR,
    val favoritesOnly: Boolean = false,
    val favoriteIds: Set<String> = Config.moduleFavorites,
    val modules: List<StoreModule> = emptyList(),
    val loadedRepositoryIds: Set<String> = emptySet(),
    val repositoryErrors: Map<String, String> = emptyMap(),
    val error: String? = null,
)

private data class RepositoryLoadResult(
    val repository: ModuleRepository,
    val modules: List<StoreModule> = emptyList(),
    val error: String? = null,
)

/**
 * Reads public Magisk module indexes without a root shell so the catalog can be browsed on any
 * supported Android device. Installation is kept in the UI layer and is explicitly root-gated.
 *
 * The built-in repositories use either the Alt-Repo format or the MMRL JSON format. A repository
 * failing to load does not hide modules from the other sources.
 */
class ModuleStoreViewModel : AsyncLoadViewModel() {

    private val _uiState = MutableStateFlow(ModuleStoreState())
    val uiState: StateFlow<ModuleStoreState> = _uiState.asStateFlow()

    override suspend fun doLoadWork() {
        _uiState.update { it.copy(loading = true, error = null) }
        val results = withContext(Dispatchers.IO) {
            coroutineScope {
                MODULE_REPOSITORIES.map { repository ->
                    async {
                        try {
                            val raw = ServiceLocator.networkService.fetchString(repository.indexUrl)
                            RepositoryLoadResult(
                                repository = repository,
                                modules = parseIndex(raw, repository),
                            )
                        } catch (e: Exception) {
                            RepositoryLoadResult(
                                repository = repository,
                                error = e.message ?: "Unable to load repository",
                            )
                        }
                    }
                }.awaitAll()
            }
        }
        val loaded = results.filter { it.error == null }
        val errors = results
            .filter { it.error != null }
            .associate { it.repository.id to (it.error ?: "Unable to load repository") }
        val modules = results.flatMap { it.modules }
        _uiState.update { state ->
            state.copy(
                loading = false,
                modules = modules,
                loadedRepositoryIds = loaded.map { it.repository.id }.toSet(),
                repositoryErrors = errors,
                error = if (modules.isEmpty() && errors.size == MODULE_REPOSITORIES.size) {
                    "Unable to load any module repository"
                } else {
                    null
                },
            )
        }
    }

    fun setQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun selectRepository(repositoryId: String) {
        _uiState.update { it.copy(selectedRepositoryId = repositoryId) }
    }

    fun setSort(sort: ModuleSort) {
        _uiState.update { it.copy(sort = sort) }
    }

    fun setFavoritesOnly(enabled: Boolean) {
        _uiState.update { it.copy(favoritesOnly = enabled) }
    }

    fun isFavorite(module: StoreModule): Boolean =
        module.favoriteKey() in _uiState.value.favoriteIds

    fun toggleFavorite(module: StoreModule) {
        val key = module.favoriteKey()
        val current = _uiState.value.favoriteIds
        val next = if (key in current) current - key else current + key
        Config.moduleFavorites = next
        _uiState.update { it.copy(favoriteIds = next) }
    }

    fun filteredModules(state: ModuleStoreState): List<StoreModule> {
        val query = state.query.trim()
        val sourceModules = if (state.selectedRepositoryId == ALL_MODULE_REPOSITORIES) {
            state.modules.distinctBy { it.id.lowercase() }
        } else {
            state.modules.filter { it.repositoryId == state.selectedRepositoryId }
        }
        val favoriteFiltered = if (state.favoritesOnly) {
            sourceModules.filter { it.favoriteKey() in state.favoriteIds }
        } else {
            sourceModules
        }
        val filtered = if (query.isEmpty()) {
            favoriteFiltered
        } else {
            favoriteFiltered.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.id.contains(query, ignoreCase = true)
            }
        }
        return when (state.sort) {
            ModuleSort.POPULAR -> filtered.sortedWith(
                compareByDescending<StoreModule> { it.stars }
                    .thenByDescending { it.lastUpdate }
                    .thenBy { it.name.lowercase() },
            )
            ModuleSort.RECENT -> filtered.sortedWith(
                compareByDescending<StoreModule> { it.lastUpdate }
                    .thenByDescending { it.stars }
                    .thenBy { it.name.lowercase() },
            )
            ModuleSort.NAME -> filtered.sortedBy { it.name.lowercase() }
        }
    }

    private fun StoreModule.favoriteKey(): String = "$repositoryId::$id"

    suspend fun loadDetails(module: StoreModule): StoreModuleDetails = withContext(Dispatchers.IO) {
        val readme = module.readmeUrl.takeIf(String::isNotBlank)?.let { url ->
            runCatching { ServiceLocator.networkService.fetchString(url) }.getOrDefault("")
        }.orEmpty()
        val readmeImages = extractImageUrls(readme, module.readmeUrl)
        val iconUrls = (module.iconUrls + readmeImages.filter(::isLikelyIcon))
            .distinct()
        val screenshotUrls = readmeImages
            .filterNot(::isBadge)
            .filterNot { it in iconUrls }
            .distinct()
            .take(MAX_SCREENSHOTS)
        StoreModuleDetails(
            module = module,
            description = module.description.ifBlank { readmeSummary(readme) },
            readme = readme,
            iconUrls = iconUrls,
            screenshotUrls = screenshotUrls,
        )
    }

    private fun parseIndex(raw: String, repository: ModuleRepository): List<StoreModule> = when (
        repository.format
    ) {
        ModuleRepositoryFormat.ALT_REPO -> parseAltRepoIndex(raw, repository)
        ModuleRepositoryFormat.MMRL -> parseMmrlIndex(raw, repository)
    }

    private fun parseAltRepoIndex(raw: String, repository: ModuleRepository): List<StoreModule> {
        val array = JSONObject(raw).optJSONArray("modules") ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optString("id").trim()
                val zipUrl = item.optString("zip_url").trim()
                if (id.isEmpty() || zipUrl.isEmpty()) continue
                add(
                    StoreModule(
                        id = id,
                        name = id.toDisplayName(),
                        repositoryId = repository.id,
                        repositoryName = repository.name,
                        stars = item.optInt("stars", 0),
                        zipUrl = zipUrl,
                        notesUrl = item.optString("notes_url").trim(),
                        propUrl = item.optString("prop_url").trim(),
                        lastUpdate = item.optLong("last_update", 0L),
                        readmeUrl = item.optString("notes_url").trim(),
                        sourceUrl = githubRepositoryUrl(
                            item.optString("notes_url").trim().ifBlank {
                                item.optString("prop_url").trim()
                            }
                        ).orEmpty(),
                        iconUrls = githubIconUrls(
                            githubRepositoryUrl(
                                item.optString("notes_url").trim().ifBlank {
                                    item.optString("prop_url").trim()
                                }
                            ).orEmpty()
                        ),
                    )
                )
            }
        }.sortedWith(compareByDescending<StoreModule> { it.stars }.thenBy { it.name.lowercase() })
    }

    private fun parseMmrlIndex(raw: String, repository: ModuleRepository): List<StoreModule> {
        val array = JSONObject(raw).optJSONArray("modules") ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optString("id").trim()
                val version = item.latestVersion() ?: continue
                val zipUrl = version.optString("zipUrl").trim()
                if (id.isEmpty() || zipUrl.isEmpty()) continue

                val track = item.optJSONObject("track")
                val sourceUrl = track?.optString("source")?.trim().orEmpty()
                val readmeUrl = item.optString("readme").trim()
                val notesUrl = item.optString("readme").trim().ifBlank {
                    track?.optString("homepage")?.trim().orEmpty()
                }
                val propUrl = track?.optString("source")?.trim().orEmpty().ifBlank {
                    item.optString("support").trim()
                }
                add(
                    StoreModule(
                        id = id,
                        name = item.optString("name").trim().ifBlank { id.toDisplayName() },
                        repositoryId = repository.id,
                        repositoryName = repository.name,
                        stars = item.optInt("stars", 0),
                        zipUrl = zipUrl,
                        notesUrl = notesUrl,
                        propUrl = propUrl,
                        lastUpdate = version.optDouble("timestamp", 0.0).toTimestampMillis(),
                        description = item.optString("description").trim(),
                        author = item.optString("author").trim(),
                        version = item.optString("version").trim(),
                        versionCode = item.optInt("versionCode", -1),
                        readmeUrl = readmeUrl,
                        sourceUrl = sourceUrl,
                        iconUrls = githubIconUrls(
                            githubRepositoryUrl(sourceUrl).orEmpty()
                        ),
                    )
                )
            }
        }.sortedWith(compareByDescending<StoreModule> { it.stars }.thenBy { it.name.lowercase() })
    }

    private fun JSONObject.latestVersion(): JSONObject? {
        val versions = optJSONArray("versions")
        if (versions != null && versions.length() > 0) {
            var latest: JSONObject? = null
            var latestTimestamp = Double.MIN_VALUE
            for (index in 0 until versions.length()) {
                val version = versions.optJSONObject(index) ?: continue
                val timestamp = version.optDouble("timestamp", 0.0)
                if (latest == null || timestamp >= latestTimestamp) {
                    latest = version
                    latestTimestamp = timestamp
                }
            }
            if (latest != null) return latest
        }

        val states = optJSONObject("states") ?: return null
        val directZipUrl = states.optString("zipUrl").trim()
        if (directZipUrl.isNotEmpty()) return states
        var latest: JSONObject? = null
        var latestTimestamp = Double.MIN_VALUE
        for (key in states.keys()) {
            val state = states.optJSONObject(key) ?: continue
            val timestamp = state.optDouble("timestamp", 0.0)
            if (state.optString("zipUrl").isNotBlank() &&
                (latest == null || timestamp >= latestTimestamp)
            ) {
                latest = state
                latestTimestamp = timestamp
            }
        }
        return latest
    }

    private fun Double.toTimestampMillis(): Long = when {
        this <= 0.0 -> 0L
        this < 10_000_000_000.0 -> (this * 1000.0).toLong()
        else -> toLong()
    }

    private fun String.toDisplayName(): String =
        replace(Regex("([a-z])([A-Z])"), "$1 $2")
            .replace('_', ' ')
            .replace('-', ' ')
            .split(Regex("\\s+"))
            .filter(String::isNotBlank)
            .joinToString(" ") { word ->
                word.replaceFirstChar { char -> char.uppercase() }
            }

    private fun extractImageUrls(markdown: String, baseUrl: String): List<String> {
        val markdownImages = Regex("!\\[[^]]*]\\(([^)]+)\\)")
            .findAll(markdown)
            .map { it.groupValues[1].substringBefore(' ').trim('<', '>') }
        val htmlImages = Regex("<img[^>]+src=[\\\"']([^\\\"']+)", RegexOption.IGNORE_CASE)
            .findAll(markdown)
            .map { it.groupValues[1] }
        return (markdownImages + htmlImages)
            .mapNotNull { resolveUrl(baseUrl, it) }
            .filter { it.startsWith("https://") || it.startsWith("http://") }
            .distinct()
            .toList()
    }

    private fun readmeSummary(markdown: String): String {
        return markdown.lines()
            .asSequence()
            .map(String::trim)
            .dropWhile { it.isEmpty() || it.startsWith("#") || it.startsWith("!") || isBadge(it) }
            .takeWhile { it.isNotEmpty() }
            .joinToString(" ")
            .replace(Regex("\\[([^]]+)]\\([^)]*\\)"), "$1")
            .replace(Regex("[`*_]"), "")
            .trim()
            .take(MAX_DESCRIPTION_LENGTH)
    }

    private fun isBadge(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("shields.io") ||
            lower.contains("badge") ||
            lower.contains("badgen.net")
    }

    private fun isLikelyIcon(url: String): Boolean {
        val path = runCatching { URI(url).path.orEmpty() }.getOrDefault(url).lowercase()
        return path.contains("icon") || path.contains("logo")
    }

    private fun resolveUrl(baseUrl: String, url: String): String? {
        if (url.startsWith("data:") || url.startsWith("//")) return null
        return runCatching {
            if (url.startsWith("http://") || url.startsWith("https://")) url
            else URI(baseUrl).resolve(url).toString()
        }.getOrNull()
    }

    private fun githubRepositoryUrl(url: String): String? {
        val match = Regex("(?:github\\.com|raw\\.githubusercontent\\.com)/([^/]+)/([^/#?]+)")
            .find(url) ?: return null
        val owner = match.groupValues[1]
        val repo = match.groupValues[2]
            .removeSuffix(".git")
            .takeIf { it !in setOf("raw", "blob", "tree") }
            ?: return null
        return "https://github.com/$owner/$repo"
    }

    private fun githubIconUrls(repositoryUrl: String): List<String> {
        if (repositoryUrl.isBlank()) return emptyList()
        val path = repositoryUrl.removePrefix("https://github.com/").trimEnd('/')
        val iconPaths = listOf(
            "icon.png",
            "icon.webp",
            "icon.jpg",
            "icon.jpeg",
            ".github/icon.png",
            "assets/icon.png",
        )
        return listOf("main", "master").flatMap { branch ->
            iconPaths.map { icon ->
                "https://raw.githubusercontent.com/$path/$branch/$icon"
            }
        }
    }

    private companion object {
        const val MAX_SCREENSHOTS = 8
        const val MAX_DESCRIPTION_LENGTH = 600
    }
}

@Parcelize
class StoreModuleSubject(
    override val url: String,
    override val title: String,
    val moduleId: String,
    override val notifyId: Int = Notifications.nextId(),
) : Subject() {

    @IgnoredOnParcel
    override val file: Uri by lazy { MediaStoreUtils.getFile(title).uri }

    override fun pendingIntent(context: android.content.Context) =
        FlashUtils.installIntent(context, file)
}
