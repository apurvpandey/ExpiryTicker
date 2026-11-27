# Releasing ExpiryTicker to Google Play

Everything needed for the first submission of version 1.1.0 (version code 2) is here.

## What is in this folder

| File | What it is |
|---|---|
| `listing-en.md`, `listing-hi.md` | Title, short and full description, release notes (within Play's limits) |
| `data-safety.md` | Answers for the Data safety form |
| `content-rating.md` | Answers for the IARC content rating questionnaire |
| `app-content.md` | The other App content declarations and permission notes |
| `assets/icon-512.png` | Store icon (512 x 512) |
| `assets/feature-graphic-en.png`, `-hi.png` | Feature graphics (1024 x 500) |
| `assets/en/`, `assets/hi/` | Phone screenshots (1080 x 2160; English 6, Hindi 3) |
| `../privacy-policy.md`, `../privacy-policy.html` | Privacy policy, ready to host |

## Before you submit: things only you can do

1. **Fill in the placeholders** in `docs/privacy-policy.md` and `docs/privacy-policy.html`:
   `[YOUR NAME OR COMPANY]` and `[YOUR EMAIL ADDRESS]` (each appears more than once). Use an
   address you read.
2. **Host the policy** at a public URL that stays up. The simplest route is GitHub Pages: in the
   repository settings choose "Deploy from a branch", branch `main`, folder `/docs`. The page then
   lives at `https://<your-user>.github.io/<repo>/privacy-policy.html` (a private repository needs
   a paid plan for Pages; any static host works).
3. **Put that URL in the app:** set `privacy_policy_url` in `app/src/main/res/values/config.xml`,
   then rebuild. The Settings > About > Privacy policy row stays hidden until it is set.
4. **Have the Hindi text reviewed** by a native speaker (the app strings in
   `values-hi/strings.xml` and `listing-hi.md`). Drop the Hindi listing if you prefer not to
   publish it yet; the app still works in Hindi.
5. **Test on a real phone**, ideally a mid-range one (see "Not verified" below).

## Build the release

```
./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab` (about 8 MB). Gradle reads the signing
details from your git-ignored local properties file; they are never committed. Use Play App
Signing (the default for new apps) and keep a backup of your upload keystore: losing it means a
support request to reset the upload key. The R8 mapping file travels inside the bundle, so crash
stack traces in the Console are readable.

The next release must have a higher `versionCode` than 2 (in `app/build.gradle.kts`).

## Play Console steps

1. **Create app** (free, app not game), default language English (United States) or English (India).
2. **Store listing:** paste `listing-en.md`; upload the icon, the English feature graphic and the
   screenshots from `assets/en/` (phone). Category **Productivity**. Add contact email and the
   privacy policy URL. Add Hindi under Manage translations and upload `assets/hi/` plus the Hindi
   feature graphic.
3. **App content:** work through `app-content.md`, `data-safety.md` and `content-rating.md`.
4. **Countries and pricing:** free; choose countries.
5. **Release:** upload the `.aab` to Internal testing first, install from the Play link on a real
   device, then promote.
6. **New personal developer accounts** must run a closed test with a minimum number of testers
   for a minimum number of days before production access is granted (at the time of writing
   12 testers for 14 days). Check the current requirement in Play Console > Dashboard.
7. After the first review, check the **pre-launch report** (it runs the app on real devices and
   reports crashes and accessibility issues), then watch **Android vitals** for crashes and ANRs.

## Upgrading existing users

Version 1.1.0 changes the database from version 2 to 3 with an additive migration (new columns and
two new tables). It was verified by installing this build over a real version 2 database on an
emulator, and the exported schema is in `app/schemas/`. Before the first public rollout, also
install the **currently published** build on a device, add a few items, and update to the release
build to confirm the same on your real data. Use a staged rollout (for example 10%) so a problem
reaches few people.

## Not verified (be honest in your own testing)

- Never run on a physical phone: all testing was on one emulator. Cold start there was 6 seconds
  on an idle machine and 10 to 19 seconds when the host was busy, with "not responding" dialogs
  while the host was overloaded. That points at the emulator, but it is not proof. Measure cold
  start on a real mid-range device with the release build.
- The "Take photo" camera flow, a real fingerprint or face unlock (only the device PIN path was
  tried), and Google's auto-backup restore were not exercised.
- The widget was checked in light mode only; right-to-left layouts were not checked.
- No Compose UI tests and no TalkBack audit; the lint warnings were not reviewed (no lint errors).
- No Baseline Profile, which would shorten start-up on real devices.

## Re-creating the screenshots

Debug builds include a test receiver that fills the app with a realistic demo set
(`adb shell am broadcast -n com.apurvpandey.expiryticker/.DebugSeedReceiver -a com.apurvpandey.expiryticker.SEED`).
It does not exist in release builds. Play requires screenshots no taller than twice their width,
so the ones here were scaled and padded to 1080 x 2160.
