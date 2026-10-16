# Growth QA — Samsung / visual E2E (DEBUG only)

This lab is **not** present in release. It never alters production streak/cooldown counters, never writes fake reviews, and never automatically sends messages.

## Run the interactive visual lab

Windows PowerShell (JDK 17+; Android Studio JBR 21):
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
git pull --ff-only origin feat/growth-organico-v1
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon
.\gradlew.bat :app:installDebug --no-daemon
& $adb shell am start -n br.com.calcmot/.debug.GrowthQaActivity
```

On the Samsung, interact with the **Growth QA** screen:
- Change streak simulation: 0, 2, 5, 10, 30 days.
- Open the **real production Compose sharing bottom sheet** to inspect the approved 3D art, responsive copy, primary CTA and dismiss button. In preview the CTA is simulated, not sent.
- Test actual Android Sharesheet via the *separate* "Testar compartilhador Android real" button (manual user interaction only).
- Open the **clearly labeled informational rating preview**. It is NOT a counterfeit Google Play review prompt. The official In-App Review dialogue is controlled by Play and cannot be forced.
- Open the simulated notification text; post a *real* quiet notification only if already authorized.

## Instrumented visual tests + screenshots
```powershell
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=br.com.calcmot.debug.GrowthVisualE2ETest' --no-daemon
```

Tests persist PNGs in the app's external files dir. After tests, run:
```powershell
& $adb shell ls /sdcard/Android/data/br.com.calcmot/files/growth-qa-screenshots
& $adb pull /sdcard/Android/data/br.com.calcmot/files/growth-qa-screenshots .\growth-qa-screenshots
Start-Process .\growth-qa-screenshots
```
**Note:** Android Gradle Plugin test cleanup can uninstall the debug APK after tests, deleting external app files. If screenshots are missing, install and open the lab, take screenshots using `adb shell screencap -p /sdcard/Download/growth-qa.png` and `adb pull`, or run tests with test uninstall cleanup disabled per installed AGP configuration.

## Coverage and limits
Automated: Compose controls, variable streak counter, mock flows, dismiss/CTA behavior, visual capture. Unit: review cadence, no lifetime limit, sharing eligibility. Not guaranteed by these tests: actual WhatsApp send, Play-delivered In-App Review card, OEM notification style, two real days with Uber/99 captures, or release build. Manual device review remains required.

No review gating by satisfaction or manipulation of Play rating; no in-app custom star-rating dialogue.
