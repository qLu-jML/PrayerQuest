# PrayerQuest — Sprint 3 Functional Test Checklist

Use this as a manual smoke-test walkthrough. Each item should work on a fresh install and after normal use. Check items off as you verify them; note anything that behaves unexpectedly.

---

## 1. First Launch & Core Infrastructure

- [ ] App opens to the Home screen without crashing.
- [ ] Famous Prayers are seeded on first launch (Library → Famous Prayers shows 30+ entries).
- [ ] Suggested collection packs appear in the Library (Foundational, Peace, Family, Gratitude, etc.).
- [ ] Bottom navigation bar shows five tabs: **Home · Pray · Library · Profile · Settings**.
- [ ] Tapping each bottom tab switches screens and the selected tab is highlighted.

## 2. Home Dashboard

- [ ] Streak flame displays (0 days on fresh install).
- [ ] Hearts indicator visible.
- [ ] Level card shows current level + XP progress.
- [ ] "Start Prayer" CTA navigates to the Mode Picker.
- [ ] "Log Today's Gratitude" quick button navigates to the gratitude log screen.
- [ ] "Prayer Groups" quick link navigates to the groups list.
- [ ] Daily quests card renders with 3 quest slots.
- [ ] Prayer-minutes strip reflects totals after sessions.

## 3. Pray Tab — Mode Picker ("Netflix of Prayer Modes")

- [ ] Tapping the **Pray** bottom tab goes directly to the Mode Picker (no intermediate screen, no back arrow — it's a root).
- [ ] Reaching the picker from Home's "Start Prayer" button shows a **back arrow** (modal picker behavior).
- [ ] Modes are organized into horizontally-scrolling shelves (Guided, Reflective, Contemplative, Rapid-fire, etc.).
- [ ] Tapping a mode card starts a prayer session in that mode.
- [ ] Back arrow (when present) returns to the origin screen.

## 4. Prayer Session — All 10 Modes

For each mode, start a session from the Pray tab and verify:

- [ ] **Guided ACTS Walkthrough** — steps through Adoration → Confession → Thanksgiving → Supplication.
- [ ] **Voice Record & Reflect** — mic button, live transcription appears, editable before completion.
- [ ] **Prayer Journal** — free-type, voice-to-text button works.
- [ ] **Gratitude Blast** — chip bank, rapid-fire entry, speed bonus tallied.
- [ ] **Intercession Drill** — pulls active list items, 30-sec timers per item.
- [ ] **Scripture Soak** — shows a verse + prayer prompt.
- [ ] **Contemplative Silence** — timer + ambient + "still small voice" prompts.
- [ ] **Flash-Pray Swipe** — swipeable cards, right = prayed, left = skip, counter updates, completion button appears after all cards swiped.
- [ ] **Daily Examen** — Ignatian 5-step review runs to completion.
- [ ] **Breath Prayer** — inhale/exhale phrase cycles and can be finished.

### Session Completion

- [ ] When a mode ends, the app goes directly to the **Session Summary screen** (no leftover blank screen with only a back arrow).
- [ ] Summary shows XP earned, streak info, level-up banner (if any), new achievements (if any).
- [ ] "Done" / dismiss on summary returns to the screen the session started from (Home, Pray tab, or Collection Detail).

## 5. Tradition Toggle (Settings)

- [ ] Settings → Tradition section shows the available options.
- [ ] Changing tradition filters the Mode Picker shelves (modes outside the chosen tradition are hidden or greyed).
- [ ] Setting persists after app restart.

## 6. Library Tab

### Collections

- [ ] Library → Collections tab shows user collections + suggested packs.
- [ ] "New Collection" button opens the Create Collection screen.
- [ ] Creating a collection with name/emoji/description persists and appears in the list.
- [ ] Tapping a collection opens the Collection Detail screen.
- [ ] Collection Detail shows its items list, item count, and description.
- [ ] "Add Items" flow lets you attach existing prayer items to the collection.
- [ ] "Start Praying" from a collection opens the Mode Picker **with collection context**.
- [ ] The resulting session uses the collection's **actual items** — not placeholders. (Look for the "Praying for: …" banner listing the real titles.)
- [ ] Flash-Pray Swipe from a collection shows swipe cards whose titles are the collection items (not generic topics).
- [ ] After finishing a collection session, Back/Done returns to the Collection Detail screen.

### Famous Prayers

- [ ] Library → Famous Prayers shows the catalogue (30+).
- [ ] Search/filter by category or author works.
- [ ] Tapping a famous prayer opens its detail screen with full text, author, source.
- [ ] "I prayed this today" button logs a record and shows confirmation.
- [ ] Usage count increments after logging.

### Answered Prayers

- [ ] Archive view lists prayers marked Answered or Partially Answered.
- [ ] Timeline/chronological ordering by answer date.
- [ ] Tapping an entry opens the Answered Prayer Detail screen with testimony + date + photo/voice (if attached).

## 7. Gratitude

- [ ] Home "Log Gratitude" → Gratitude Log screen.
- [ ] Text field + voice-to-text button both work.
- [ ] Optional photo from camera or gallery attaches successfully.
- [ ] Category chips selectable (Family, Provision, Nature, etc.).
- [ ] Save returns to previous screen; the new entry shows in the catalogue.
- [ ] Gratitude Catalogue shows chronological list of entries.
- [ ] Filter by category / photo-only works.
- [ ] Edit existing entry works.
- [ ] Logging a gratitude awards XP (check the summary / profile stats).

## 8. Prayer Groups

- [ ] Prayer Groups entry point from Home works.
- [ ] Create Group — name/description/emoji, generates an invite code like `PRAY-XXXXXX`.
- [ ] Join Group — entering a valid code joins successfully; invalid code shows error.
- [ ] Group Detail screen lists shared prayer items and members.
- [ ] Adding a prayer item to a group persists and shows in the list.
- [ ] "I prayed for this" one-tap increments the count.
- [ ] Praying a group item via any mode logs it to the group's activity.
- [ ] Leave Group / Remove Member (admin) works.

## 9. Profile

- [ ] Level header + XP progress.
- [ ] Daily goal picker (minutes) saves and persists.
- [ ] Theme toggle (light/dark) applies immediately.
- [ ] Lifetime stats grid: total minutes, sessions, gratitudes, answered prayers, group activity.
- [ ] Achievement grid renders with locked/unlocked states.
- [ ] "Science Behind PrayerQuest" expandable card opens.

## 10. Gamification Hot Path

After each prayer/gratitude/group action, verify:

- [ ] XP total increases on the summary.
- [ ] Streak updates (first session of the day advances streak by 1).
- [ ] Daily quests progress when their criteria are met.
- [ ] Full-quest-set bonus triggers when all 3 daily quests complete.
- [ ] Level-up banner appears when threshold crossed.
- [ ] New achievements unlock at the right milestones (e.g., first session, first gratitude, first group joined).

## 11. Notifications (WorkManager)

- [ ] Settings → Notifications lets you toggle: Daily Prayer Reminder, Streak Alert, Quest Notification, Daily Gratitude Prompt, Group Activity Alerts.
- [ ] Scheduled reminder fires at the chosen time (test with a near-future time).

## 12. Persistence & Offline

- [ ] Airplane mode → all features still work (100% offline).
- [ ] Kill and relaunch the app → all data (prayers, sessions, gratitudes, groups, streak) restored.
- [ ] Prayer items, collections, famous-prayer log, gratitudes all survive restart.

## 13. Recently Fixed Flows (Sprint 3 Hot Fixes)

Special attention — these are the bugs just repaired:

- [ ] **Flash-Pray summary appears** after swiping through all cards (previously a blank screen with just a back arrow).
- [ ] **Collection prayer uses real items** — start from a Collection Detail, verify the "Praying for:" banner lists your collection items and Flash-Pray cards show those titles (not "Global peace", "Healing from pain", etc. placeholders).
- [ ] **Single-mode session → summary directly** — when starting from the Mode Picker, the grade bar is skipped and you go straight to the summary.
- [ ] **Back-nav from summary** lands on the origin screen (Home → Home, Collection → Collection Detail), not back on the Mode Picker.
- [ ] **Pray bottom tab is a root** (no back arrow); **Home → Start Prayer** shows a back arrow on the picker.

## 14. Edge Cases

- [ ] Start a prayer session with **zero active prayer items** — mode should still run with synthetic/default content, no crash.
- [ ] Swipe through Flash-Pray without marking any as prayed → summary still shows (prayed count = 0).
- [ ] Create a collection with no items and try "Start Praying" → gracefully falls back (either informs the user or uses default content).
- [ ] Log gratitude without a photo → saves fine.
- [ ] Very long prayer titles / journal entries don't break the UI.

---

## How to Report Issues

For anything unchecked, capture:
1. Which screen / flow
2. What you did
3. What happened vs. what you expected
4. A screenshot if the UI misbehaves

Then we'll triage into Sprint 4.
