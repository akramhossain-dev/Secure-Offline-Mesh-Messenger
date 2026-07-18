# Branch Strategy

## Overview

This project follows **GitHub Flow** with structured branch naming conventions.
All feature work happens on short-lived branches. `main` is always deployable.

---

## Branch Types

| Branch Type | Pattern | Purpose | Base Branch |
|---|---|---|---|
| `main` | `main` | Production-ready, always deployable | — |
| `develop` | `develop` | Integration branch for completed features | `main` |
| Feature | `feature/<ticket>-<description>` | New features | `develop` |
| Bugfix | `bugfix/<ticket>-<description>` | Non-critical bug fixes | `develop` |
| Hotfix | `hotfix/<ticket>-<description>` | Critical production fixes | `main` |
| Release | `release/<version>` | Release preparation | `develop` |
| Phase | `phase/<phase-id>` | Development phase branches | `develop` |
| Chore | `chore/<description>` | Build, docs, config changes | `develop` |

---

## Naming Conventions

### Feature branches
```
feature/A2-ble-transport
feature/A3-lora-integration
feature/A4-qr-pairing
```

### Bugfix branches
```
bugfix/42-ble-disconnect-crash
bugfix/17-dark-theme-contrast
```

### Hotfix branches
```
hotfix/sos-not-broadcasting
hotfix/crash-on-startup
```

### Release branches
```
release/0.1.0
release/0.2.0
```

### Phase branches
```
phase/A1-foundation
phase/A2-ble-encryption
phase/A3-lora-mesh
```

### Chore branches
```
chore/update-dependencies
chore/setup-ci
chore/add-detekt-rules
```

---

## Workflow

### Feature development
```
main
  └── develop
        └── feature/A2-ble-transport
              ↓ (PR to develop)
        develop
              ↓ (PR to main after QA)
        main
```

### Hotfix
```
main
  └── hotfix/critical-crash
        ↓ (PR to main + cherry-pick to develop)
main
```

---

## Branch Protection Rules

> Configure these in GitHub Settings → Branches:

- **`main`**: Require PR + at least 1 approval + status checks pass
- **`develop`**: Require PR + status checks pass
- **No direct pushes** to `main` or `develop`
- **Delete branches** after merge

---

## Merge Strategy

| Target | Strategy | Squash? |
|---|---|---|
| `develop` | Squash merge | Yes — one clean commit per feature |
| `main` | Merge commit | No — preserve full history |
| `hotfix → develop` | Cherry-pick | Yes |
