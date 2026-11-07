package io.github.nico7an.veille

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Palette HyperOS : fond gris très clair / noir, cartes blanches / gris foncé, bleu Xiaomi. */
private class Couleurs(sombre: Boolean) {
    val fond = if (sombre) Color(0xFF000000) else Color(0xFFF7F7F7)
    val carte = if (sombre) Color(0xFF1C1C1C) else Color(0xFFFFFFFF)
    val texte = if (sombre) Color(0xFFF2F2F2) else Color(0xFF1A1A1A)
    val secondaire = if (sombre) Color(0xFF8C8C8C) else Color(0xFF8A8A8A)
    val gris = if (sombre) Color(0xFF2C2C2C) else Color(0xFFF0F0F0)
    val piste = if (sombre) Color(0xFF2A2A2A) else Color(0xFFEDEDED)
    val bleu = Color(0xFF3482FF)
    val bleuPale = if (sombre) Color(0xFF14284A) else Color(0xFFE5EFFF)
    val orange = Color(0xFFFF8A00)
}

private val PRESETS = listOf(15, 30, 45, 60, 90)
private val ARRONDI = RoundedCornerShape(24.dp)

class MainActivity : ComponentActivity() {
    /** Incrémenté à chaque retour sur l'appli pour relire l'état (minuteur, administrateur). */
    private var retour by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { Ecran(retour) }
    }

    override fun onResume() {
        super.onResume()
        retour++
    }

    private fun activerAdmin() {
        val demande = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, AdminVeille.composant(this))
            .putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Sert uniquement à verrouiller l'écran quand le minuteur arrive à zéro.",
            )
        try {
            startActivity(demande)
        } catch (_: ActivityNotFoundException) {
            try {
                startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                Toast.makeText(this, "Activer Veille dans Administrateurs de l'appareil", Toast.LENGTH_LONG).show()
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(this, "Écran d'activation introuvable sur cet appareil", Toast.LENGTH_LONG).show()
            }
        }
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
        val k = Couleurs(isSystemInDarkTheme())
        var fin by remember { mutableLongStateOf(Minuteur.fin(c)) }
        var duree by remember { mutableIntStateOf(Minuteur.duree(c)) }
        var maintenant by remember { mutableLongStateOf(System.currentTimeMillis()) }
        var admin by remember { mutableStateOf(AdminVeille.active(c)) }

        LaunchedEffect(retour) {
            fin = Minuteur.fin(c)
            admin = AdminVeille.active(c)
        }
        LaunchedEffect(fin) {
            while (true) {
                maintenant = System.currentTimeMillis()
                if (fin != 0L && maintenant >= fin) fin = 0L
                delay(250)
            }
        }

        val minuteur: @Composable ColumnScope.() -> Unit = {
            if (fin != 0L) {
                val reste = ((fin - maintenant).coerceAtLeast(0) + 999) / 1000
                val total = (fin - Minuteur.debut(c)).coerceAtLeast(1)
                Anneau(k, (fin - maintenant).toFloat() / total) {
                    Text(
                        if (reste >= 3600) "%d:%02d:%02d".format(reste / 3600, reste / 60 % 60, reste % 60)
                        else "%02d:%02d".format(reste / 60, reste % 60),
                        color = k.texte, fontSize = 64.sp, fontWeight = FontWeight.Light,
                    )
                    Text("Mise en veille à ${Minuteur.heure(fin)}", color = k.secondaire, fontSize = 16.sp)
                }
                Spacer(Modifier.height(28.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Pilule("+15 min", k.gris, k.texte, Modifier.weight(1f)) {
                        Minuteur.prolonger(c, 15); fin = Minuteur.fin(c)
                    }
                    Pilule("Annuler", k.gris, k.orange, Modifier.weight(1f)) {
                        Minuteur.annuler(c); fin = 0L
                    }
                }
            } else {
                Anneau(k, 1f, discret = true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Rond("−", k) { duree = (duree - 5).coerceAtLeast(5) }
                        Column(Modifier.widthIn(min = 150.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$duree", color = k.texte, fontSize = 80.sp, fontWeight = FontWeight.Light)
                            Text("minutes", color = k.secondaire, fontSize = 16.sp)
                        }
                        Rond("+", k) { duree = (duree + 5).coerceAtMost(600) }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PRESETS.forEach { m ->
                        val choisi = m == duree
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .background(if (choisi) k.bleuPale else k.gris)
                                .clickable { duree = m }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "$m min", color = if (choisi) k.bleu else k.texte, fontSize = 15.sp,
                                fontWeight = if (choisi) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Pilule("Démarrer", k.bleu, Color.White, Modifier.fillMaxWidth()) {
                    demanderNotifications()
                    Minuteur.demarrer(c, duree)
                    fin = Minuteur.fin(c)
                }
            }
        }

        val reglages: @Composable ColumnScope.() -> Unit = {
            Titre("À la fin du minuteur", k)
            Carte(k) {
                Etape(k, "1", "Pause", "Ce qui est en lecture s'arrête")
                Etape(k, "2", "Volume coupé", "Le volume multimédia passe à zéro")
                Etape(
                    k, "3", "Mise en veille",
                    if (admin) "L'écran se verrouille" else "Administrateur d'appareil à activer",
                    alerte = !admin,
                )
            }
            if (!admin) {
                Spacer(Modifier.height(20.dp))
                Carte(k) {
                    Column(
                        Modifier
                            .clickable { activerAdmin() }
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Activer la mise en veille", color = k.bleu, fontSize = 17.sp, modifier = Modifier.weight(1f))
                            Text("›", color = k.secondaire, fontSize = 22.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Sert uniquement à verrouiller l'écran, comme le bouton marche/arrêt.",
                            color = k.secondaire, fontSize = 13.sp, lineHeight = 18.sp,
                        )
                    }
                }
            }
        }

        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(k.fond)
                .safeDrawingPadding()
        ) {
            val large = maxWidth > 760.dp
            Column(Modifier.fillMaxSize().padding(horizontal = if (large) 48.dp else 20.dp)) {
                Spacer(Modifier.height(if (large) 28.dp else 16.dp))
                Text("Veille", color = k.texte, fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    if (fin != 0L) "Minuteur en cours" else "Minuteur de mise en veille",
                    color = k.secondaire, fontSize = 15.sp,
                )
                Spacer(Modifier.height(24.dp))
                if (large) {
                    Row(Modifier.fillMaxSize().padding(bottom = 32.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                        Column(
                            Modifier
                                .weight(1.15f)
                                .fillMaxHeight()
                                .clip(ARRONDI)
                                .background(k.carte)
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            content = minuteur,
                        )
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), content = reglages)
                    }
                } else {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                        Column(
                            Modifier.clip(ARRONDI).background(k.carte).padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            content = minuteur,
                        )
                        Spacer(Modifier.height(24.dp))
                        reglages()
                    }
                }
            }
        }
    }
}

/** Anneau de progression façon Horloge HyperOS. */
@Composable
private fun Anneau(k: Couleurs, progression: Float, discret: Boolean = false, contenu: @Composable ColumnScope.() -> Unit) {
    val p by animateFloatAsState(progression.coerceIn(0f, 1f), label = "anneau")
    Box(Modifier.widthIn(max = 380.dp).fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(8.dp)) {
            val epaisseur = 10.dp.toPx()
            val cote = size.minDimension - epaisseur
            val origine = Offset((size.width - cote) / 2, (size.height - cote) / 2)
            drawArc(k.piste, 0f, 360f, false, origine, Size(cote, cote), style = Stroke(epaisseur))
            if (!discret) {
                drawArc(k.bleu, -90f, 360f * p, false, origine, Size(cote, cote), style = Stroke(epaisseur, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, content = contenu)
    }
}

@Composable
private fun Pilule(texte: String, fond: Color, couleur: Color, modifier: Modifier, action: () -> Unit) {
    Box(
        modifier
            .clip(CircleShape)
            .background(fond)
            .clickable(onClick = action)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(texte, color = couleur, fontSize = 17.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun Rond(texte: String, k: Couleurs, action: () -> Unit) {
    Box(
        Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(k.gris)
            .clickable(onClick = action),
        contentAlignment = Alignment.Center,
    ) {
        Text(texte, color = k.texte, fontSize = 24.sp)
    }
}

@Composable
private fun Titre(texte: String, k: Couleurs) {
    Text(texte, color = k.secondaire, fontSize = 13.sp, modifier = Modifier.padding(start = 20.dp, bottom = 8.dp))
}

@Composable
private fun Carte(k: Couleurs, contenu: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(ARRONDI).background(k.carte).padding(vertical = 4.dp), content = contenu)
}

@Composable
private fun Etape(k: Couleurs, numero: String, titre: String, detail: String, alerte: Boolean = false) {
    Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(32.dp).clip(CircleShape).background(if (alerte) k.gris else k.bleuPale),
            contentAlignment = Alignment.Center,
        ) {
            Text(numero, color = if (alerte) k.orange else k.bleu, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(Modifier.padding(start = 14.dp)) {
            Text(titre, color = k.texte, fontSize = 17.sp)
            Text(detail, color = if (alerte) k.orange else k.secondaire, fontSize = 13.sp)
        }
    }
}
