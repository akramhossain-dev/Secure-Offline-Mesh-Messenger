# Commit Message Convention

This project follows the **Conventional Commits** specification.
See: [https://www.conventionalcommits.org](https://www.conventionalcommits.org)

---

## Format

```
<type>(<scope>): <short summary>

[optional body]

[optional footer(s)]
```

### Rules
- Subject line: **72 characters max**
- Use **imperative mood** ("add" not "added", "fix" not "fixed")
- **No period** at end of subject line
- Separate body from subject with a blank line
- Body: wrap at 80 characters

---

## Commit Types

| Type | When to Use | Example |
|---|---|---|
| `feat` | New feature | `feat(ble): add BLE device discovery` |
| `fix` | Bug fix | `fix(chat): correct message ordering` |
| `refactor` | Code refactor (no behavior change) | `refactor(crypto): extract key derivation` |
| `style` | Formatting, ktlint fixes | `style: apply ktlint formatting` |
| `test` | Adding or fixing tests | `test(result): add fold operator tests` |
| `docs` | Documentation changes | `docs: update architecture overview` |
| `chore` | Build, config, tooling | `chore: update AGP to 8.7.3` |
| `ci` | CI/CD pipeline changes | `ci: add detekt to PR workflow` |
| `perf` | Performance improvement | `perf(lora): reduce packet serialization overhead` |
| `build` | Build system changes | `build: add Spotless plugin` |
| `phase` | Phase completion | `phase: complete A1 project foundation` |

---

## Scopes

Use scopes from the module/layer affected:

| Scope | Layer / Area |
|---|---|
| `app` | Application class, MainActivity |
| `ble` | Bluetooth transport |
| `lora` | LoRa transport |
| `crypto` | Encryption / keys |
| `chat` | Chat feature |
| `contacts` | Contacts feature |
| `emergency` | Emergency / SOS feature |
| `map` | Offline map feature |
| `dashboard` | Network dashboard |
| `settings` | Settings feature |
| `theme` | Design system / theming |
| `locale` | Localization |
| `di` | Dependency injection |
| `db` | Database / Room |
| `domain` | Domain layer |
| `data` | Data layer |
| `gradle` | Gradle / build |
| `docs` | Documentation |

---

## Examples

### Simple feature
```
feat(ble): add BLE device discovery with scan timeout
```

### Bug fix with body
```
fix(chat): prevent duplicate messages on reconnect

The BLE reconnect event was re-triggering message sync,
causing duplicate inserts. Added idempotency check in
MessageRepositoryImpl using message UUID.

Closes #42
```

### Breaking change
```
feat(crypto)!: migrate key format to v2

BREAKING CHANGE: Key files stored by previous versions are
incompatible. Users must re-pair all contacts after upgrade.
```

### Phase completion
```
phase: complete A1 — project foundation & workspace setup

All A1 deliverables implemented:
- Gradle configuration with Version Catalog
- Material Design 3 theme system
- Localization infrastructure (EN + BN)
- Logging foundation (Timber)
- Code quality tooling (Detekt, ktlint, Spotless)
- Git foundation
```

---

## Commitizen (optional tooling)

You can use [Commitizen](https://commitizen.github.io/cz-cli/) with the
`cz-conventional-changelog` adapter to interactively build commit messages.

```bash
npm install -g commitizen cz-conventional-changelog
echo '{ "path": "cz-conventional-changelog" }' > ~/.czrc
```

Then use `git cz` instead of `git commit`.
