# Tile Merge

A 2048-style number puzzle game for Android, built with Kotlin and Jetpack Compose.

- 4×4, 5×5 and 6×6 boards, undo, best score per board, light and dark themes
- Earns money with Google AdMob (banner, interstitial, rewarded undo) and a one-time
  "Premium" purchase (product ID `premium`) that removes ads and unlocks unlimited undo
- Shows Google's consent form (UMP) where the law requires it

## Builds

Every push to `main` runs [the Build workflow](.github/workflows/build.yml):

- **tile-merge-test-apk**: a debug APK with Google's test ads, for installing on a phone
- **tile-merge-release-aab**: the signed bundle to upload to Play Console (only once the
  `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` secrets are set)

Download them from the run's **Artifacts** section in the Actions tab.

## Configuration

Real AdMob IDs and the privacy policy link live in [`gradle.properties`](gradle.properties).
Debug builds always use Google's test ad IDs.

The privacy policy is in [`docs/privacy-policy.html`](docs/privacy-policy.html) and is
served by GitHub Pages from the `docs` folder.

## Code map

- `game/` – board logic (pure Kotlin, unit tested in `app/src/test`)
- `ads/` – consent, banner, interstitial and rewarded ads
- `billing/` – Google Play Billing for the Premium purchase
- `ui/` – Compose screens, board and theme
