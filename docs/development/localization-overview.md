# Localization Overview

## Supported Languages

| Language | BCP-47 Tag | Resource Directory | Status |
|---|---|---|---|
| English | `en` | `values/` | ✅ Default — complete |
| বাংলা (Bengali) | `bn` | `values-bn/` | ✅ Foundation strings translated |

---

## Architecture

### Language selection flow

```
User selects language in Settings
        ↓
SettingsViewModel.setLanguage(SupportedLanguage)   [Phase A2]
        ↓
LocaleManager.setLanguage(SupportedLanguage)
        ↓
Persist to DataStore (PREF_KEY_LANGUAGE)
        ↓
AppCompatDelegate.setApplicationLocales(localeList)
        ↓
System triggers Activity recreation
        ↓
App reloads in selected language
```

### On app startup

```
MainActivity.onCreate()
        ↓
LocaleManager.selectedLanguage.collect()           [Phase A2]
        ↓
LocaleManager.applyLocale(language)
        ↓
AppCompatDelegate.setApplicationLocales(localeList)
```

---

## Key Components

### `SupportedLanguage` enum
```kotlin
enum class SupportedLanguage(
    val tag: String,        // BCP-47 language tag
    val displayName: String, // Native display name
    val englishName: String  // English name for accessibility
) {
    SYSTEM(tag = "", displayName = "System Default", ...),
    ENGLISH(tag = "en", displayName = "English", ...),
    BANGLA(tag = "bn", displayName = "বাংলা", ...),
}
```

### `LocaleManager`
- Singleton, injected by Hilt
- Reads/writes language preference via `DataStore<Preferences>`
- Applies locale via `AppCompatDelegate.setApplicationLocales()`
- Compatible with Android 13+ per-app locale and older devices

### DataStore key
```kotlin
AppConstants.PREF_KEY_LANGUAGE = "app_language"
```

---

## Adding a New Language

To add a new language (e.g., Arabic):

1. **Add to `SupportedLanguage` enum:**
   ```kotlin
   ARABIC(
       tag = "ar",
       displayName = "العربية",
       englishName = "Arabic",
   )
   ```

2. **Create resource directory:**
   ```
   android/app/src/main/res/values-ar/strings.xml
   ```

3. **Copy and translate strings:**
   Copy `values/strings.xml` to `values-ar/strings.xml` and translate all strings.

4. **Add to `resourceConfigurations`** in `app/build.gradle.kts`:
   ```kotlin
   resourceConfigurations += listOf("en", "bn", "ar")
   ```

5. **Update settings UI** to show Arabic in the language picker (Phase A2).

---

## String Resource Guidelines

### Naming convention
```
<feature>_<element>_<modifier>
```

Examples:
- `nav_chat` — navigation label for Chat tab
- `action_send` — send button label
- `permission_bluetooth_message` — Bluetooth permission rationale
- `settings_theme_dark` — Dark theme option in Settings
- `accessibility_sos_button` — SOS button content description

### Formatting rules
- Never hardcode user-visible text in Kotlin/Compose files
- Use `stringResource(R.string.xxx)` in Composables
- Use `context.getString(R.string.xxx)` in ViewModels (injected context)
- Use `%s`, `%d`, `%1$s` for string formatting arguments
- Do NOT use machine translation for emergency/safety-critical strings

### Plurals (Phase A2+)
For quantity-dependent strings, use `<plurals>`:
```xml
<plurals name="msg_unread_count">
    <item quantity="one">%d unread message</item>
    <item quantity="other">%d unread messages</item>
</plurals>
```

---

## Right-to-Left (RTL) Support

The app has `android:supportsRtl="true"` in the manifest.

For future RTL language support (e.g., Arabic, Urdu):
- Use `start`/`end` instead of `left`/`right` in XML
- Use `Arrangement.Start` / `Alignment.Start` in Compose
- Test with **Force RTL** in Developer Options

---

## Testing Localization

### In Android Studio emulator
1. Settings → Language & Input → Language → Add a language → বাংলা
2. Drag বাংলা to the top of the list
3. Relaunch the app

### In-app language switch (Phase A2+)
Settings → Language → বাংলা → App reloads in Bangla

### Screenshot testing (Phase A4+)
Automated screenshot tests will run in both `en` and `bn` to detect layout overflow or truncation issues.

---

## Files Reference

| File | Purpose |
|---|---|
| [`SupportedLanguage.kt`](../../android/app/src/main/java/com/mesh/emergency/core/common/localization/SupportedLanguage.kt) | Language enum |
| [`LocaleManager.kt`](../../android/app/src/main/java/com/mesh/emergency/core/common/localization/LocaleManager.kt) | Locale persistence + application |
| [`values/strings.xml`](../../android/app/src/main/res/values/strings.xml) | English strings (default) |
| [`values-bn/strings.xml`](../../android/app/src/main/res/values-bn/strings.xml) | Bangla strings |
| [`AppConstants.kt`](../../android/app/src/main/java/com/mesh/emergency/core/common/constants/AppConstants.kt) | `PREF_KEY_LANGUAGE` constant |
