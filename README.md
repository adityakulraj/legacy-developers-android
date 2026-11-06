# Legacy Developers Android App

![Legacy Developers logo](app/src/main/res/drawable-nodpi/legacy_developers_logo.png)

A single-user Android app for managing residential flat inventory, booking collections, and monthly customer-payment GST records. It is built with Kotlin and Jetpack Compose and stores data locally on the device.

## Features

- Dashboard totals for all, booked, and unsold flats
- Flat inventory with super built-up area
- Booking consideration, paid amount, and calculated balance due
- Add, edit, search, and delete flat records
- Monthly customer-payment and GST collection tracking
- Intra-state CGST/SGST and inter-state IGST summaries
- Paginated PDF payment statements shared through Android's share sheet
- Branded launcher icon and in-app header
- No login, cloud account, or server dependency

## Requirements

- JDK 17
- Android SDK 35
- Android build tools 35

The Gradle wrapper is included, so a separate Gradle installation is not required.

## Build

```bash
./gradlew assembleDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Open the project directory in Android Studio if you prefer to build and run through the IDE.

## Tests

Run the fast JVM unit tests:

```bash
./gradlew testDebugUnitTest
```

Compile the Android end-to-end test APK:

```bash
./gradlew assembleDebugAndroidTest
```

Run the end-to-end tests on a connected device or running emulator:

```bash
./gradlew connectedDebugAndroidTest
```

The test suite covers booking balance calculations, GST allocation, logo rendering, primary navigation, creating a flat, and opening the customer-payment flow.

## Continuous integration

Every push to `master` runs unit tests, compiles the Android test suite, builds a fresh debug APK, and publishes it as the `legacy-developers-debug-apk` workflow artifact. A second CI job runs the end-to-end tests on an Android 15 emulator.

Download artifacts from the completed workflow run on the repository's **Actions** tab.

## Data and GST scope

Inventory and payment records are stored in Android `SharedPreferences` on the local device. There is no cloud sync or multi-user access, so important records should also be backed up separately.

The GST report is a supporting customer-payment collection statement for accounts-team review and manual filing. It is not a statutory GST return and does not determine tax liability. Confirm invoice reporting, time-of-supply, advances, and applicable tax treatment with an accountant or tax professional.
