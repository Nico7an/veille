package io.github.nico7an.veille

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/** Tuile des réglages rapides : lance le minuteur avec la dernière durée, ou l'annule. */
class TuileMinuteur : TileService() {
    override fun onStartListening() = afficher()

    override fun onClick() {
        if (Minuteur.actif(this)) Minuteur.annuler(this) else Minuteur.demarrer(this, Minuteur.duree(this))
        afficher()
    }

    private fun afficher() {
        val tuile = qsTile ?: return
        val fin = Minuteur.fin(this)
        tuile.state = if (fin != 0L) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= 29) {
            tuile.subtitle = if (fin != 0L) "jusqu'à ${Minuteur.heure(fin)}" else "${Minuteur.duree(this)} min"
        }
        tuile.updateTile()
    }
}
