# Veille

Minuteur de sommeil pour Android. Quand il arrive à zéro :

1. **pause** de ce qui est en lecture (touche média + prise du focus audio) ;
2. **volume média coupé** ;
3. **mise en veille** : l'écran est verrouillé, comme avec le bouton marche/arrêt.

Durées rapides (15 à 90 min) ou au pas de 5 min, notification à rebours avec « +15 min » et « Annuler », tuile pour les réglages rapides (lance la dernière durée). Option pour remettre le volume au déverrouillage.

## Installation

Télécharger l'APK de la [dernière release](../../releases/latest) et l'installer.

Pour le verrouillage de l'écran, activer le service d'accessibilité : *Paramètres → Accessibilité → Applications installées → Veille*. Il ne lit pas le contenu de l'écran ; il sert uniquement à verrouiller. Sur Android 13 et plus, si l'option est grisée : *Infos de l'appli → ⋮ → Autoriser les paramètres restreints*, puis recommencer.

Sans ce service, la pause et la coupure du volume fonctionnent quand même.

## Construire

Publier une release `vX.Y.Z` : le workflow `APK` construit l'APK et le joint à la release. Signature via les secrets `VEILLE_KEYSTORE_B64` et `VEILLE_KEYSTORE_MDP` (sinon clé de debug).

En local : `gradle assembleRelease` (JDK 17, SDK Android 35).

## Licence

MIT
