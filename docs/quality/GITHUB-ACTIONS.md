# GitHub Actions — CalcMot

The repository now has a safe **CI** (not an automatic production release).
- **Android CI** triggers on pull requests targeting `main`, pushes to `main` and the Growth feature branch, and manual dispatch. It provisions JDK 21, Android SDK 36, Gradle, executes `:app:testDebugUnitTest :app:assembleDebug`, and uploads the Debug APK and test reports as artifacts.
- **Android emulator E2E** is manually triggered to run `:app:connectedDebugAndroidTest` on an Android 35 Google APIs emulator and upload reports. Physical Samsung validation remains necessary for camera/audio, real Uber/99 offers and Play review dialogs.
- No keystore, Play Console key or signing secrets are needed to execute either workflow. Neither workflow deploys to Google Play.
- The repository's existing `app/google-services.json` is already tracked; do not print its contents in logs. Review its client identifiers and Firebase configuration in Google Cloud Console.

## How to use GitHub
1. Visit `https://github.com/DUGELO/CalcMot/actions` and open **Android CI**. If GitHub displays a banner saying workflows are disabled, enable them in the repository's Actions settings. In **Settings > Actions > General**, allow the selected GitHub/marketplace actions according to your repository policy and grant only **Read repository contents**.
2. Open PR #2, tab **Checks**. Each push or pull-request update targeting `main` should run Android CI. Green means build and unit tests have passed; red means inspect the failed step logs.
3. In **Actions > Android emulator E2E > Run workflow**, select `feat/growth-organico-v1` and run it. The first emulator download is slow. The absence of a hardware camera or installed driver apps means some tests may require separate physical Samsung runs.
4. From the successful run, download `calcmot-debug-apks` and/or the test report artifacts.
5. Before merging, run the Samsung device tests and manually verify Uber/99 capture, camera/audio recording, sharing, and Play Store opening.
6. Optional: once both workflows have produced passing runs, set a branch rule on `main` in **Settings > Branches** (or Rulesets), requiring **Unit tests and debug build**. Do not require the manual emulator workflow as a mandatory check unless you also arrange for it to run automatically.

## Release/CD
A safe deployment workflow needs separately designed release signing, Play Console service-account authorization, versioning and approval gates. **Not configured intentionally**. Do not store keystores, service-account JSON or passwords in the repository.

## Local commands
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
git pull --ff-only origin feat/growth-organico-v1
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon
```
