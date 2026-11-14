# helios-android

Android app for Helios: Trusted Web Activity shell plus a home screen widget.

License: AGPL-3.0, the same as Helios.

Android app for [Helios](https://github.com/gnacho/helios), the solar dashboard.

It does two things and nothing else:

1. Opens the Helios web UI full screen in a **Trusted Web Activity**. No screen is rewritten in Kotlin, so every change deployed to the server reaches the phone with no release here.
2. Shows a **home screen widget** with battery state of charge, current production, consumption and when the data was refreshed.

## Requirements

- A Helios server with an API token. Create it in Helios, Settings, API tokens, with the `read` scope. Only a widget is reading, so `read` is enough.
- For the full screen experience, the server must serve `/.well-known/assetlinks.json` with this app's package name and the release certificate fingerprint (see below). Without it Chrome runs the activity as a normal tab with an address bar.

## Build

```sh
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew :app:assembleDebug          # debug APK
./gradlew :app:assembleRelease        # signed release APK (needs keystore.properties)
./gradlew :app:testDebugUnitTest      # JVM tests
```

`keystore.properties` is not in git:

```properties
storeFile=/path/to/helios-release.jks
storePassword=…
keyAlias=helios
keyPassword=…
```

## Signing and asset links

The release keystore is the permanent identity of the app. If it is lost, updates cannot be published. If it is replaced, the fingerprint in Helios must change too, or every installed app falls back to a browser tab.

Current certificate SHA-256:

```
D7:41:06:B7:84:79:9F:AD:42:0D:63:B0:ED:E1:D9:BC:1D:47:08:3F:20:DA:95:AE:6A:9B:E8:17:3B:70:B2:A1
```

Check a built APK with:

```sh
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

## How the widget gets data

`WorkManager` runs `WidgetRefreshWorker` every 15 minutes, the minimum Android allows, and on demand when you tap the widget. The worker writes a snapshot to `SharedPreferences` and the widget only renders that: a widget must produce its views fast, and a network call inside the render would show up as a blank widget.

Errors are explicit. No configuration, a rejected token, an unreachable server and stale data each get their own message. The widget never renders a plausible zero in place of real data.

## Security notes

- The token is stored in plain `SharedPreferences`. It only has `read` scope, lives on the phone of the account owner and can be revoked from Helios at any moment. If this app is ever published more widely, move it to `EncryptedSharedPreferences` with a Keystore key.
- `allowBackup` is off so the token does not travel to cloud backups.

## Layout

```
app/src/main/java/club/cloudless/helios/
├── config/ConfigStore.kt        server URL, token, last snapshot
├── data/                        DTOs, OkHttp client, snapshot mapper
├── ui/MainActivity.kt           Trusted Web Activity launcher
├── ui/SettingsActivity.kt       server + token form (Compose)
└── widget/                      Glance widget, worker, formatting
```
