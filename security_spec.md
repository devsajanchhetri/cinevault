# CINEVAULT Security Specification (Payload-First TDD)

## 1. Data Invariants
1. **Strict UID Ownership & Isolation**: Every document under `/users/{userId}` and its subcollections (`studios`, `shoots`, `withdrawals`, `expenses`, `notifications`, `settings`) must require `request.auth != null`, `request.auth.uid == userId`, and `incoming().userId == request.auth.uid`.
2. **Exclusive NPR Currency Constraint**: `currency` in `UserProfile` and `UserSettings` must strictly equal `'NPR'`.
3. **Allowed Nepal Payment Methods**: `paymentMethod` in `Withdrawal` and `Expense` must strictly belong to `['Bank Transfer', 'eSewa', 'Cash', 'Khalti', 'Other']`.
4. **Positive Financial Amounts**: `confirmedEarningsPaisa` in `Shoot` must be `>= 0`; `amountPaisa` in `Withdrawal` and `Expense` must be `> 0`.
5. **Temporal & Identity Immutability**: `userId` and `createdAt` are immutable across all updates (`incoming().userId == existing().userId && incoming().createdAt == existing().createdAt`).
6. **No Ghost Fields**: Every `create` and `update` must enforce `hasOnly(...)` on keys / affectedKeys.

## 2. The Dirty Dozen Payloads
1. **Unauthenticated Read/Write**: Caller with `request.auth == null` reading `/users/alice/studios`.
2. **Cross-User Read (PII Leak)**: Authenticated `bob` reading `/users/alice` or `/users/alice/studios/studio_1`.
3. **Identity Spoofing on Create**: Authenticated `alice` creating `/users/alice/studios/s1` with `userId: "bob"`.
4. **Currency Tampering**: Creating or updating `UserProfile` or `UserSettings` with `currency: "USD"`.
5. **Unsupported Payment Method**: Creating `Withdrawal` with `paymentMethod: "PayPal"`.
6. **Negative or Zero Withdrawal Amount**: Creating `Withdrawal` with `amountPaisa: 0` or `-5000`.
7. **Shadow Field Injection (Create)**: Creating `Studio` with extra field `isAdmin: true`.
8. **Shadow Field Injection (Update)**: Updating `Shoot` with extra field `bypassLedger: true`.
9. **Immutable Field Mutation**: Updating `createdAt` or `userId` on an existing `Shoot`.
10. **Invalid Shoot Category**: Creating `Shoot` with `category: "InvalidCategory"`.
11. **Oversized String DoS**: Creating `Studio` with a `name` exceeding 120 characters.
12. **Future Timestamp Spoofing**: Creating any document with `createdAt` in the future (`> request.time`).
