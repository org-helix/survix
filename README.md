# Survix

Write surveys on an Android phone, block by block, keep them on the phone, and export them to use elsewhere. Free, open source and offline first: Survix has no Internet permission, no account and no tracking.

Part of the [Helix](https://github.com/org-helix) family and built on its design: the Helix palette on the Dusk DS structure, Bricolage Grotesque titles, Geist text, and the pixel-art sunset.

## Features

- **Library**: every survey saved on the phone, newest first, with search once there are a few. Duplicate, delete (with undo), preview, export or share from each card.
- **Blocks**: a survey is a list of modules, added from a sheet and edited in place. Open a block to edit it; move it up or down, duplicate or delete it.

  | Block | What it asks |
  |---|---|
  | Section | Nothing: a title and a text that split the survey into parts. |
  | Free text | A short answer or a paragraph, with an optional hint. |
  | Choice | One or several options, with an optional "Other" field. |
  | Yes / No | A closed question. |
  | Rating | A score from 1 to 2–10, with labels for both ends. |
  | Number | A value, with optional minimum, maximum and unit. |
  | Date | A day on the calendar. |

  Every question has a label, a help text and a "required" switch. The editor flags empty labels, missing or duplicated options and reversed bounds.
- **Saved as you type**: edits are kept in memory at once and written to the phone a moment later, and again when the app goes to the background. Files are written atomically, so a crash never leaves a half-written survey.
- **Preview**: fill in the survey as a respondent would and check required answers and number bounds. Nothing typed there is saved.
- **Export and import**:
  - `.survix.json`: readable, versioned JSON ([format](docs/format.md)), one survey or all of them in one file. Import it back on any phone with **Import a file**, by opening it from a file manager, or by sharing it to Survix. Importing never overwrites an existing survey.
  - `.md`: the survey as Markdown, ready to print or paste into a document.
  - **Share** sends the `.survix.json` file through any app (messaging, email, Bluetooth…).
- **First launch**: choose the language and the look, applied live, and optionally start with an example survey.
- **Languages**: French (default) and English. On Android 13+ the language can also be set in the system settings.
- **Themes**: system, light (ink on Pale Amber) or dark (deepened ink, Pale Amber text).
- **Backup**: surveys are included in the phone's own backup and device-to-device transfer.

Android 7.0 (API 24) or newer.

## Download

Builds come from GitHub Actions: the **CI** workflow attaches a debug APK (`survix-debug.apk`) to every run, and pushing a `v*` tag publishes `survix-android.apk` as a release. Install it after allowing your browser or file manager to install unknown apps.

## Development

Requires JDK 17 and the Android SDK (API 35).

```sh
./gradlew :core:test              # model, file format, store, Markdown, palette contrast, illustration
./gradlew :app:assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:lintDebug
./gradlew installDebug            # on a connected phone or emulator
```

| Module | What it holds |
|---|---|
| `core` | Pure Kotlin, no Android: the survey model and editing operations (`Model.kt`, `Edits.kt`), the file format (`SurvixFormat.kt`), the on-disk store (`SurveyStore.kt`), the Markdown export, the Helix palette (`HelixPalette.kt`) and the pixel sunset. All unit-tested on the JVM. |
| `app` | The Android app in Jetpack Compose: `data/` (repository, settings, file picker and sharing), `ui/theme/` (Helix theme and type scale), `ui/components/` (buttons, cards, fields, the Helix mark), and one package per screen: `welcome`, `library`, `editor`, `preview`, `settings`. |

Strings live in `app/src/main/res/values/strings.xml` (French) and `values-en/strings.xml` (English); add a string to both.

Releases are built by `.github/workflows/release.yml` when a `v*` tag is pushed. To sign with a release key, set the secrets listed at the top of that file; without them the APK is signed with a throwaway debug key.

## Design

Survix follows the Helix design used by [netlix](https://github.com/org-helix/netlix) and the [Helix website](https://github.com/org-helix/helix-website): the Helix colour palette on the Dusk DS structure, Bricolage Grotesque titles with negative tracking, Geist text, a 4 dp grid, radii of 8, 12 and 16, and no drop shadows (cards have a hairline outline instead). Red `#B24E30` is the only action colour. The light theme puts ink `#322921` on Pale Amber `#FAF6ED` with Paper cards; the dark theme is the dark Helix sky (`#1E1914` canvas, Pale Amber text, Blush focus ring).

All colours live in `core/src/main/kotlin/org/helix/survix/core/HelixPalette.kt`, and `HelixPaletteTest` checks that every text and background pair meets WCAG AA in both themes. Fonts are bundled under the SIL Open Font License ([`docs/licenses`](docs/licenses)).
