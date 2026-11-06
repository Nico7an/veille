package io.github.nico7an.veille

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val Nuit = Color(0xFF14130F)
private val Ardoise = Color(0xFF221F19)
private val Craie = Color(0xFFF2EDE4)
private val Cendre = Color(0xFF8C857A)
private val Sodium = Color(0xFFE8A33D)

private val PRESETS = listOf(15, 30, 45, 60, 90)

class MainActivity : ComponentActivity() {
    /** Incrémenté à chaque retour sur l'appli pour relire l'état (minuteur, accessibilité). */
    private var retour by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { Ecran(retour) }
    }

    override fun onResume() {
        super.onResume()
        Extinction.remettreVolume(this)
        retour++
    }

    private fun demanderNotifications() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    @Composable
    private fun Ecran(retour: Int) {
        val c = this
        var fin by remember { mutableLongStateOf(Minuteur.fin(c)) }
        var duree by remember { mutableIntStateOf(Minuteur.duree(c)) }
        var remettre by remember { mutableStateOf(Minuteur.prefs(c).getBoolean("remettre", true)) }
        var maintenant by remember { mutableLongStateOf(System.currentTimeMillis()) }
        var accessibilite by remember { mutableStateOf(VeilleAccessibilite.active(c)) }

        LaunchedEffect(retour) {
            fin = Minuteur.fin(c)
            accessibilite = VeilleAccessibilite.active(c)
        }
        LaunchedEffect(fin) {
            while (true) {
                maintenant = System.currentTimeMillis()
                if (fin != 0L && maintenant >= fin) fin = 0L
                delay(1000)
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .background(Nuit)
                .safeDrawingPadding()
                .padding(horizontal = 28.dp, vertical = 20.dp)
        ) {
            Text("veille", color = Cendre, fontSize = 15.sp, letterSpacing = 3.sp)
            Spacer(Modifier.weight(1f))

            if (fin != 0L) {
                val reste = ((fin - maintenant).coerceAtLeast(0) + 999) / 1000
                Text(
                    "%d:%02d".format(reste / 60, reste % 60),
                    color = Craie, fontSize = 88.sp, fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Light, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                )
                Text(
                    "extinction à ${Minuteur.heure(fin)}",
                    color = Sodium, fontSize = 17.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(48.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Bouton("+15 min", Modifier.weight(1f), plein = false) {
                        Minuteur.prolonger(c, 15); fin = Minuteur.fin(c)
                    }
                    Bouton("Annuler", Modifier.weight(1f), plein = false) {
                        Minuteur.annuler(c); fin = 0L
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Rond("−") { duree = (duree - 5).coerceAtLeast(5) }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$duree", color = Craie, fontSize = 96.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Light)
                        Text("minutes", color = Cendre, fontSize = 15.sp)
                    }
                    Rond("+") { duree = (duree + 5).coerceAtMost(600) }
                }
                Spacer(Modifier.height(32.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PRESETS.forEach { m ->
                        val choisi = m == duree
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (choisi) Ardoise else Nuit)
                                .border(1.dp, if (choisi) Sodium else Ardoise, RoundedCornerShape(10.dp))
                                .clickable { duree = m }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("$m", color = if (choisi) Sodium else Craie, fontSize = 16.sp)
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
                Bouton("Lancer", Modifier.fillMaxWidth(), plein = true) {
                    demanderNotifications()
                    Minuteur.demarrer(c, duree)
                    fin = Minuteur.fin(c)
                }
            }

            Spacer(Modifier.weight(1f))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Remettre le volume au déverrouillage",
                    color = Craie, fontSize = 15.sp, modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = remettre,
                    onCheckedChange = {
                        remettre = it
                        Minuteur.prefs(c).edit().putBoolean("remettre", it).apply()
                    },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Sodium, checkedThumbColor = Nuit,
                        uncheckedTrackColor = Ardoise, uncheckedThumbColor = Cendre, uncheckedBorderColor = Ardoise,
                    ),
                )
            }

            if (!accessibilite) {
                Spacer(Modifier.height(16.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Ardoise)
                        .clickable { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                        .padding(16.dp)
                ) {
                    Text("Verrouillage de l'écran désactivé", color = Sodium, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Touchez ici, puis Applications installées → Veille → activer. " +
                            "Si l'option est grisée : Infos de l'appli → ⋮ → Autoriser les paramètres restreints.",
                        color = Cendre, fontSize = 13.sp, lineHeight = 18.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun Bouton(texte: String, modifier: Modifier, plein: Boolean, action: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (plein) Sodium else Ardoise)
            .clickable(onClick = action)
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(texte, color = if (plein) Nuit else Craie, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun Rond(texte: String, action: () -> Unit) {
    Box(
        Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .border(1.dp, Ardoise, RoundedCornerShape(28.dp))
            .clickable(onClick = action),
        contentAlignment = Alignment.Center,
    ) {
        Text(texte, color = Craie, fontSize = 26.sp)
    }
}
