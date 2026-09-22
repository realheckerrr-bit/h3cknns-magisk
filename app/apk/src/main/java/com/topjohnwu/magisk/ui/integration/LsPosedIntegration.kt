package com.topjohnwu.magisk.ui.integration

import android.content.Context
import android.content.Intent

private const val LSPOSED_MANAGER_PACKAGE = "org.lsposed.manager"
private const val LSPOSED_FRAMEWORK_PACKAGE = "org.lsposed.lspd"
private const val LSPOSED_SOURCE_URL = "https://github.com/LSPosed/LSPosed"

data class LsPosedStatus(
    val managerPackage: String? = null,
    val frameworkInstalled: Boolean = false,
) {
    val managerInstalled: Boolean
        get() = managerPackage != null

    val detected: Boolean
        get() = managerInstalled || frameworkInstalled
}

/** Small, package-only integration layer for the optional LSPosed companion app. */
object LsPosedIntegration {

    fun status(context: Context): LsPosedStatus {
        val packageManager = context.packageManager
        val manager = runCatching {
            packageManager.getApplicationInfo(LSPOSED_MANAGER_PACKAGE, 0)
            LSPOSED_MANAGER_PACKAGE
        }.getOrNull()
        val frameworkInstalled = runCatching {
            packageManager.getApplicationInfo(LSPOSED_FRAMEWORK_PACKAGE, 0)
        }.isSuccess
        return LsPosedStatus(manager, frameworkInstalled)
    }

    fun openManager(context: Context, status: LsPosedStatus): Boolean {
        val packageName = status.managerPackage ?: return false
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun openSource(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(LSPOSED_SOURCE_URL))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
