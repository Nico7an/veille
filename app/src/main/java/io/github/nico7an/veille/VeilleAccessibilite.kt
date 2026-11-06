package io.github.nico7an.veille

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

/**
 * Service d'accessibilité minimal : il ne lit rien à l'écran, il sert seulement
 * à verrouiller l'appareil (équivalent du bouton marche/arrêt) et à remettre
 * le volume au déverrouillage.
 */
class VeilleAccessibilite : AccessibilityService() {
    private val deverrouillage = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) = Extinction.remettreVolume(c)
    }

    override fun onServiceConnected() {
        instance = this
        registerReceiver(deverrouillage, IntentFilter(Intent.ACTION_USER_PRESENT))
    }

    override fun onDestroy() {
        instance = null
        try {
            unregisterReceiver(deverrouillage)
        } catch (_: IllegalArgumentException) {
        }
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    companion object {
        @Volatile
        private var instance: VeilleAccessibilite? = null

        fun verrouiller(): Boolean =
            instance?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) ?: false

        fun active(c: Context): Boolean {
            val nom = ComponentName(c, VeilleAccessibilite::class.java).flattenToString()
            val actifs = Settings.Secure.getString(c.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
            return actifs.split(':').any { it.equals(nom, ignoreCase = true) }
        }
    }
}
