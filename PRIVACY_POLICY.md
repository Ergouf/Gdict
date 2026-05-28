# Gdict Privacy Policy

**Last updated: May 28, 2026**

This privacy policy applies to the Gdict application ("App") for Android and Windows Desktop platforms.

## Data Collection

Gdict does **not** collect, store, or transmit any personal data to our servers. We do not have servers.

Specifically, we do **not** collect:

- Personal identification information (name, email, phone number)
- Device identifiers (IMEI, advertising ID, device serial number)
- Location data
- Usage analytics or telemetry
- Crash reports

## Data Stored Locally

All user data is stored **exclusively on your device**:

- **Dictionary files** — MDX/MDD files you import are copied to the app's private directory
- **Bookmarks** — Words you bookmark are saved in local storage
- **Search history** — Recent searches are stored locally
- **Settings** — App preferences (dark mode, zoom levels, etc.) are saved locally
- **FSRS review data** — Spaced repetition scheduling data is stored locally

You can delete all locally stored data at any time by clearing the app data (Android) or deleting the app data directory (Desktop).

## Third-Party Services

The App interacts with the following third-party services:

### Microsoft Edge TTS

- **Purpose**: Cloud-based text-to-speech for word pronunciation
- **Data sent**: The word text you request pronunciation for is sent to Microsoft's Edge TTS service
- **No personal data is transmitted** — only the word text itself
- **Offline fallback**: If no internet connection is available, the app falls back to MDD audio or local TTS

### Afdian (爱发电)

- **Purpose**: Optional donation/sponsor link
- **Data sent**: Clicking the sponsor button opens the Afdian website in your default browser; no data is sent by the app itself

### JCEF (Java Chromium Embedded Framework)

- **Purpose**: Renders dictionary HTML content locally within the app
- **Network access**: JCEF is configured to block all network requests (`blockNetworkLoads = true`)
- **No data is transmitted** — all rendering is done offline using local dictionary content

## Permissions

### Android

| Permission | Purpose |
|------|------|
| Internet access | Edge TTS pronunciation (cloud service) |
| Read external storage | Import dictionary files from your device |

### Windows Desktop

| Capability | Purpose |
|------|------|
| Internet client | Edge TTS pronunciation (cloud service) |
| Run full trust | Standard desktop application access (JCEF, file system) |

## Children's Privacy

The App is suitable for all ages. We do not knowingly collect personal information from children.

## Changes to This Policy

We may update this privacy policy from time to time. Changes will be reflected in the "Last updated" date above.

## Contact

If you have questions about this privacy policy, please contact us at:

- GitHub: [https://github.com/Ergouf/Gdict](https://github.com/Ergouf/Gdict)
