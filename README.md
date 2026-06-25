# SubSpy

**Find your forgotten subscriptions.** The average American loses $200/year on forgotten subscriptions. SubSpy solves this by scanning Gmail for payment receipts and showing all active subscriptions in one place.

## Features

- **Gmail Scanning** — Automatically finds payment receipts and invoices using Gmail API (read-only)
- **Subscription Tracking** — Shows service name, amount, billing frequency, and next billing date
- **Forgotten Detection** — Highlights subscriptions not used in 6+ months in red
- **Cancellation Guides** — Step-by-step instructions and direct links to cancel unwanted subscriptions
- **Billing Reminders** — Notifications 3 or 7 days before each charge
- **Premium Version** — $2.99/month for unlimited tracking via Google Play Billing

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material Design 3 |
| Auth | Google OAuth 2.0 |
| Email | Gmail API (read-only scope) |
| Database | Firebase Firestore |
| Notifications | Firebase Cloud Messaging + WorkManager |
| Billing | Google Play Billing Library 6.x |
| DI | Hilt |
| Min SDK | API 26 (Android 8.0) |

## Screens

1. **Login** — Google Sign-In with Gmail read-only permission
2. **Home** — Total monthly spend + subscription list (forgotten ones highlighted)
3. **Subscription Detail** — Amount, history, cancellation steps, website link
4. **Notifications** — Configure reminders, view upcoming charges
5. **Premium** — Upgrade to unlock unlimited subscriptions

## Languages Supported

English, Spanish, Russian, French, German, Portuguese, Italian, Chinese, Japanese, Arabic, Hindi

## Setup

1. Create a Firebase project and add `google-services.json` to `app/`
2. Enable Gmail API in Google Cloud Console
3. Configure OAuth consent screen with `gmail.readonly` scope
4. Set up Google Play Billing product `subspy_premium_monthly`
5. Build and run:

```bash
./gradlew assembleDebug
```

## Architecture

```
com.subspy.app/
├── billing/          # Google Play Billing integration
├── data/
│   ├── model/        # Subscription, UserProfile, CancellationInfo
│   └── repository/   # GmailRepository, FirestoreRepository
├── di/               # Hilt dependency injection modules
├── notifications/    # FCM service + WorkManager scheduler
├── ui/
│   ├── components/   # Reusable composables
│   ├── navigation/   # NavHost setup
│   ├── screens/      # Login, Home, Detail, Notifications, Premium
│   └── theme/        # Material 3 dark/light theme with green accent
└── viewmodel/        # AuthViewModel, SubscriptionViewModel, PremiumViewModel
```

## Design

- Dark theme by default with green (#00E676) accent color
- Material Design 3 guidelines
- Full dark mode support (follows system setting)
- Premium, trustworthy feel for financial data handling

## Package

`com.subspy.app`
