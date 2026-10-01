package com.aura.dashboard.domain.model

/** An application pinned by the user to the Quick Apps launcher. */
data class QuickApp(
    val packageName: String,
    /** Label shown in the UI: the custom name if set, otherwise the installed app label. */
    val label: String,
    val defaultLabel: String,
    val customLabel: String?,
    val isInstalled: Boolean,
    val showOnHome: Boolean,
    val position: Int,
)

/** A launchable application currently installed on the device. */
data class InstalledApp(
    val packageName: String,
    val label: String,
)
