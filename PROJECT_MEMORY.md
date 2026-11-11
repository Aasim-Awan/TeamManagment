# TeamFund — Project Memory

## Current state
- Android app (Kotlin, Jetpack Compose, Room v10, DataStore). Package `com.teamfund.manager`. Local-only, no backend.
- Preparing for first Play release (`versionCode = 1`).
- Home top card: balance and "Money In (all time)" are all-time; "Spent this month" = sum of expenses dated in the current calendar month.
- Bottom bar: Home, Players, Matches, Expenses (All expenses screen), Reports. Home no longer links to Players or All expenses.
- **All expenses** screen (`feature/allexpenses/`): Timeline tab (all expenses, contributions, settlements, charges grouped by month then day, with type/month/player filters and a summary) and By player tab (per-player share, paid, unpaid, balance). Also opened from Home "Recent activity → View All".
- Settings: grouped list (team header card with switch/new team; Team: currency, seasons, monthly dues, kit; Appearance: theme segmented control; Data: export, import, reset; About). Seasons and dues open as bottom sheets. Results show as snackbars.
- Store kit (`store-assets/`): 8 screenshots, slot 07 = All expenses. Rebuild: record test, then `scripts/build-store-assets.cjs` (see UPLOAD_CHECKLIST).
- Release build: R8 + resource shrinking on, unsigned APK about 2.0 MB. No Firebase/secrets plugins. No notification permission.

## Change log
- 2026-11-11: Full release review done (findings: crash on match edit, money-input and balance bugs, missing transactions, CSV issues, release config).
- 2026-11-11: Batch A fixes + All Expenses screen.
  - Repository save methods return the real id on edit (Room `@Upsert` returns -1 on update). New `saveMatchWithRoster`, `recordPlayerPayment`. Multi-step writes wrapped in `database.withTransaction`. `deleteFine` also deletes its linked settlement. Match fee / ground fee are not charged twice for one match.
  - `MoneyFormatter.toMinorUnits` accepts comma decimals ("12,50").
  - Balance use case counts share payments only up to the share amount (overpay shows as credit). `decrementSharePayment` clamped at 0.
  - Add Expense blocks over-allocated manual splits. `SaveGuard` (`core/common/SaveGuard.kt`) + `isSaving` disable Save buttons during save (match, expense, contribution, player, settle sheet; Create Team uses local flag).
  - `ActivityRow` moved to `core/designsystem/ActivityRow.kt` (shared); added `ActivityItem.FineActivity`.
  - Tests added: MoneyFormatter comma cases, overpay balance, DAO match edit / duplicate match fee / paid fine delete, `AllExpensesLedgerTest`.

- 2026-11-11: All expenses moved to the bottom bar; Players & Balances and All expenses links removed from Home. Fixed stale `StoreScreenshotTest` text ("6 Team Members" became "6 active").
- 2026-11-11: Batch B. Match delete removes auto match-fee fines and the auto ground-fee expense, unlinks other records (one transaction). Dues: charged once per period, next date +1 calendar month, Charge button disabled until due. CSV export: quotes escaped, formula guard (`'` before = + - @), ISO dates. CSV import: shared `ImportCsvUseCase`, skips duplicates/unknown players/bad rows with a result message, reads ISO and old English dates, expenses imported without splits.
- 2026-11-11: Batch C. R8 + `isShrinkResources` on, ProGuard rules cleaned (enum names kept for Room). Removed `secrets` and `google-services` plugins. `org.gradle.java.home` moved to `~/.gradle/gradle.properties`. Removed `POST_NOTIFICATIONS`, `NotificationHelper` and reminder test code; notifications default off. Explicit backup rules (database + datastore). Privacy text updated (2026-11-11).
- 2026-11-11: Deleted `~/.gradle/caches` (disk was full) with owner approval.
- 2026-11-11: Settings screen redesigned (`SettingsScreen.kt`, same ViewModel); removed reminders card; currency change warns that amounts are not converted. All expenses summary made compact. Store assets re-recorded (5-tab bar, new Settings, All expenses replaces expense-splits shot), listing/checklist/manifest/zip updated. StoreScreenshotTest: data dated around today, season + dues seeded, semantic clicks (no ripple in shots).

## Pending / open decisions
- R8 release build not yet run on a device (needs signed build; installing over an existing debug install needs uninstall = data loss).
- Public privacy policy page (Google Sites) must match the updated in-app text: no notification permission, backup of database and preferences.
- All 2026-11-11 work is uncommitted.
- `unslop` writing plugin not installed: adding its marketplace (`jlwin/unslop_ai`, unreviewed) was blocked by the permission classifier; owner can install via `/plugin`.
- Top-level tabs Players, Matches and Reports still show a back arrow in the top bar.
- Smaller: hardcoded strings (no `strings.xml`), theme default LIGHT, currency change relabels amounts, inventory can go negative, Room schema `3.json` has the same hash as `4.json`.

## Project rules & gotchas
- Ask before any code change, Gradle file change, or Gradle task.
- Room `@Upsert` returns -1 when it updates; never use its return value as the id on edit.
- Player id `0L` = Team Fund (in expense shares and settlement `fromPlayerId`).
- All money is Long minor units (2 decimals); parse with `MoneyFormatter.toMinorUnits`.
- No schema change in 2026-11-11 work; DB still version 10.
- Ground-fee expenses created from a match are found by title prefix `Ground fee — ` (`GROUND_FEE_TITLE_PREFIX`); do not change it without a migration.
- Roborazzi writes store images only with `-Proborazzi.test.record=true`.
- Machine needs `org.gradle.java.home` (JDK 17) in `~/.gradle/gradle.properties`.
