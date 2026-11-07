package io.github.nico7an.veille

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context

/** Administrateur d'appareil limité au verrouillage de l'écran (équivalent du bouton marche/arrêt). */
class AdminVeille : DeviceAdminReceiver() {
    companion object {
        fun composant(c: Context) = ComponentName(c, AdminVeille::class.java)

        fun active(c: Context): Boolean =
            c.getSystemService(DevicePolicyManager::class.java).isAdminActive(composant(c))

        fun verrouiller(c: Context) {
            if (active(c)) c.getSystemService(DevicePolicyManager::class.java).lockNow()
        }
    }
}
