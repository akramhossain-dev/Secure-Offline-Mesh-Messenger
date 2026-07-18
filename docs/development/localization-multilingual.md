# Multi Language & Localization — Phase A22

## Localization Architecture

The Localization subsystem coordinates resource lookups and locale formats offline, completely independent from active databases or network hops:

```
 ┌───────────────────────────┐
 │     Language Manager      │  (Tracks preferences and formats metrics)
 └─────────────┬─────────────┘
               │ locales context
 ┌─────────────▼─────────────┐
 │    Local XML Resources    │  (values/strings.xml & values-bn/strings.xml)
 └─────────────┬─────────────┘
               │
 ┌─────────────▼─────────────┐
 │   Compose Theme / Views   │  (Renders localized text strings)
 └───────────────────────────┘
```

The system resolves strings dynamically using the standard Android resource manager, wrapping preferences inside the [`LanguageManager`](../../android/app/src/main/java/com/mesh/emergency/core/localization/LanguageManager.kt) contract.

---

## Supported Locales & Fallbacks

- **English (`en`)**: Default language. Used when system locales do not match Bangla.
- **Bangla (`bn`)**: Full translation support. Triggered automatically if the system locale language is `bn` or selected manually.

---

## Locale-Specific Date & Number Formats

Disaster telemetry messages and GPS coordinates must be displayed using the correct numerals to prevent confusion:

1. **Number Formatting**:
   - English: `1,234.56`
   - Bangla: `১,২৩৪.৫৬` (formatted dynamically using `java.text.NumberFormat`).
2. **Date & Time formatting**:
   - English: `yyyy-MM-dd HH:mm`
   - Bangla: `dd/MM/yyyy HH:mm`

---

## Accessibility & Screen Reader Compliance

To ensure accessibility in emergency areas:
- **Scalable Font Sizes**: All text components utilize Compose `sp` dimensions, allowing layout scaling based on system accessibility sizes.
- **Content Descriptions**: Every asset, button, and status indicator has a matching description key (e.g. `accessibility_sos_button`) to support Android TalkBack screen reader loops.
- **Contrast Ratios**: Layout colors conform to WCAG AA contrast rules (checked in Phase A3).
