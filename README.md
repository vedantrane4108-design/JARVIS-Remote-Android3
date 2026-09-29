# JARVIS Remote Android App

Futuristic blue Android remote for the JARVIS `jarvis_remote` plugin.

## Features
- PIN pairing with the PC
- Voice commands using the phone microphone
- Wake JARVIS
- Quick light controls
- Sleep PC
- Open YouTube
- Stop
- Typed commands
- Remembers the paired PC

## Build the APK

### GitHub Actions (recommended)
1. Create a GitHub repository.
2. Upload this entire `JARVIS-Remote` folder.
3. Open **Actions** → **Build JARVIS Remote APK** → **Run workflow**.
4. Open the completed workflow run.
5. Download the artifact named **jarvis-remote-apk**.
6. Inside it is `app-debug.apk`.

### Local Android Studio
Open this folder as an Android Studio project and build the debug APK.

## PC setup
The PC must run the `jarvis_remote` plugin. The phone and PC need to be on the same local network/hotspot. The plugin listens on port 8000 by default.
