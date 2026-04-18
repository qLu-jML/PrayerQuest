# PrayerQuest — Upload Signing Setup

One-time guide for generating the Play Store upload key and wiring it into
Gradle. After this is done, `./gradlew :app:bundleRelease` produces a signed
`.aab` that Play Console will accept. **Lose this keystore and you lose the
ability to publish updates to the same app ID**, so treat it carefully:

- Never commit the `.jks` file.
- Never paste its password into a shared doc.
- Keep at least two backups (encrypted external drive, password manager).

## 1. Generate the upload keystore

Run this once, in a directory **outside the repo** (e.g. `~/keys/`):

```powershell
keytool -genkey -v `
    -keystore upload.jks `
    -keyalg RSA `
    -keysize 2048 `
    -validity 10000 `
    -alias prayerquest-upload
```

`keytool` will prompt for:

- **Keystore password** — pick a strong one; you'll need it for every
  release build.
- **Key password** — recommend the same value as the keystore password, to
  keep things simple (Play Console doesn't mind).
- **Distinguished name** fields (CN, OU, O, L, ST, C) — use real values
  (name, org, city, state, two-letter country). These end up in the cert
  and are visible to Play's internal tooling.

Rename / move the resulting file as desired; the path goes into Gradle in
the next step. A conventional location is `~/keys/prayerquest-upload.jks`.

## 2. Add secrets to `~/.gradle/gradle.properties`

The project's `app/build.gradle.kts` reads signing creds from your **user-
level** Gradle properties — outside the repo, never committed. Open (or
create) `~/.gradle/gradle.properties` (Windows:
`%USERPROFILE%\.gradle\gradle.properties`) and paste the block from
`gradle.properties.template` at the repo root, filling in real values.

Minimum for signing to work:

```properties
uploadKeystorePath=~/keys/prayerquest-upload.jks
uploadKeystorePassword=<the password you chose above>
uploadKeyAlias=prayerquest-upload
uploadKeyPassword=<the key password — usually same as keystore password>
```

The same file is where you'll paste real AdMob unit IDs (see the template).

## 3. Verify the signing config

From the repo root:

```powershell
./gradlew :app:signingReport
```

Look for `Variant: release` in the output and confirm:

- `Store: <your keystore path>`
- `Alias: prayerquest-upload`
- `Valid until: <10000 days from today>`

If Gradle prints `⚠️  PrayerQuest release signing: missing …`, one of the
four properties above is blank or absent — fix `~/.gradle/gradle.properties`
and re-run.

## 4. Build the release bundle

```powershell
./gradlew :app:bundleRelease
```

Output lands at `app/build/outputs/bundle/release/app-release.aab`. Upload
that file to Play Console → Production → Create new release.

## 5. Backup the keystore

Right now. Before you forget.

- Copy the `.jks` file to an encrypted external drive.
- Store the password in your password manager (1Password, Bitwarden, etc.).
- Optional but recommended: enroll this app in Play App Signing so Google
  holds the production signing key. Even with that enrolled, you still
  need the upload key for every release — losing it means you have to
  ask Google to reset it and they're picky about proof-of-ownership.

## Troubleshooting

- **"Keystore was tampered with, or password was incorrect."** → Wrong
  `uploadKeystorePassword`. Re-check `~/.gradle/gradle.properties`.
- **"Failed to read key … from store: Given final block not properly padded."**
  → `uploadKeyPassword` is wrong. Same fix.
- **"Could not resolve all files for configuration"** before getting to the
  sign step → Nothing to do with signing; you've got a dependency fetch
  issue. Fix that first.
- **`./gradlew :app:bundleRelease` prints the signing warning but succeeds
  anyway** → That means Gradle fell back to no signing config, and your
  `.aab` is unsigned. Play Console will reject it. Fix the missing
  properties before re-running.
