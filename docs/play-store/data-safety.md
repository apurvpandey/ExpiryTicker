# Data safety form: answers

Play Console > App content > Data safety. These answers describe version 1.1.0 and are based on
the merged release manifest and the code: no internet permission, no analytics, ads, or
crash-reporting libraries, no accounts, no server.

## Data collection and security

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |

Choosing "No" ends the form. The follow-up questions (encryption in transit, deletion requests)
are not asked when nothing is collected or shared.

## Why "No" is accurate

- The app declares **no INTERNET permission** (only `ACCESS_NETWORK_STATE`, added by WorkManager),
  so it cannot transmit anything by itself.
- Everything the user enters lives in the app's private storage on the device.
- There are no third-party SDKs that collect data (no ads, analytics, attribution or crash reporting).
- Features that hand data to other apps (open website, call, add to calendar, open an attachment)
  are user-initiated actions that go through the Android system to the app the user picks.

## Two things to double-check in the Console

1. **Android Auto Backup.** The app allows Android's device backup of its database and settings
   (not attachments). Google's Data safety guidance treats a user's own device backup as not being
   collection by the developer, but read the current wording in the Console help ("Data collection
   and sharing" > backups) before submitting.
2. **Google Fonts.** The Inter typeface may be downloaded by Google Play services, not by the app.
   The privacy policy discloses it. It is not data the app collects.

## If you add any of these later, the answer changes

Crash reporting or analytics (Firebase, Sentry, ...), ads, sign-in, cloud sync, or the internet
permission. Update the policy and this form before releasing that version.

## Privacy policy

Link to the hosted copy of `docs/privacy-policy.html` (see `README.md`). The Settings row that
opens it appears once the address is set in `app/src/main/res/values/config.xml`.
