# i3TOUCH Kalender

Lokale Android-Kalender-App für große Touchdisplays (Android 5.0+). Zielgerät: i3TOUCH ES86 mit Android 8.0.0. Noch nicht auf diesem Gerät getestet. Monats-, Wochen- und Tagesansicht; Termine mit Farbe, Notiz und Ganztagsschalter; Bearbeiten und Löschen. Die Daten liegen in der privaten SQLite-Datenbank der App. Keine Netzwerkberechtigung, kein Konto. Landschaftsmodus.

## APK erstellen

Android Studio installieren, diesen Ordner als Projekt öffnen und **Build → Build APK(s)** wählen. Alternativ mit installiertem Android SDK und Gradle 8.9: `gradle :app:assembleDebug`. Die Datei liegt danach unter `app/build/outputs/apk/debug/app-debug.apk`. Der enthaltene GitHub-Actions-Workflow baut die APK auch automatisch nach dem Hochladen des Projekts; sie erscheint als Workflow-Artefakt. Die Debug-APK ist nur für den eigenen Test gedacht.

Auf dem i3TOUCH die APK auf einen USB-Stick kopieren, im Dateimanager öffnen und die Installation erlauben. Je nach Geräteverwaltung kann die Installation fremder Apps gesperrt sein. Bei unbekanntem Gerätemodell ist die echte Kompatibilität erst am Display prüfbar.

**Grenzen der ersten Version:** keine Synchronisierung mit iCloud/Google/Outlook, keine Erinnerungsbenachrichtigungen, keine Wiederholungsregeln und kein automatisches Backup. Beim Deinstallieren können lokal gespeicherte Termine verloren gehen.
