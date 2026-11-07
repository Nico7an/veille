package io.github.nico7an.veille

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent

/** Fin du minuteur : pause de la lecture, volume coupé, puis verrouillage de l'écran. */
object Extinction {
    /** Délai laissé aux lecteurs pour se mettre en pause avant de couper le son. */
    private const val DELAI_PAUSE = 2_000L

    fun lancer(c: Context, fini: () -> Unit) {
        Minuteur.terminer(c)
        val audio = c.getSystemService(AudioManager::class.java)

        // 1. Pause : touche média vers la session active, puis prise du focus audio
        //    pour que les autres lecteurs (navigateur, etc.) s'arrêtent aussi.
        val t = SystemClock.uptimeMillis()
        audio.dispatchMediaKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE, 0))
        audio.dispatchMediaKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE, 0))
        val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener { }
            .build()
        audio.requestAudioFocus(focus)

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                // 2. Volume coupé.
                try {
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                } catch (_: SecurityException) {
                }
                audio.abandonAudioFocusRequest(focus)

                // 3. Mise en veille.
                AdminVeille.verrouiller(c)
            } finally {
                fini()
            }
        }, DELAI_PAUSE)
    }
}
