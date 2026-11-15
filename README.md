# EquiSplit

EquiSplit is an offline Android app for splitting shared bills between friends, couples, and families. Each named payer covers one or more people. Enter each expense under the person who paid it, then see the payments that settle the group.

## Install

1. Open [Releases](https://github.com/MRPRANAVPATEL/EquiSplit/releases) and download **EquiSplit.apk** from a release’s **Assets**.
2. Open the APK on an Android 8.0 or later phone and follow Android’s install prompts.
3. Open **EquiSplit** and create a group. Groups and expenses stay in the app’s local storage.

## Split a group bill

1. Enter the group name and total number of people.
2. Add each person who handles payments. Set the number they cover, including themselves. The counts must add up to the group total.
3. Log each expense with its amount, date, description, and payer. The app shares each expense across the full group.
4. Open **Settle up** for each payer’s balance and the exact transfers. Use **Share settlement** to send the summary.

For example, a trip costs ₹32,000 for eight people. Each person’s share is ₹4,000. Ram covers two people, so his share is ₹8,000. If he paid ₹10,000, he should receive ₹2,000. EquiSplit calculates in whole paise and allocates any rounding difference across the group.

## GitHub Actions

**Build EquiSplit APK** tests the settlement calculations and builds the Android APK on every push to `main». Open the successful run and download **EquiSplit-apk** under **Artifacts**. The first passing build for each version also attaches **EquiSplit.apk** to a preview release.

Preview builds use test signing keys. Android requires a matching signing key for in-place updates. Use a consistent release key before distributing updates to other people. Share any settlement summaries you want to keep before uninstalling a preview.

## Publish a release signed with your own key

Generate an Android app signing keystore with a JDK, or use an existing EquiSplit release key. Keep a private backup of the key and passwords.

```sh
keytool -genkeypair -keystore equisplit-release.jks -storetype JKS \
  -alias equisplit -keyalg RSA -keysize 3072 -validity 10000
base64 < equisplit-release.jks | tr -d '\r\n'
```

In GitHub, open **Settings → Secrets and variables → Actions**. Add these repository secrets. Keep the keystore and its Base64 contents private.

| Secret | Value |
| --- | --- |
| `EQUISPLIT_KEYSTORE` | Single-line Base64 encoding of the keystore |
| `EQUISPLIT_STORE_PASSWORD` | Keystore password |
| `EQUISPLIT_KEY_ALIAS` | The signing key alias, such as `equisplit` |
| `EQUISPLIT_KEY_PASSWORD` | Signing key password |

Commit the version in `gradle.properties` and push a matching tag such as `v1.0.0`. **Publish signed EquiSplit APK** builds, verifies, and attaches the signed APK to the tagged GitHub Release. Increase `equisplitVersionCode` for future releases and continue signing with the same key.

## Build locally

Use Java 17, Gradle 8.7, Android SDK Platform 34, and Build Tools 34.0.0. Set `JAVA_HOME` and `ANDROID_HOME` before running the build.

```sh
gradle testDebugUnitTest assembleDebug --stacktrace
```

The APK is at `app/build/outputs/apk/debug/app-debug.apk`.
