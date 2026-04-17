# Play Store Screenshot Plan — PrayerQuest

Google Play requires at least 2 phone screenshots (minimum 320px, max 3840px,
16:9 or 9:16 aspect). We'll ship **8 screenshots** for phones — this is the
Play Console maximum and lets every core differentiator get its own frame.

Phone dimensions: **1080 × 2400 px** (9:20, matches most modern Android
phones and is safely within Play's bounds).

## Capture order (appears left-to-right in the listing)

1. **Home Dashboard — "Start Praying Today"**
   - Shot: Home screen with a healthy 12-day streak flame, 3 active daily
     quests, and the big "Start Prayer" CTA.
   - Overlay text (top): "Build a prayer habit that sticks"
   - Why first: Gives the reviewer-scrolling user the instant promise.

2. **8 Prayer Modes — Mode Picker**
   - Shot: Pray tab showing the full grid of 8 prayer modes with their
     icons and names.
   - Overlay text: "Pray your way — 8 beautiful modes"
   - Why: Visual density, shows unique feature vs. generic prayer journals.

3. **Guided ACTS Walkthrough — Mid-session**
   - Shot: ACTS screen on the "Thanksgiving" step with voice-to-text button
     visible and the guidance text on screen.
   - Overlay: "Adoration · Confession · Thanksgiving · Supplication"

4. **Gratitude Log — With Photo**
   - Shot: Gratitude Log screen with 3 entries filled in (one with a sunset
     photo), category chips visible.
   - Overlay: "Log gratitude with photos — see God's faithfulness"

5. **Gratitude Catalogue — Calendar Heatmap**
   - Shot: Gratitude Catalogue with calendar heatmap showing a green-rich
     month.
   - Overlay: "A year of thanks, at a glance"

6. **Answered Prayers — Timeline**
   - Shot: Answered Prayers list with 4-5 answered entries, one showing a
     testimony preview.
   - Overlay: "Archive answered prayers. Never forget."

7. **Prayer Groups — Group Detail**
   - Shot: A group detail screen ("Rodriguez Family" or similar) showing
     shared prayer requests with "Prayed by 4" tags.
   - Overlay: "Pray together — invite-only groups for family & friends"

8. **Profile & Achievements**
   - Shot: Profile screen with level card (Level 7 "Intercessor"), lifetime
     stats grid, and achievement categories with progress bars.
   - Overlay: "Level up. Unlock badges. Keep seeking Him."

## Feature graphic (required, 1024 × 500 px)
- Background: warm gradient (soft blue-purple → gentle gold)
- Foreground: phone mockup displaying the Home screen (screenshot #1)
- Headline: "PrayerQuest"
- Tagline: "Build a daily prayer habit."
- Corner badge: "Free · Ad-supported · Premium $4.99/mo"

## App icon (already exists at `store/app_icon_512.png` once generated)
- 512 × 512 PNG, 32-bit, no transparency
- Design: soft gradient background (blue-purple), hands-in-prayer silhouette,
  or a stylized flame/dove — match the in-app `ic_launcher` foreground so the
  Play Store icon and installed launcher icon feel like the same mark.

## Promo video (optional, YouTube link in Play Console)
- 30 seconds, no voiceover (captioned)
- Beat sheet:
  - 0:00–0:03 "How often do you actually pray?" (black → Home fade-in)
  - 0:03–0:10 Mode picker → ACTS walkthrough → voice-to-text in action
  - 0:10–0:17 Gratitude entry with photo → catalogue heatmap fills in
  - 0:17–0:23 Answered Prayer timeline scrolls, one turns green
  - 0:23–0:27 Prayer Group members pray, counter ticks up
  - 0:27–0:30 Logo + "Start your prayer habit today — free on Google Play"

## Capture checklist before shipping
- [ ] Clear demo account (no test data visible, realistic streak 7-14 days)
- [ ] All screenshots taken on the same device with same status bar (9:41,
      Wi-Fi, 100% battery — clean look)
- [ ] Dark-mode alternate set captured (optional but good for A/B testing)
- [ ] Every screenshot passes Play Store policy (no "review now" prompts
      visible, no external app branding, no placeholder Lorem Ipsum text)
- [ ] File names sorted by prefix (`01_home.png` ... `08_profile.png`) so they
      upload in the intended order
