# FightPlan

Android-App, die meinen Papier-Wochenplan fürs Muay-Thai-Training ersetzt: Tagesplan, Essensfenster, Makro-Ziele, Gewichtsverlauf, wöchentlicher Check-in und Erinnerungen. Alles bleibt lokal auf dem Handy, es gibt kein Backend und keinen Login.

## Funktionen

- **Heute:** Tagestyp, Termine, Essensfenster zum Abhaken, Makro-Ziele, Eingabe des Morgengewichts
- **Woche:** Tabelle mit Schule, Training und Essen
- **Gewicht:** Diagramm mit Tageswerten und 7-Tage-Schnitt, Verlust pro Woche, Taillenumfang, Anpassungsvorschläge
- **Check-in:** Schlaf, Stimmung, Libido, Infekt, Kraft, Sprunghöhe; ab 2 Warnzeichen erscheint ein roter Hinweis
- **Mehr:** Start- und Zielgewicht, alle Alarmzeiten (einzeln änderbar und abschaltbar), Berechtigungen, JSON-Export und -Import
- **Widget „FightPlan Heute“ (4×2):** Tagestyp, nächster Termin, nächstes Essen, Makros

Der Plan (Termine, Essen, Makro-Faktoren, Anpassungsregeln) steht in `app/src/main/assets/plan.json`. Wenn sich der Plan ändert, reicht es, diese Datei zu bearbeiten und die App neu zu bauen.

## Rechenlogik

- Makros = Faktor (g/kg) × aktueller 7-Tage-Schnitt; kcal = 4·KH + 4·E + 9·F
- Anpassung: Verglichen wird der 7-Tage-Schnitt von heute mit dem von vor 2 Wochen. Unter 0,3 kg/Woche schlägt die App −150 kcal an Normal- und Ruhetagen vor, über 0,8 kg/Woche +200 kcal an allen Tagen. Jede Änderung muss bestätigt werden. Danach kommt frühestens nach 14 Tagen ein neuer Vorschlag.
- Ist das Zielgewicht erreicht, schlägt die App Erhaltungskalorien vor (+350 kcal)
- Alles ist in `domain/` ohne Android-Abhängigkeiten und durch Unit-Tests abgedeckt

## Build

Voraussetzungen: JDK 17 oder neuer, Android SDK (Platform 36). Entweder liegt das Projekt in Android Studio, oder `local.properties` enthält `sdk.dir=/pfad/zum/sdk`.

```bash
./gradlew assembleDebug
```

Die APK liegt danach unter `app/build/outputs/apk/debug/app-debug.apk`.

Tests (Rechenlogik, Export/Import-Roundtrip, Robolectric-Smoke-Test der App):

```bash
./gradlew testDebugUnitTest
```

## Installation per ADB

1. Am Handy die Entwickleroptionen aktivieren (7× auf „Build-Nummer“ tippen) und USB-Debugging einschalten
2. Das Handy per USB verbinden und die Abfrage am Handy bestätigen
3. Installieren:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Nötige Berechtigungen

Beim ersten Start zeigt „Heute“ einen roten Hinweis. Über **Jetzt einrichten** (oder **Mehr → Berechtigungen → Prüfen**) lässt sich alles freigeben:

| Berechtigung | Warum | Wo |
|---|---|---|
| Benachrichtigungen | Ab Android 13 nötig, sonst erscheinen keine Erinnerungen | Dialog in der App |
| Exakte Alarme („Wecker und Erinnerungen“) | Ab Android 14 standardmäßig aus. Ohne diese Freigabe kann Android Erinnerungen um mehrere Minuten verschieben | Einstellungen → Apps → FightPlan → Wecker und Erinnerungen |
| Akku-Optimierung: „Nicht eingeschränkt“ | Sonst beenden manche Hersteller die App im Hintergrund | Einstellungen → Apps → FightPlan → Akku |

Bei **Xiaomi/HyperOS** muss zusätzlich „Autostart“ erlaubt sein, bei **Samsung** darf FightPlan nicht unter „Apps im Ruhezustand“ stehen. Hinweise für weitere Hersteller gibt es auf <https://dontkillmyapp.com>.

Nach einem Neustart, einem App-Update oder einer Zeitzonenänderung plant die App alle Alarme automatisch neu (`BootReceiver`).

## Datensicherung

**Mehr → Export** schreibt eine JSON-Datei über die Android-Dateiauswahl, zum Beispiel nach Google Drive oder in „Downloads“. **Import** ersetzt alle Daten durch den Inhalt der Datei.
