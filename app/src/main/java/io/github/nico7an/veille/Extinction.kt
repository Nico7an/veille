package io.github.nico7an.veille

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent

/**
 * Fin du minuteur : pause de la lecture, volume coupé, puis verrouillage de l'écran.
 * Chaque étape est tentée même si la précédente a échoué : le but est que rien ne joue toute la nuit.
 */
object Extinction {
    /** Délai laissé aux lecteurs pour se mettre en pause avant de couper le son. */
    private const val DELAI_PAUSE = 2_000L

    fun lancer(c: Context, fini: () -> Unit) {
        essayer { Minuteur.terminer(c) }
        val audio = c.getSystemService(AudioManager::class.java)

        // 1. Pause : touche média vers la session active, puis prise du focus audio
        //    pour que les autres lecteurs (navigateur, etc.) s'arrêtent aussi.
        //    Le focus n'est pas rendu : un lecteur qui le récupérerait pourrait reprendre.
        essayer {
            val t = SystemClock.uptimeMillis()
            audio.dispatchMediaKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE, 0))
            audio.dispatchMediaKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE, 0))
        }
        essayer {
            audio.requestAudioFocus(
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setOnAudioFocusChangeListener { }
                    .build()
            )
        }

        val suite = Runnable {
            try {
                essayer { couperVolume(audio) }
                essayer { AdminVeille.verrouiller(c) }
            } finally {
                fini()
            }
        }
        try {
            Handler(Looper.getMainLooper()).postDelayed(suite, DELAI_PAUSE)
        } catch (_: Exception) {
            suite.run()
        }
    }

    private fun couperVolume(audio: AudioManager) {
        essayer { audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0) }
        if (audio.getStreamVolume(AudioManager.STREAM_MUSIC) > 0) {
            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
        }
    }

    private inline fun essayer(bloc: () -> Unit) {
        try {
            bloc()
        } catch (_: Exception) {
        }
    }
}
