package io.github.nico7an.veille

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class FinReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
        when (intent.action) {
            ANNULER -> Minuteur.annuler(c)
            PLUS -> Minuteur.prolonger(c, 15)
            FIN -> {
                val attente = goAsync()
                Extinction.lancer(c.applicationContext) { attente.finish() }
            }
        }
    }

    companion object {
        const val FIN = "io.github.nico7an.veille.FIN"
        const val PLUS = "io.github.nico7an.veille.PLUS"
        const val ANNULER = "io.github.nico7an.veille.ANNULER"
    }
}
