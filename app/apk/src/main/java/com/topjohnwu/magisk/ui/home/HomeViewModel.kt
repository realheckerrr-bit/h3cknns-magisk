package com.topjohnwu.magisk.ui.home

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import com.topjohnwu.magisk.arch.AsyncLoadViewModel
import com.topjohnwu.magisk.core.AppContext
import com.topjohnwu.magisk.core.BuildConfig
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.core.ktx.await
import com.topjohnwu.magisk.core.ktx.toast
import com.topjohnwu.magisk.core.repository.NetworkService
import com.topjohnwu.magisk.utils.asFlow
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.topjohnwu.magisk.core.R as CoreR

class HomeViewModel(
    private val svc: NetworkService
) : AsyncLoadViewModel() {

    enum class State {
        LOADING, INVALID, OUTDATED, UP_TO_DATE
    }

    data class UiState(
        val isNoticeVisible: Boolean = Config.safetyNotice,
        val appState: State = State.LOADING,
        val managerRemoteVersion: String = "",
        val magiskLatestVersion: String = "",
        val managerProgress: Int = 0,
        val showUninstall: Boolean = false,
        val showManagerInstall: Boolean = false,
        val showHideRestore: Boolean = false,
        val envFixCode: Int = 0,
        val magiskState: State = computeMagiskState(),
        val magiskInstalledVersion: String = computeMagiskInstalledVersion(),
        val managerInstalledVersion: String = computeManagerInstalledVersion(),
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val magiskState get() = _uiState.value.magiskState
    val magiskInstalledVersion get() = _uiState.value.magiskInstalledVersion
    val managerInstalledVersion get() = _uiState.value.managerInstalledVersion

    companion object {
        private var checkedEnv = false

        fun computeMagiskState(latestVersionCode: Int = -1) = when {
            Info.isRooted && Info.env.isUnsupported -> State.OUTDATED
            !Info.env.isActive -> State.INVALID
            Info.env.versionCode < maxOf(BuildConfig.APP_VERSION_CODE, latestVersionCode) -> State.OUTDATED
            else -> State.UP_TO_DATE
        }

        fun computeMagiskInstalledVersion() = Info.env.run {
            if (isActive)
                "$versionString ($versionCode)" + if (isDebug) " (D)" else ""
            else
                ""
        }

        fun computeManagerInstalledVersion() =
            "${BuildConfig.MANAGER_VERSION_NAME} (${BuildConfig.MANAGER_VERSION_CODE})" +
                if (BuildConfig.DEBUG) " (D)" else ""
    }

    init {
        viewModelScope.launch {
            Info.isConnected.asFlow().collect {
                startLoading()
            }
        }
    }

    override suspend fun doLoadWork() {
        _uiState.update {
            it.copy(
                appState = State.LOADING,
                magiskState = computeMagiskState(),
                magiskInstalledVersion = computeMagiskInstalledVersion(),
                managerInstalledVersion = computeManagerInstalledVersion(),
            )
        }
        val magiskUpdate = svc.fetchMagiskLatest()
        val managerUpdate = Info.fetchUpdate(svc)
        _uiState.update {
            it.copy(
                magiskState = computeMagiskState(magiskUpdate?.versionCode ?: -1),
                magiskLatestVersion = magiskUpdate?.let { update ->
                    "${update.version} (${update.versionCode})"
                }.orEmpty(),
                appState = managerUpdate?.let { update ->
                    if (BuildConfig.MANAGER_VERSION_CODE < update.versionCode) State.OUTDATED
                    else State.UP_TO_DATE
                } ?: State.INVALID,
                managerRemoteVersion = managerUpdate?.let { update ->
                    "${update.version} (${update.versionCode})" + if (BuildConfig.DEBUG) " (D)" else ""
                }.orEmpty(),
            )
        }
        ensureEnv()
    }

    fun onLinkPressed(link: String) {
        val intent = Intent(Intent.ACTION_VIEW, link.toUri())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            AppContext.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            AppContext.toast(CoreR.string.open_link_failed_toast, Toast.LENGTH_SHORT)
        }
    }

    fun onDeletePressed() {
        _uiState.update { it.copy(showUninstall = true) }
    }

    fun onUninstallConsumed() {
        _uiState.update { it.copy(showUninstall = false) }
    }

    fun onManagerPressed() {
        when (_uiState.value.appState) {
            State.LOADING -> showSnackbar(CoreR.string.loading)
            State.INVALID -> showSnackbar(CoreR.string.no_connection)
            else -> _uiState.update { it.copy(showManagerInstall = true) }
        }
    }

    fun onManagerInstallConsumed() {
        _uiState.update { it.copy(showManagerInstall = false) }
    }

    fun onHideRestorePressed() {
        _uiState.update { it.copy(showHideRestore = true) }
    }

    fun onHideRestoreConsumed() {
        _uiState.update { it.copy(showHideRestore = false) }
    }

    fun onEnvFixConsumed() {
        _uiState.update { it.copy(envFixCode = 0) }
    }

    fun hideNotice() {
        Config.safetyNotice = false
        _uiState.update { it.copy(isNoticeVisible = false) }
    }

    private suspend fun ensureEnv() {
        if (magiskState == State.INVALID || checkedEnv) return
        val cmd = "env_check ${Info.env.versionString} ${Info.env.versionCode}"
        val code = Shell.cmd(cmd).await().code
        if (code != 0) {
            _uiState.update { it.copy(envFixCode = code) }
        }
        checkedEnv = true
    }
}
