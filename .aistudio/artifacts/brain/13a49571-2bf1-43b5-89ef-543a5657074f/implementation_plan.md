# CINEVAULT — Cinematography Business & Financial Vault

CINEVAULT is a native Android financial management, project scheduling, and AI business intelligence platform purpose-built for freelance cinematographers, wedding filmmakers, and independent production teams in Nepal. It unifies studio ledgers, shoot earnings, payment collections, business expenses, and automated financial reporting inside an atmospheric Liquid Glass interface backed by real-time Firebase Cloud Firestore persistence and Firebase AI Logic.

### User Review & Critical Decisions

> [!IMPORTANT]
> The following product and architectural decisions incorporate your latest revisions and govern the full implementation of CINEVAULT.

- **Confirmed Decision 1 — Exclusive Currency Support**: **Nepalese Rupee (`NPR`)** is the sole supported currency across the entire application, ensuring consistent decimal-safe NPR formatting (`NPR 45,000.00`) across all ledgers, analytics, AI prompts, and PDF/CSV statements without multi-currency selectors.
- **Confirmed Decision 2 — Supported Payment Methods**: All withdrawal collections and business expense records exclusively use the following payment methods:
  - `Bank Transfer`
  - `eSewa`
  - `Cash`
  - `Khalti`
  - `Other`
- **Confirmed Decision 3 — Shoot Earnings Recognition Rule**: **Only Completed shoots add to the collectible studio ledger**. Planned/upcoming shoots are tracked on the production calendar and schedule conflict detector, and enter a studio's collectible balance only once marked as `Completed` with confirmed earnings.
- **Confirmed Decision 4 — App Lock & Device Security**: **Optional toggle in Settings, disabled by default**. Users can enable device credential / biometric unlock at any time from the Settings screen.
- **Platform Integration — Firebase Auth, Firestore & AI Logic**: Upon plan approval, the app will initiate the interactive Firebase setup flow (`firestore` and `ai_logic`) to provision Google Sign-In via Jetpack Credential Manager, a dedicated Cloud Firestore database with strict UID-isolated security rules, and serverless Gemini (`gemini-3.8-flash`) protected by Firebase App Check.

---

### 1. Overview & Core Concept

- **What It Does**: CINEVAULT acts as a single source of truth for a cinematographer's business operations in `NPR`:
  - **Studio & Client Ledgers**: Complete CRUD management for production houses and studios, calculating total shoot earnings, withdrawals received, outstanding collectible balances, and collection rates dynamically from ledger records.
  - **Shoot & Production Management**: Tracks shoots across categories (`Wedding`, `Commercial`, `Music Video`, `Corporate`, `Pre-Wedding`, `Documentary`, `Other`), duration units, locations, expected payment dates, and status (`Planned`, `Completed`).
  - **Withdrawals & Collections Ledger**: Records payments collected exclusively via `Bank Transfer`, `eSewa`, `Cash`, `Khalti`, and `Other` with reference numbers, duplicate prevention, and automatic updates to studio collectible balances.
  - **Business Expense Ledger**: Separate operational ledger (`Camera Equipment`, `Lens Rental`, `Lighting`, `Assistant Payments`, `Travel`, `Accommodation`, `Food`, `Editing`, `Equipment Maintenance`, `Software Subscriptions`, `Other`) using `Bank Transfer`, `eSewa`, `Cash`, `Khalti`, and `Other`, with receipt image attachments via the zero-permission Android Photo Picker (`PickVisualMedia`). Keeps business overhead separate from studio collectible balances while computing `Net Business Profit = Gross Shoot Earnings - Total Business Expenses`.
  - **Production Calendar & Conflict Detection**: Monthly interactive calendar distinguishing planned vs. completed projects, highlighting same-day/overlapping schedule conflicts, and surfacing overdue payment reminders.
  - **CINEVAULT AI Assistant**: Powered by `gemini-3.8-flash` via Firebase AI Logic with App Check attestation, offering natural language financial Q&A in NPR, studio performance & delayed payment intelligence, historical smart pricing recommendations, one-tap WhatsApp/email payment reminder drafting, and natural language shoot entry with a mandatory user review confirmation sheet before saving.
  - **Native PDF & CSV Statements**: Generates branded multi-section PDF studio statements and financial reports in `NPR` via Android `PdfDocument` and valid Excel/Sheets CSV exports, shared directly via `FileProvider` and the Android Share Sheet.
- **Target Audience / Persona**: Freelance cinematographers, Directors of Photography (DoPs), wedding filmmakers, commercial videographers, and boutique production studios in Nepal who work with multiple partner studios and need crystal-clear visibility into what they have earned, collected, spent, and are owed.
- **Key Value**: Eliminates scattered spreadsheets and manual balance tracking with a strict zero-state, audit-grade NPR ledger that separates studio receivables from internal business expenses and accelerates collections with instant PDF statements and AI-drafted reminders.

---

### 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Startup & Authentication**: Animated CINEVAULT aperture splash transition into a Google Sign-In screen powered by Android `CredentialManager` (`GetSignInWithGoogleOption`).
  2. **First-Login Onboarding & Strict Zero State**: On initial sign-in, initializes the user's isolated Firestore profile in `NPR`, presents an interactive feature walkthrough, and lands on a strict zero-state dashboard (`Total Earnings: NPR 0`, `Total Withdrawals: NPR 0`, `Total Expenses: NPR 0`, `Available Balance: NPR 0`, `Studios: 0`, `Shoots: 0`) with zero fake or demo records.
  3. **Home Dashboard & Quick Actions**: Displays the hero **AVAILABLE TO COLLECT** spotlight in `NPR` with animated counters, Lifetime Earnings, Total Withdrawals, Business Expenses, Net Business Profit, studio & shoot counts, one-tap Quick Actions (`Add Shoot`, `Add Studio`, `Record Withdrawal`, `Add Expense`), Studio Spotlight cards, Payment Alerts (pending & overdue), Financial Trends chart, AI Business Insights card, and Recent Activity feed.
  4. **5-Tab Floating Liquid Glass Navigation**:
     - `HOME`: Executive financial spotlight, quick actions, alerts, studio spotlight, recent ledger activity, and quick access to Withdrawals, Expenses, Calendar, Reports, Reminders, Global Search, and Profile/Settings.
     - `STUDIOS`: Searchable studio directory, archive toggle, studio creation/editing, accidental-deletion guard when financial records exist, full studio detail ledger, and single-studio PDF statement export.
     - `SHOOTS`: Searchable, filterable (by category, studio, date range, planned vs. completed), and sortable shoot ledger with instant balance recognition upon completion.
     - `INSIGHTS`: Interactive analytics suite with date range filters (`This Week`, `This Month`, `Last Month`, `Last 3 Months`, `This Year`, `All Time`, `Custom Date Range`), custom Compose canvas charts (`Monthly Earnings`, `Monthly Withdrawals`, `Monthly Expenses`, `Net Profit`, `Studio Performance`, `Category Performance`, `Outstanding Payments`), and key performance metrics in `NPR`.
     - `AI ASSISTANT`: Dedicated conversational & structured AI workspace with mode chips (`Financial Q&A`, `Business Intelligence`, `Smart Pricing`, `Payment Assistant`, `Natural Language Shoot Entry`) and a pre-save review confirmation modal for parsed natural language entries.

- **Visual Identity & Theme**:
  - *Aesthetic Direction*: Intentional Dark Mode ("Obsidian Cinema & Liquid Glass") inspired by high-end cinema camera interfaces (RED / ARRI telemetry) and Apple's translucent Liquid Glass depth.
  - *Color Palette & Mood*:
    - Primary Canvas (`Obsidian`): `#08090D`
    - Elevated Surface (`Deep Charcoal`): `#14151B`
    - Translucent Glass Surface: `#FFFFFF` at `8%–12%` opacity with subtle top-left specular gradient luminance
    - Specular Glass Border: `1.dp` hairline stroke transitioning from `#FFFFFF` at `20%` opacity (top-left) to `6%` opacity (bottom-right)
    - Primary Brand Accent (`Cinema Crimson`): `#E50914`
    - Positive / Collected (`Success`): `#46D369`
    - Pending / Warning (`Amber`): `#F5A623`
    - Primary Text (`Pure White`): `#FFFFFF`
    - Secondary Text (`Zinc Muted`): `#A1A1AA`
  - *Typography & Hierarchy*: Bundled local Google Fonts (`Outfit` for geometric display headlines and hero financial figures, `Plus Jakarta Sans` for crisp UI labels and body copy, and `JetBrains Mono` with tabular numerals for `NPR` financial amounts, dates, and reference codes).
  - *Component Styling & Layout*: Reusable Liquid Glass design system (`GlassCard`, `GlassButton`, `GlassNavigationBar`, `GlassInputField`, `GlassDialog`, `GlassBottomSheet`, `GlassMetricCard`, `GlassFilterChip`, `GlassTransactionRow`, `GlassStudioCard`, `GlassChartContainer`) respecting `WindowInsets.safeDrawing` and `WindowInsets.navigationBars` with adaptive canonical layouts for compact handhelds and expanded screens.
- **Interactive Feedback & Motion**:
  - Smooth spring physics (`spring(dampingRatio = Spring.DampingRatioLowBouncy)`) on tab transitions, filter chips, and expandable cards.
  - Animated number interpolation on financial hero counters when ledger entries update.
  - Clear loading skeletons, structured error banners with retry actions, and rich empty states guiding first-time entry.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1 — Derived Balances Over Stored Counters**:
  - *Chosen Approach*: Compute studio collectible balances, collection percentages, lifetime earnings, total withdrawals, and net profit reactively from the underlying `shoots`, `withdrawals`, and `expenses` streams rather than storing mutable balance counters on studio or user documents.
  - *Why*: Guarantees 100% accounting integrity, eliminates counter drift during edits or deletions, and ensures filtering date ranges never corrupts balances.
  - *Alternatives Considered*: Incrementing/decrementing balance fields via client-side counters, which is prone to race conditions and partial update drift.
- **Decision 2 — Strict Separation of Completed vs. Planned Shoots & Business Expenses**:
  - *Chosen Approach*: Only shoots with `status == COMPLETED` contribute to `Gross Shoot Earnings` and studio collectible balances (`Completed Shoot Earnings - Studio Withdrawals`). Business expenses are stored in a dedicated `/expenses` collection and deducted from `Gross Shoot Earnings` to compute `Net Business Profit`, never reducing what a studio owes the cinematographer.
  - *Why*: Matches real-world production accounting where a cinematographer's out-of-pocket lens rental or travel cost reduces their personal net profit but does not forgive the client studio's invoice balance.
- **Decision 3 — Serverless AI Security via Firebase AI Logic & App Check**:
  - *Chosen Approach*: Use `Firebase.ai.generativeModel("gemini-3.8-flash")` secured by Firebase App Check (`configureAppCheck` in `MainActivity.onCreate`) and strict UID-scoped context injection, plus `Schema.obj(...)` structured JSON output for Natural Language Shoot Entry.
  - *Why*: Eliminates client-side API key exposure in APK binaries, enforces authenticated user isolation, and guarantees deterministic JSON parsing into a mandatory human-in-the-loop review sheet before any Firestore write occurs.

---

### 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Component Diagram**:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        PRESENTATION LAYER (Jetpack Compose)                 │
│  ┌──────────────┐  ┌─────────────────────────────────────────────────────┐  │
│  │ Splash / Auth│  │            Main Vault Shell + App Lock Gate         │  │
│  │  & Onboard   │  │  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────┐ │  │
│  │ (Credential  │  │  │  HOME  │ │STUDIOS │ │ SHOOTS │ │INSIGHTS│ │ AI │ │  │
│  │   Manager)   │  │  └───┬────┘ └───┬────┘ └───┬────┘ └───┬────┘ └─┬──┘ │  │
│  └──────┬───────┘  │      ┴──────────┴──────────┴──────────┴────────┴    │  │
│         │          │  Secondary Modals: Withdrawals │ Expenses │ Calendar│  │
│         │          │                    Reports/PDF │ Search   │ Settings│  │
│         │          └─────────────────────────┬───────────────────────────┘  │
└─────────┼────────────────────────────────────┼──────────────────────────────┘
          ▼                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                     DOMAIN & FINANCIAL ENGINE LAYER                         │
│  ┌──────────────────────────┐  ┌───────────────────────┐  ┌──────────────┐  │
│  │ LedgerCalculatorUseCase  │  │ ConflictDetector &    │  │ PDF & CSV    │  │
│  │ (NPR BigDecimal Math,    │  │ PaymentAlertEngine    │  │ ExportEngine │  │
│  │  Studio/Profit Rollups)  │  │ (Schedule & Overdue)  │  │ (PdfDocument)│  │
│  └────────────┬─────────────┘  └───────────┬───────────┘  └──────┬───────┘  │
└───────────────┼────────────────────────────┼─────────────────────┼──────────┘
                ▼                            ▼                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        DATA & CLOUD INTEGRATION LAYER                       │
│  ┌────────────────────────────────────┐  ┌───────────────────────────────┐  │
│  │ CineVaultRepository (Firestore)    │  │ CineVaultAiService            │  │
│  │ • Real-time .snapshots() Flows     │  │ • Firebase AI Logic           │  │
│  │ • Named DB (firestore_database_id) │  │   (gemini-3.8-flash)          │  │
│  │ • Safe Transactions & Dedup Checks │  │ • App Check Attestation       │  │
│  │ • Structured Error Handler         │  │ • Schema.obj NL Entry Parser  │  │
│  └─────────────────┬──────────────────┘  └───────────────┬───────────────┘  │
└────────────────────┼─────────────────────────────────────┼──────────────────┘
                     ▼                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                   FIREBASE BACKEND & ANDROID SYSTEM SERVICES                │
│  • Firebase Auth (Google Sign-In)     • Cloud Firestore (/users/{uid}/...)  │
│  • Zero-Trust firestore.rules         • Android FileProvider & Share Sheet  │
└─────────────────────────────────────────────────────────────────────────────┘
```

- **Data Model & State**:
  All collections are strictly isolated under `/users/{uid}` in the provisioned named Firestore database (`R.string.firestore_database_id`), with every document validated by `firestore.rules`:
  - **User Profile & Settings (`/users/{uid}` & `/users/{uid}/settings/preferences`)**: `uid`, `displayName`, `email`, `photoUrl`, `currency` (fixed to `"NPR"`), `onboardingCompleted`, `aiEnabled`, `appLockEnabled`, `notificationsEnabled`, `paymentReminderDays`, `createdAt`, `updatedAt`.
  - **Studios (`/users/{uid}/studios/{studioId}`)**: `id`, `userId`, `name`, `contactPerson`, `phone`, `email`, `address`, `notes`, `archived`, `createdAt`, `updatedAt`.
  - **Shoots (`/users/{uid}/shoots/{shootId}`)**: `id`, `userId`, `eventTitle`, `studioId`, `studioName`, `shootDateEpochMs`, `category`, `location`, `durationValue`, `durationUnit` (`Hours`, `Days`), `confirmedEarningsPaisa` (stored in integer minor units / BigDecimal-safe NPR representation), `expectedPaymentDateEpochMs`, `paymentStatus` (`Pending`, `Collectible`, `Overdue`, `Settled`), `projectStatus` (`Planned`, `Completed`), `attachmentUri`, `notes`, `createdAt`, `updatedAt`.
  - **Withdrawals (`/users/{uid}/withdrawals/{withdrawalId}`)**: `id`, `userId`, `studioId`, `studioName`, `amountPaisa`, `dateEpochMs`, `paymentMethod` (`Bank Transfer`, `eSewa`, `Cash`, `Khalti`, `Other`), `referenceNumber`, `idempotencyKey`, `notes`, `createdAt`, `updatedAt`.
  - **Expenses (`/users/{uid}/expenses/{expenseId}`)**: `id`, `userId`, `expenseName`, `category`, `amountPaisa`, `dateEpochMs`, `associatedShootId`, `associatedStudioId`, `paymentMethod` (`Bank Transfer`, `eSewa`, `Cash`, `Khalti`, `Other`), `receiptUri`, `notes`, `createdAt`, `updatedAt`.
  - **Notifications / Reminders (`/users/{uid}/notifications/{notificationId}`)**: `id`, `userId`, `title`, `message`, `type`, `relatedEntityId`, `dueDateEpochMs`, `isRead`, `createdAt`.

- **Interactive Component & State Mapping**:
  - **Studio Safeguard**: Attempting to delete a studio checks active `shoots` and `withdrawals` for `studioId`; if records exist, deletion is blocked with a clear prompt offering **Archive Studio** instead so historical ledgers remain intact.
  - **Withdrawal Validation & Deduplication**: Recording a withdrawal validates `amount > 0`, restricts payment method to `Bank Transfer`, `eSewa`, `Cash`, `Khalti`, or `Other`, checks for duplicate `referenceNumber` / same-day identical studio amount warnings, and uses atomic Firestore writes.
  - **Calendar Conflict Detection**: Adding or editing a shoot compares `shootDateEpochMs` against existing shoots and displays a real-time conflict warning banner if another shoot is scheduled on the same date.
  - **AI Natural Language Entry & Confirmation Gate**: When a user submits a sentence like *"I completed a wedding shoot for ABC Studio in Kathmandu today for NPR 45,000"*, `CineVaultAiService` invokes `gemini-3.8-flash` with a strict `Schema.obj` response schema to extract `eventTitle`, `studioName`, `shootDate`, `location`, `category`, `confirmedEarnings`, and `projectStatus`. The UI displays an editable **Review Extracted Shoot Sheet**; nothing is written to Firestore until the user taps **Confirm & Save to Ledger**. If the user disables AI in Settings, all AI requests are blocked cleanly.
  - **Account Export & Account Deletion**: Settings provides a complete JSON/CSV data bundle export via `FileProvider` and a destructive two-step **Delete Account & Purge Vault** action that deletes all user subcollections, removes the user profile document, signs out of Firebase Auth, and clears Credential Manager state.
