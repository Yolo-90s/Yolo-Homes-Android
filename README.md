# Yolo-Home's — Smart Apartment Management

A production-ready Android app for apartment **Maintenance** and **Water Consumption** management,
built with Kotlin, Jetpack Compose, Material 3, MVVM + Clean Architecture, Hilt, and Cloud Firestore.

> ✅ The project **compiles and builds a debug APK** (`./gradlew :app:assembleDebug`).

---

## ⚠️ Required setup before running on a device

The repo ships with **placeholder** Firebase config so it builds out of the box. To actually sign in
and read/write data you must wire your own Firebase project:

1. Create a Firebase project and add an Android app with package `com.example.yolo_homes`.
2. Download the real **`google-services.json`** and replace `app/google-services.json`.
3. Enable **Google** sign-in under Authentication → Sign-in method.
4. Copy the **Web client ID** (OAuth 2.0) into `app/src/main/res/values/strings.xml`:
   ```xml
   <string name="default_web_client_id">YOUR_WEB_CLIENT_ID.apps.googleusercontent.com</string>
   ```
5. Add your machine's **SHA-1** to the Firebase Android app (for Google Sign-In).
6. Deploy `firestore.rules` to your project.

## Admin role

`AuthRepository.resolveIsAdmin` marks a user as admin when a `masterFlats` doc with `role == "admin"`
is linked to them by `email` (optional field) or owner/tenant phone. Firestore rules can't query
collections, so they additionally honor a custom `admin` auth claim or a `role` field on the user doc —
mirror the admin flag there (or set a custom claim) when provisioning admins.

---

## Architecture

```
core/            Resource, Formatters, Constants, PdfExporter
data/
  model/         AppUser, Flat, MaintenanceReceipt, Reading, AppSettings, UserSession
  repository/    Auth, Flat, Maintenance, Reading, Settings, Preferences
  FirestoreExt   snapshot-listener → Flow helpers (single source of truth, offline-first)
di/              FirebaseModule (Hilt) — Firestore with persistent cache
feature/
  auth/          Google Sign-In (Credential Manager), Splash, Login, AuthViewModel
  dashboard/     SaaS-style dashboard + stats + quick actions
  maintenance/   Home, Add, History, Receipt detail (+ PDF), MaintenanceViewModel
  water/         Dashboard, Add reading (live calc), History, Bill (+ PDF), WaterViewModel
  reports/       Combined charts + PDF export
  profile/       Profile, dark-mode toggle (DataStore), logout
  settings/      Admin-editable appSettings/main
ui/
  theme/         Brand colors, Material 3 typography, light/dark
  components/     StatCard, Charts (Canvas), Shimmer, EmptyState, FormFields, Avatar
  navigation/     Routes, MainShell (bottom nav + FAB), YoloNavGraph
```

### Highlights
- **StateFlow + `combine`** over Firestore snapshot listeners → reactive, offline-first UI.
- **Firestore persistent cache** enabled for offline support.
- **Custom Canvas charts** (bar + line) — no third-party chart dependency.
- **PDF generation** for receipts, water bills and combined reports via `PdfDocument` + FileProvider.
- **Animated counters, shimmer skeletons, splash fade/scale, animated screen transitions.**
- Existing Firestore collections are read/written **without schema changes**.

## Build

```bash
./gradlew :app:assembleDebug      # build APK
./gradlew :app:installDebug       # install on a connected device
```
