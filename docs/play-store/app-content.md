# App content declarations

Play Console > Policy > App content. Each item below is a declaration the Console asks for.

| Declaration | Answer | Notes |
|---|---|---|
| Privacy policy | Hosted URL | See `README.md` for hosting |
| Ads | No ads | No ad SDKs in the app |
| App access | All functionality available without sign-in | No special instructions needed |
| Target audience | **18 and over** | Not designed for children, so the Families policy does not apply |
| News app | No | |
| COVID-19 contact tracing / status | No | |
| Data safety | See `data-safety.md` | No data collected or shared |
| Government app | No | |
| Financial features | None | The app records costs the user types in; it offers no payments, loans or banking |
| Health apps | Not a health app | "Health" is only one of the labels for an item (for example a check-up date) |
| Advertising ID | Not used | No `AD_ID` permission in the merged manifest |

## Permissions in the release build

| Permission | Why |
|---|---|
| `POST_NOTIFICATIONS` | Shows the reminders the user sets (asked at runtime on Android 13+) |
| `USE_BIOMETRIC`, `USE_FINGERPRINT` | Optional app lock through Android's system prompt |
| `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK` | Added by WorkManager so scheduled reminders survive a restart |
| `ACCESS_NETWORK_STATE` | Added by WorkManager; not used by the app |

None of these are in Google Play's restricted or sensitive permission groups, so no permission
declaration form is needed. The foreground-service permission and service that WorkManager
normally adds have been removed because the app does not use foreground work.

## Photos and videos / files

The app does **not** request photo or video permissions. "Attach file" uses Android's system file
picker and "Take photo" opens the camera app, so the photo and video permission declaration does
not apply.
