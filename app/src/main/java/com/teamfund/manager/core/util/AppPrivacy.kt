package com.teamfund.manager.core.util

/** Local privacy disclosure. The public policy also needs the publisher's contact details. */
object AppPrivacy {
    val disclosure = """
        TeamFund Manager — Privacy & data
        Updated November 11, 2026
        Publisher: S&Ainnovation
        Privacy and support: sainnovation14@gmail.com

        Information you enter
        TeamFund stores team names, player names and optional phone numbers, match details, expenses, contributions and settlements on your device. This information supports your roster, fixture schedule and financial records.

        Storage and access
        TeamFund works locally and has no online account or team synchronization service. Anyone who can access your unlocked device may be able to view your team records. Protect your device and obtain permission before entering another person's contact details.

        Sharing and exports
        CSV exports and match details are shared only when you choose a receiving app. That app handles the shared content under its own privacy policy. Exports can contain player names and financial information; choose recipients carefully.

        Device backups
        Android may back up and restore app data according to your device and account settings. Manage these backups through Android and your backup provider. CSV exports are reports, not full backups of teams, matches, settlements or split selections.

        Retention and deletion
        Records remain locally until you delete them or use Settings → Reset All Data. Deactivating a player keeps their financial history. Reset All Data deletes this app's local teams and records, but does not delete files already exported, shared copies or device backups. Manage those copies separately in their receiving apps and backup settings.

        Payments
        TeamFund records payments for bookkeeping. It does not transfer money or collect bank-account or card credentials.

        Permissions and external services
        You choose files through Android's file picker. Sharing uses Android's share sheet. TeamFund does not request notification permission and does not send reminders. Opening public policy or support links uses your chosen external app, which may process connection information under its own policy.

        Children and team members
        Team managers are responsible for permission to enter players' details, including permission from a parent or guardian where required. Enter only information needed for team administration.

        Changes
        Review this disclosure and the published privacy policy when updating the app. Contact S&Ainnovation at sainnovation14@gmail.com for privacy questions. Support emails are used to answer your enquiry and retained only as needed to resolve it and meet applicable obligations. Request deletion through the same address.
    """.trimIndent()
}
