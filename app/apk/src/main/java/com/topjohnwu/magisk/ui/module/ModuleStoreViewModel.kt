package com.topjohnwu.magisk.ui.module

import android.net.Uri
import com.topjohnwu.magisk.arch.AsyncLoadViewModel
import com.topjohnwu.magisk.core.di.ServiceLocator
import com.topjohnwu.magisk.core.download.Subject
import com.topjohnwu.magisk.core.utils.MediaStoreUtils
import com.topjohnwu.magisk.ui.flash.FlashUtils
import com.topjohnwu.magisk.view.Notifications
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

private const val ALT_REPO_INDEX =
    "https://raw.githubusercontent.com/Magisk-Modules-Alt-Repo/json/main/modules.json"

data class StoreModule(
    val id: String,
    val name: String,
    val stars: Int,
    val zipUrl: String,
    val notesUrl: String,
    val propUrl: String,
    val lastUpdate: Long,
)

data class ModuleStoreState(
    val loading: Boolean = true,
    val query: String = "",
    val modules: List<StoreModule> = emptyList(),
    val error: String? = null,
)

/**
 * Reads the public, moderated Magisk-Modules-Alt-Repo index.
 *
 * The index is deliberately fetched without a root shell so the catalog can be browsed on any
 * supported Android device. Installation is kept in the UI layer and is explicitly root-gated.
 */
class ModuleStoreViewModel : AsyncLoadViewModel() {

    private val _uiState = MutableStateFlow(ModuleStoreState())
    val uiState: StateFlow<ModuleStoreState> = _uiState.asStateFlow()

    override suspend fun doLoadWork() {
        _uiState.update { it.copy(loading = true, error = null) }
        try {
            val modules = withContext(Dispatchers.IO) {
                parseIndex(ServiceLocator.networkService.fetchString(ALT_REPO_INDEX))
            }
            _uiState.update { state ->
                state.copy(loading = false, modules = modules, error = null)
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    loading = false,
                    error = e.message ?: "Unable to load the module catalog"
                )
            }
        }
    }

    fun setQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun filteredModules(state: ModuleStoreState): List<StoreModule> {
        val query = state.query.trim()
        if (query.isEmpty()) return state.modules
        return state.modules.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.id.contains(query, ignoreCase = true)
        }
    }

    private fun parseIndex(raw: String): List<StoreModule> {
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
                        stars = item.optInt("stars", 0),
                        zipUrl = zipUrl,
                        notesUrl = item.optString("notes_url").trim(),
                        propUrl = item.optString("prop_url").trim(),
                        lastUpdate = item.optLong("last_update", 0L),
                    )
                )
            }
        }.sortedWith(compareByDescending<StoreModule> { it.stars }.thenBy { it.name.lowercase() })
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
