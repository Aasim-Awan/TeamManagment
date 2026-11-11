# TeamFund release preparation

## Positioning

TeamFund is an offline tool for a team manager to keep a roster, fixtures and team finances on one Android device. It records payments; it does not process payments or provide shared online accounts, chat, RSVP or live sync.

Suggested short description: Manage your sports team's players, fixtures, expenses and contributions offline.

Suggested listing description:

Keep your sports team's administration in one place. Maintain a player roster, schedule matches with dates and start times, share fixture details through your preferred messaging app, and record team expenses and player contributions. Split expenses, record settlements, review player balances and export financial records to CSV. TeamFund works locally on your Android device. CSV exports are financial reports, not complete team backups.

## Before uploading

- Confirm application ID, version code and name against any existing Play listing. Never change a published app's signing identity.
- Build with JDK 17. This checkout currently pins a machine-specific JDK path in gradle.properties; configure that path for the release machine.
- Supply KEYSTORE_PATH, STORE_PASSWORD and KEY_PASSWORD securely in the build environment. Default signing alias is `upload`; set KEY_ALIAS if your alias differs. Do not commit passwords or keystores. The owner will create the key separately.
- Run `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` and fix errors.
- Run `./gradlew :app:verifyPlaySigning :app:bundleRelease` with the owner's upload key. Upload the signed AAB from app/build/outputs/bundle/release to internal testing first. Without signing environment variables, bundleRelease creates an unsigned review artifact that cannot be uploaded.
- Confirm target SDK against the current Play Console requirement; this checkout targets API 36.
- Test fresh install, team switching, rotation, roster search, date/time editing, sharing, expense creation, partial settlement, revocation and CSV import on real devices.
- Schema 3→4 migration tests cover both legacy shares and development v3 partial-payment storage. Unsupported schema 1–2 upgrades fail without erasing records; no migration support is claimed for those legacy schemas. Test a real-device upgrade before distribution.
- Public privacy policy is published and linked in Settings: https://sites.google.com/view/teamfund-manager-privacy/home. Recheck access before upload.
- Complete Data safety, content rating, audience, ads and app-access declarations based on the final artifact and actual practices. Account for Android backups and user-directed exports.
- Prepared store icon, feature graphic, actual Compose-rendered screenshots with fictional data, support email and release notes are in store-assets. See store-assets/UPLOAD_CHECKLIST.md. Do not advertise online collaboration, payment processing or scheduled reminders.
- Follow any closed-testing requirement shown for the developer account before production access.

## Publisher and privacy

Publisher: S&Ainnovation. Support: sainnovation14@gmail.com. Full policy source: docs/PRIVACY_POLICY.md. Public policy effective October 1, 2026. Records remain local; Android backups and user-directed sharing are disclosed. Check the final signed artifact against Data safety definitions.

## Research sources

- TeamSnap team management: https://www.teamsnap.com/teams/fall — roster, schedule and communication are core team workflows. This release improves local search and fixture sharing; RSVP and multi-user sync remain future work.
- Google Play target API requirements: https://support.google.com/googleplay/android-developer/answer/11926878
- Google Play Data safety: https://support.google.com/googleplay/android-developer/answer/10787469

Upload remains pending signing verification, device testing and owner-supplied store information. A successful debug build alone is not production approval.
