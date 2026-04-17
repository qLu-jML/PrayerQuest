# Google Play Data Safety Form — PrayerQuest

Use this guide when filling out the Data Safety section in Google Play Console.

## Does your app collect or share any of the required user data types?
**Yes**

## Data Collection & Sharing Summary

### Device or other IDs
- **Collected:** Yes — Android Advertising ID (by AdMob)
- **Shared:** Yes — with Google (AdMob)
- **Purpose:** Advertising (AdMob)
- **Optional:** AdMob identifiers are not collected for premium (ad-free) users

### Financial info (Purchase history)
- **Collected:** Yes — subscription status via Google Play Billing
- **Shared:** No (processed by Google Play, not shared with additional parties)
- **Purpose:** App functionality (unlocking premium / ad-free experience, unlimited photos, larger prayer groups)

### App activity (App interactions)
- **Collected:** Yes — ad interactions (impressions, clicks) by AdMob
- **Shared:** Yes — with Google (AdMob)
- **Purpose:** Advertising

### Photos/Videos
- **Collected:** No (photos attached to gratitude entries and answered-prayer testimonies are stored in app-private storage on the user's device only; never uploaded to any server)
- **Shared:** No
- **Purpose:** Personal journaling / answered-prayer testimony
- Note: Photos are never transmitted off the device. They remain accessible only to PrayerQuest until the user deletes them or uninstalls the app.

### Audio files
- **Collected:** No
- Note: Microphone is used for on-device speech-to-text in prayer journal, voice recording, and gratitude entry. Audio is NOT recorded, stored, or transmitted — only the transcribed text is saved locally.

### Personal info (Name, email, etc.)
- **Collected:** No
- Note: Users enter a display name for use in Prayer Groups. This stays on the user's device and is only visible to other members of a group the user has manually shared an invite code with (MVP: offline groups with manual sync; all data stays on-device).

### App info and performance (Diagnostics)
- **Collected:** No (no crash reporting or analytics SDK integrated)

### Location
- **Collected:** No (not directly; AdMob may infer approximate location from IP)
- **Shared:** No

### Messages / In-app prayer requests
- **Collected:** No
- Note: Prayer requests added to Prayer Groups remain on the user's device. The MVP uses offline groups with a share-code invite model; no prayer content is transmitted to Anthropic, PrayerQuest servers, or any third party. (Cloud sync is planned post-MVP and will be disclosed here if/when added.)

## Encryption
- All network communication (AdMob, Google Play Billing) uses HTTPS/TLS encryption
- Local data (prayers, journal, photos) is stored in standard app-private storage on the user's device

## Data deletion
- Users can delete all local data by clearing app storage or uninstalling
- No server-side user accounts exist to delete

## App targets children?
- **No** — app is not directed at children under 13
