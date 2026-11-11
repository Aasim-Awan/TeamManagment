# Free Google Sites privacy page

Publisher: S&Ainnovation. Contact: sainnovation14@gmail.com.

Published policy: https://sites.google.com/view/teamfund-manager-privacy/home

Published October 1, 2026 using Safari. Verified with an unauthenticated HTTP request (200) containing the policy sections and publisher contact. The verified URL is configured in gradle.properties for Settings → Privacy policy and can be used in Play Console.

1. Sign in to https://sites.google.com/new with the account that should own the page.
2. Create a blank site named `TeamFund Manager`.
3. Add a page titled `Privacy Policy`. Paste the text from PRIVACY_POLICY.md into a text box; use heading styles for each section.
4. Publish with an available address, for example `teamfund-manager-privacy` (availability must be checked; this is not a published URL).
5. Set the published site's audience to Public so reviewers can read it without signing in. Check the URL in a signed-out/private browser window.
6. Add the verified complete HTTPS URL to gradle.properties as `privacyPolicyUrl=...`. Rebuild the app. Settings → Privacy policy will offer the public page.
7. Add the same URL to Play Console's privacy-policy field.

Support email is already wired. After the app's store listing is live, add `publicAppUrl=` with the verified store listing URL; Share TeamFund will then include that URL. Until then it shares only the app description.

Build: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`. Release preparation: see RELEASE.md.

Google Sites publishing instructions: https://support.google.com/sites/answer/6372880

Google Play privacy requirements: https://support.google.com/googleplay/android-developer/answer/10144311
