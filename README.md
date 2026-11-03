# Trafix

Személyes Android app: figyelmeztet, ha fix traffipaxhoz közeledsz (5 km, majd 1 km),
és naponta egyszer frissíti a listát az autopalyamatrica.hu oldalról.

## Futtatás
1. Android Studio (Koala vagy újabb) -> File > Open -> ez a mappa. Várd meg a Gradle szinkront.
2. Telefonon: Fejlesztői beállítások > USB-hibakeresés, kábellel csatlakoztasd, Run (zöld nyíl).
   Vagy: Build > Build APK(s), és az app-debug.apk-t telepítsd a telefonra.
3. Első indításkor internet kell: az app letölti a listát. Utána "Figyelés indítása".
4. Telefon akkukímélő beállításában az appot vedd ki a korlátozásból, különben leállhat a háttérben.

## Hangolható értékek: AlertService.kt, companion object
FAR_M, NEAR_M, MAX_ANGLE, MIN_SPEED, MIN_SPEED_FAR

## Fordítás GitHubon (Android Studio nélkül)
A .github/workflows/build.yml minden push után (vagy az Actions fülön kézzel indítva)
lefordítja az appot, és az eredmény az "Trafix-debug-apk" artifactban tölthető le.
