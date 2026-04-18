# PrayerQuest Play Store Launch Checklist

Everything that needs to be true before pushing the "Release to Production"
button in Play Console. Same structure as ScriptureQuest's launch flow so the
runbook is predictable.

## 1. Code & Build

- [ ] `versionCode` bumped from previous release in `app/build.gradle.kts`
- [ ] `versionName` follows semver (e.g., "1.0.0" for first release)
- [ ] `minSdk` = 26, `targetSdk` = current Play requirement (34+ as of 2026)
- [ ] Release build flavour uses **real** AdMob unit IDs — not the Google
      test IDs used in debug
- [ ] Release build flavour uses real Play Billing product ID
      (`prayerquest_premium_monthly` or whatever was configured in the
      Play Console → Monetize → Products screen)
- [ ] ProGuard / R8 minification on for release: `isMinifyEnabled = true`
- [ ] `signingConfigs.release` points at the upload key (NOT debug.keystore);
      credentials kept out of source control
- [ ] `./gradlew :app:bundleRelease` produces an `.aab` cleanly with no
      deprecation warnings
- [ ] `.aab` uploaded to Play Console → Production → "Create new release"

## 2. Play Console listing

- [ ] **Title** (from `listing/en-US/title.txt`) — ≤ 30 chars
- [ ] **Short description** (from `listing/en-US/short_description.txt`) — ≤ 80 chars
- [ ] **Full description** (from `listing/en-US/full_description.txt`) — ≤ 4000 chars
- [ ] **App icon** — 512 × 512 PNG (`store/app_icon_512.png`)
- [ ] **Feature graphic** — 1024 × 500 PNG per `SCREENSHOTS_PLAN.md`
- [ ] **Phone screenshots** — 8 per `SCREENSHOTS_PLAN.md`
- [ ] **Tablet screenshots** — 2 minimum if targeting tablets (optional at launch)
- [ ] **Category** — "Lifestyle" (primary), "Education" (secondary)
- [ ] **Tags** — prayer, faith, christian, gratitude, habit, meditation
- [ ] **Contact email** — nathan@5catsgamestudios.com
- [ ] **Privacy policy URL** — hosted copy of `privacy-policy.html`
      (e.g., https://prayerquest.app/privacy)

## 3. Data Safety

- [ ] Complete the Data Safety form using `DATA_SAFETY_GUIDE.md`
- [ ] Confirm "Does your app collect or share user data?" = **Yes**
- [ ] Device IDs (AdMob), Purchase history, App interactions — all disclosed
- [ ] Audio / photos — both marked **Not collected** (on-device only)

## 4. Content rating

- [ ] Complete the IARC questionnaire in Play Console
- [ ] Expected rating: **Everyone**
- [ ] No violence, gambling, or user-generated communication between strangers
      (prayer groups are invite-only + offline MVP)

## 5. Monetization

- [ ] In **Play Console → Monetize → Products → Subscriptions**, create
      `prayerquest_premium_monthly` at **$4.99 USD / month** with a
      7-day free trial (optional but recommended)
- [ ] Subscription product ID string matches the constant in
      `PremiumManager.kt` exactly
- [ ] Tax category set correctly for the account's country
- [ ] Auto-renew terms included in subscription description

## 6. Ads

- [ ] AdMob account linked to the app in Play Console
- [ ] Real ad unit IDs configured in `app/build.gradle.kts` BuildConfig
      fields — replace the Google test IDs
- [ ] AdMob app ID added to `AndroidManifest.xml`
      (`com.google.android.gms.ads.APPLICATION_ID` meta-data)
- [ ] Ads disabled entirely for premium users (verified in `AdManager`)
- [ ] Consent / UMP SDK configured if targeting EEA / UK users

## 7. Testing

- [ ] Internal Testing track used first; at least 3 real devices
- [ ] Subscription purchase tested with a Play Console license-tested account
- [ ] Restore purchase tested after reinstall
- [ ] Banner, interstitial, and rewarded ad loads observed on a test device
- [ ] App survives orientation change, background/foreground, and app-kill
      during a prayer session
- [ ] Offline behavior verified (airplane mode: all core flows still work)

## 8. Policy-sensitive items

- [ ] **Subscription pricing clearly shown** on paywall BEFORE "Subscribe"
      CTA (Play policy requirement)
- [ ] **Cancel-anytime language** visible on paywall
- [ ] App description does NOT make health / medical claims about prayer
- [ ] App description does NOT misrepresent faith affiliation — the copy is
      inclusive of any prayer tradition
- [ ] All ads disabled on any screen below the age gate (not applicable;
      we don't target children)

## 9. Post-launch Day-0 monitoring

- [ ] Watch Play Console crash/ANR dashboard for first 48 hours
- [ ] Monitor AdMob dashboard for fill rate & eCPM
- [ ] Monitor Play Console subscription dashboard for first purchase /
      cancellation
- [ ] Respond to every 1★ review within 24 hours with an honest, warm reply
