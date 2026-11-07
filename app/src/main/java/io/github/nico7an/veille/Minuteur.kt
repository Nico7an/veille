package io.github.nico7an.veille

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.service.quicksettings.TileService

/** État du minuteur : heure de fin (stockée), alarme exacte et notification à rebours. */
object Minuteur {
    private const val CANAL = "minuteur"
    private const val NOTIF = 1

    fun prefs(c: Context): SharedPreferences = c.getSharedPreferences("veille", Context.MODE_PRIVATE)

    /** Heure de fin en ms, ou 0 si aucun minuteur en cours. */
    fun fin(c: Context): Long {
        val fin = prefs(c).getLong("fin", 0L)
        return if (fin > System.currentTimeMillis()) fin else 0L
    }

    fun actif(c: Context) = fin(c) != 0L

    /** Début du minuteur, pour la progression de l'anneau. */
    fun debut(c: Context): Long = prefs(c).getLong("debut", System.currentTimeMillis())

    fun duree(c: Context) = prefs(c).getInt("duree", 45)

    fun demarrer(c: Context, minutes: Int) {
        val fin = System.currentTimeMillis() + minutes * 60_000L
        prefs(c).edit().putLong("debut", System.currentTimeMillis()).putLong("fin", fin).putInt("duree", minutes).apply()
        programmer(c, fin)
    }

    fun prolonger(c: Context, minutes: Int) {
        val actuelle = fin(c)
        if (actuelle == 0L) return demarrer(c, minutes)
        val fin = actuelle + minutes * 60_000L
        prefs(c).edit().putLong("fin", fin).apply()
        programmer(c, fin)
    }

    /** Les alarmes sont effacées au redémarrage et à la mise à jour de l'appli : on les reprogramme. */
    fun relancer(c: Context) {
        val fin = fin(c)
        if (fin != 0L) programmer(c, fin) else terminer(c)
    }

    fun annuler(c: Context) {
        c.getSystemService(AlarmManager::class.java).cancel(alarme(c))
        terminer(c)
    }

    /** Oublie le minuteur (l'alarme a sonné ou a été annulée). */
    fun terminer(c: Context) {
        prefs(c).edit().remove("fin").apply()
        c.getSystemService(NotificationManager::class.java).cancel(NOTIF)
        rafraichirTuile(c)
    }

    private fun programmer(c: Context, fin: Long) {
        val am = c.getSystemService(AlarmManager::class.java)
        val pi = alarme(c)
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fin, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fin, pi)
        }
        notifier(c, fin)
        rafraichirTuile(c)
    }

    private fun alarme(c: Context) = diffusion(c, FinReceiver.FIN, 0)

    private fun diffusion(c: Context, action: String, code: Int): PendingIntent =
        PendingIntent.getBroadcast(
            c, code,
            Intent(c, FinReceiver::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun notifier(c: Context, fin: Long) {
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CANAL, "Minuteur", NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
            }
        )
        val ouvrir = PendingIntent.getActivity(
            c, 0,
            Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = Notification.Builder(c, CANAL)
            .setSmallIcon(R.drawable.ic_lune)
            .setContentTitle("Mise en veille à ${heure(fin)}")
            .setContentText(
                if (AdminVeille.active(c)) "Pause, volume coupé puis écran verrouillé"
                else "Pause et volume coupé (verrouillage non activé)"
            )
            .setWhen(fin)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_STOPWATCH)
            .setContentIntent(ouvrir)
            .addAction(Notification.Action.Builder(null, "+15 min", diffusion(c, FinReceiver.PLUS, 1)).build())
            .addAction(Notification.Action.Builder(null, "Annuler", diffusion(c, FinReceiver.ANNULER, 2)).build())
            .build()
        try {
            nm.notify(NOTIF, notif)
        } catch (_: SecurityException) {
            // Notifications refusées : le minuteur fonctionne quand même.
        }
    }

    fun heure(ms: Long): String =
        java.text.SimpleDateFormat("HH:mm", java.util.Locale.FRANCE).format(java.util.Date(ms))

    private fun rafraichirTuile(c: Context) {
        TileService.requestListeningState(c, ComponentName(c, TuileMinuteur::class.java))
    }
}
