package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.database.OperationType
import com.example.core.database.handleFirestoreError
import com.example.domain.model.CineVaultConstants
import com.example.domain.model.Expense
import com.example.domain.model.Shoot
import com.example.domain.model.Studio
import com.example.domain.model.UserProfile
import com.example.domain.model.UserSettings
import com.example.domain.model.VaultNotification
import com.example.domain.model.Withdrawal
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CineVaultRepository(private val db: FirebaseFirestore) {

    // CRITICAL: Always initialize with R.string.firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth
        get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing CINEVAULT Firestore.")
    }

    // -------------------------------------------------------------------------
    // 1. User Profile & Settings (Strict Zero State Initialization)
    // -------------------------------------------------------------------------

    fun observeUserProfile(userId: String = requireUserId()): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    fun observeUserSettings(userId: String = requireUserId()): Flow<UserSettings> = flow {
        val path = "users/$userId/settings/preferences"
        emitAll(
            db.collection("users").document(userId)
                .collection("settings").document("preferences")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(UserSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: UserSettings(id = "preferences", userId = userId)
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun ensureUserInitialized(
        displayName: String,
        email: String,
        photoUrl: String
    ): Result<UserProfile> = runCatching {
        val uid = requireUserId()
        val userDocRef = db.collection("users").document(uid)
        val settingsDocRef = userDocRef.collection("settings").document("preferences")

        try {
            val existingSnap = userDocRef.get().await()
            if (!existingSnap.exists()) {
                val newProfile = UserProfile(
                    id = uid,
                    userId = uid,
                    displayName = displayName.ifBlank { "Cinematographer" }.take(100),
                    email = email.take(200),
                    photoUrl = photoUrl.take(500),
                    currency = CineVaultConstants.CURRENCY_NPR,
                    onboardingCompleted = false,
                    createdAt = null,
                    updatedAt = null
                )
                userDocRef.set(newProfile).await()

                val defaultSettings = UserSettings(
                    id = "preferences",
                    userId = uid,
                    currency = CineVaultConstants.CURRENCY_NPR,
                    aiEnabled = true,
                    appLockEnabled = false,
                    notificationsEnabled = true,
                    paymentReminderDays = 7L,
                    createdAt = null,
                    updatedAt = null
                )
                settingsDocRef.set(defaultSettings).await()
                newProfile
            } else {
                existingSnap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: UserProfile(id = uid, userId = uid, displayName = displayName.ifBlank { "Cinematographer" }, email = email)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userDocRef.path)
            throw e
        }
    }

    suspend fun completeOnboarding(): Result<Unit> = runCatching {
        val uid = requireUserId()
        val userDocRef = db.collection("users").document(uid)
        try {
            val snap = userDocRef.get().await()
            val existing = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (existing != null && existing.createdAt != null) {
                userDocRef.set(
                    existing.copy(
                        currency = CineVaultConstants.CURRENCY_NPR,
                        onboardingCompleted = true,
                        updatedAt = null
                    )
                ).await()
            } else {
                val profile = UserProfile(
                    id = uid,
                    userId = uid,
                    displayName = auth.currentUser?.displayName?.ifBlank { "Cinematographer" } ?: "Cinematographer",
                    email = auth.currentUser?.email ?: "",
                    photoUrl = auth.currentUser?.photoUrl?.toString() ?: "",
                    currency = CineVaultConstants.CURRENCY_NPR,
                    onboardingCompleted = true,
                    createdAt = null,
                    updatedAt = null
                )
                userDocRef.set(profile).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userDocRef.path)
            throw e
        }
    }

    suspend fun updateUserProfile(displayName: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(displayName.isNotBlank()) { "Display name cannot be empty" }
        val userDocRef = db.collection("users").document(uid)
        try {
            val snap = userDocRef.get().await()
            val existing = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: throw IllegalStateException("Profile not initialized")
            requireNotNull(existing.createdAt) { "Profile createdAt missing" }
            userDocRef.set(
                existing.copy(
                    displayName = displayName.trim().take(100),
                    updatedAt = null
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userDocRef.path)
            throw e
        }
    }

    suspend fun saveUserSettings(settings: UserSettings): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("settings").document("preferences")
        try {
            val snap = docRef.get().await()
            val existing = snap.toObject(UserSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (existing != null && existing.createdAt != null) {
                docRef.set(
                    settings.copy(
                        id = "preferences",
                        userId = uid,
                        currency = CineVaultConstants.CURRENCY_NPR,
                        createdAt = existing.createdAt,
                        updatedAt = null
                    )
                ).await()
            } else {
                docRef.set(
                    settings.copy(
                        id = "preferences",
                        userId = uid,
                        currency = CineVaultConstants.CURRENCY_NPR,
                        createdAt = null,
                        updatedAt = null
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // -------------------------------------------------------------------------
    // 2. Studios CRUD (with Accidental Deletion Safeguard)
    // -------------------------------------------------------------------------

    fun observeStudios(userId: String = requireUserId()): Flow<List<Studio>> = flow {
        val path = "users/$userId/studios"
        emitAll(
            db.collection("users").document(userId).collection("studios")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Studio::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.name.lowercase() }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createStudio(studio: Studio): Result<String> = runCatching {
        val uid = requireUserId()
        require(studio.name.isNotBlank()) { "Studio name is required" }
        val colRef = db.collection("users").document(uid).collection("studios")
        val docRef = if (studio.id.isNotBlank()) colRef.document(studio.id) else colRef.document()
        try {
            val payload = studio.copy(
                id = docRef.id,
                userId = uid,
                name = studio.name.trim().take(120),
                contactPerson = studio.contactPerson.trim().take(120),
                phone = studio.phone.trim().take(40),
                email = studio.email.trim().take(150),
                address = studio.address.trim().take(250),
                notes = studio.notes.trim().take(1000),
                createdAt = null,
                updatedAt = null
            )
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateStudio(studio: Studio): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(studio.id.isNotBlank()) { "Studio ID is required for update" }
        require(studio.name.isNotBlank()) { "Studio name is required" }
        val docRef = db.collection("users").document(uid).collection("studios").document(studio.id)
        try {
            val createdAt = studio.createdAt ?: docRef.get().await()
                .toObject(Studio::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.createdAt
            requireNotNull(createdAt) { "Studio createdAt is required for update" }
            docRef.set(
                studio.copy(
                    userId = uid,
                    name = studio.name.trim().take(120),
                    contactPerson = studio.contactPerson.trim().take(120),
                    phone = studio.phone.trim().take(40),
                    email = studio.email.trim().take(150),
                    address = studio.address.trim().take(250),
                    notes = studio.notes.trim().take(1000),
                    createdAt = createdAt,
                    updatedAt = null
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteStudioIfNoRecords(studioId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val studioRef = userRef.collection("studios").document(studioId)
        try {
            val linkedShoots = userRef.collection("shoots")
                .whereEqualTo("studioId", studioId)
                .get()
                .await()
            val linkedWithdrawals = userRef.collection("withdrawals")
                .whereEqualTo("studioId", studioId)
                .get()
                .await()
            if (!linkedShoots.isEmpty || !linkedWithdrawals.isEmpty) {
                throw IllegalStateException(
                    "Cannot delete studio with ${linkedShoots.size()} shoots and ${linkedWithdrawals.size()} withdrawals. Archive this studio instead to preserve financial ledger integrity."
                )
            }
            studioRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, studioRef.path)
            throw e
        }
    }

    // -------------------------------------------------------------------------
    // 3. Shoots CRUD
    // -------------------------------------------------------------------------

    fun observeShoots(userId: String = requireUserId()): Flow<List<Shoot>> = flow {
        val path = "users/$userId/shoots"
        emitAll(
            db.collection("users").document(userId).collection("shoots")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Shoot::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.shootDateEpochMs }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createShoot(shoot: Shoot): Result<String> = runCatching {
        val uid = requireUserId()
        require(shoot.eventTitle.isNotBlank()) { "Event title is required" }
        require(shoot.studioId.isNotBlank()) { "Studio selection is required" }
        require(shoot.confirmedEarningsPaisa >= 0L) { "Earnings cannot be negative" }
        val colRef = db.collection("users").document(uid).collection("shoots")
        val docRef = if (shoot.id.isNotBlank()) colRef.document(shoot.id) else colRef.document()
        try {
            val payload = shoot.copy(
                id = docRef.id,
                userId = uid,
                eventTitle = shoot.eventTitle.trim().take(150),
                studioId = shoot.studioId.trim().take(128),
                studioName = shoot.studioName.trim().take(120),
                category = shoot.category.takeIf { it in CineVaultConstants.SHOOT_CATEGORIES } ?: "Wedding",
                location = shoot.location.trim().take(200),
                durationValue = shoot.durationValue.coerceIn(1L, 365L),
                durationUnit = shoot.durationUnit.takeIf { it in CineVaultConstants.DURATION_UNITS } ?: "Days",
                paymentStatus = shoot.paymentStatus.takeIf { it in CineVaultConstants.PAYMENT_STATUSES } ?: "Collectible",
                projectStatus = shoot.projectStatus.takeIf { it in CineVaultConstants.PROJECT_STATUSES } ?: "Completed",
                notes = shoot.notes.trim().take(1000),
                attachmentUri = shoot.attachmentUri.trim().take(500),
                createdAt = null,
                updatedAt = null
            )
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateShoot(shoot: Shoot): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(shoot.id.isNotBlank()) { "Shoot ID is required for update" }
        require(shoot.eventTitle.isNotBlank()) { "Event title is required" }
        val docRef = db.collection("users").document(uid).collection("shoots").document(shoot.id)
        try {
            val createdAt = shoot.createdAt ?: docRef.get().await()
                .toObject(Shoot::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.createdAt
            requireNotNull(createdAt) { "Shoot createdAt is required for update" }
            docRef.set(
                shoot.copy(
                    userId = uid,
                    eventTitle = shoot.eventTitle.trim().take(150),
                    studioId = shoot.studioId.trim().take(128),
                    studioName = shoot.studioName.trim().take(120),
                    category = shoot.category.takeIf { it in CineVaultConstants.SHOOT_CATEGORIES } ?: "Wedding",
                    location = shoot.location.trim().take(200),
                    durationValue = shoot.durationValue.coerceIn(1L, 365L),
                    durationUnit = shoot.durationUnit.takeIf { it in CineVaultConstants.DURATION_UNITS } ?: "Days",
                    paymentStatus = shoot.paymentStatus.takeIf { it in CineVaultConstants.PAYMENT_STATUSES } ?: "Collectible",
                    projectStatus = shoot.projectStatus.takeIf { it in CineVaultConstants.PROJECT_STATUSES } ?: "Completed",
                    notes = shoot.notes.trim().take(1000),
                    attachmentUri = shoot.attachmentUri.trim().take(500),
                    createdAt = createdAt,
                    updatedAt = null
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteShoot(shootId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("shoots").document(shootId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // -------------------------------------------------------------------------
    // 4. Withdrawals CRUD (with Duplicate Reference Prevention)
    // -------------------------------------------------------------------------

    fun observeWithdrawals(userId: String = requireUserId()): Flow<List<Withdrawal>> = flow {
        val path = "users/$userId/withdrawals"
        emitAll(
            db.collection("users").document(userId).collection("withdrawals")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Withdrawal::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.dateEpochMs }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createWithdrawal(withdrawal: Withdrawal): Result<String> = runCatching {
        val uid = requireUserId()
        require(withdrawal.studioId.isNotBlank()) { "Studio is required" }
        require(withdrawal.amountPaisa > 0L) { "Withdrawal amount must be greater than NPR 0" }
        require(withdrawal.paymentMethod in CineVaultConstants.PAYMENT_METHODS) {
            "Payment method must be Bank Transfer, eSewa, Cash, Khalti, or Other"
        }

        val colRef = db.collection("users").document(uid).collection("withdrawals")
        val trimmedRef = withdrawal.referenceNumber.trim().take(100)
        if (trimmedRef.isNotEmpty()) {
            val dupCheck = colRef
                .whereEqualTo("studioId", withdrawal.studioId)
                .whereEqualTo("referenceNumber", trimmedRef)
                .get()
                .await()
            if (!dupCheck.isEmpty) {
                throw IllegalStateException("Duplicate transaction reference '$trimmedRef' already exists for this studio.")
            }
        }

        val docRef = if (withdrawal.id.isNotBlank()) colRef.document(withdrawal.id) else colRef.document()
        try {
            val payload = withdrawal.copy(
                id = docRef.id,
                userId = uid,
                studioId = withdrawal.studioId.trim().take(128),
                studioName = withdrawal.studioName.trim().take(120),
                referenceNumber = trimmedRef,
                notes = withdrawal.notes.trim().take(1000),
                createdAt = null,
                updatedAt = null
            )
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateWithdrawal(withdrawal: Withdrawal): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(withdrawal.id.isNotBlank()) { "Withdrawal ID is required" }
        require(withdrawal.amountPaisa > 0L) { "Withdrawal amount must be greater than NPR 0" }
        require(withdrawal.paymentMethod in CineVaultConstants.PAYMENT_METHODS) {
            "Payment method must be Bank Transfer, eSewa, Cash, Khalti, or Other"
        }
        val docRef = db.collection("users").document(uid).collection("withdrawals").document(withdrawal.id)
        try {
            val createdAt = withdrawal.createdAt ?: docRef.get().await()
                .toObject(Withdrawal::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.createdAt
            requireNotNull(createdAt) { "Withdrawal createdAt is required for update" }
            docRef.set(
                withdrawal.copy(
                    userId = uid,
                    studioId = withdrawal.studioId.trim().take(128),
                    studioName = withdrawal.studioName.trim().take(120),
                    referenceNumber = withdrawal.referenceNumber.trim().take(100),
                    notes = withdrawal.notes.trim().take(1000),
                    createdAt = createdAt,
                    updatedAt = null
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteWithdrawal(withdrawalId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("withdrawals").document(withdrawalId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // -------------------------------------------------------------------------
    // 5. Expenses CRUD
    // -------------------------------------------------------------------------

    fun observeExpenses(userId: String = requireUserId()): Flow<List<Expense>> = flow {
        val path = "users/$userId/expenses"
        emitAll(
            db.collection("users").document(userId).collection("expenses")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Expense::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.dateEpochMs }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createExpense(expense: Expense): Result<String> = runCatching {
        val uid = requireUserId()
        require(expense.expenseName.isNotBlank()) { "Expense name is required" }
        require(expense.amountPaisa > 0L) { "Expense amount must be greater than NPR 0" }
        require(expense.paymentMethod in CineVaultConstants.PAYMENT_METHODS) {
            "Payment method must be Bank Transfer, eSewa, Cash, Khalti, or Other"
        }
        val colRef = db.collection("users").document(uid).collection("expenses")
        val docRef = if (expense.id.isNotBlank()) colRef.document(expense.id) else colRef.document()
        try {
            val payload = expense.copy(
                id = docRef.id,
                userId = uid,
                expenseName = expense.expenseName.trim().take(150),
                category = expense.category.takeIf { it in CineVaultConstants.EXPENSE_CATEGORIES } ?: "Other",
                associatedShootId = expense.associatedShootId.trim().take(128),
                associatedStudioId = expense.associatedStudioId.trim().take(128),
                receiptUri = expense.receiptUri.trim().take(500),
                notes = expense.notes.trim().take(1000),
                createdAt = null,
                updatedAt = null
            )
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateExpense(expense: Expense): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(expense.id.isNotBlank()) { "Expense ID is required" }
        require(expense.expenseName.isNotBlank()) { "Expense name is required" }
        require(expense.amountPaisa > 0L) { "Expense amount must be greater than NPR 0" }
        val docRef = db.collection("users").document(uid).collection("expenses").document(expense.id)
        try {
            val createdAt = expense.createdAt ?: docRef.get().await()
                .toObject(Expense::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.createdAt
            requireNotNull(createdAt) { "Expense createdAt is required for update" }
            docRef.set(
                expense.copy(
                    userId = uid,
                    expenseName = expense.expenseName.trim().take(150),
                    category = expense.category.takeIf { it in CineVaultConstants.EXPENSE_CATEGORIES } ?: "Other",
                    paymentMethod = expense.paymentMethod.takeIf { it in CineVaultConstants.PAYMENT_METHODS } ?: "Bank Transfer",
                    associatedShootId = expense.associatedShootId.trim().take(128),
                    associatedStudioId = expense.associatedStudioId.trim().take(128),
                    receiptUri = expense.receiptUri.trim().take(500),
                    notes = expense.notes.trim().take(1000),
                    createdAt = createdAt,
                    updatedAt = null
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteExpense(expenseId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("expenses").document(expenseId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // -------------------------------------------------------------------------
    // 6. Notifications & Account Purge
    // -------------------------------------------------------------------------

    fun observeNotifications(userId: String = requireUserId()): Flow<List<VaultNotification>> = flow {
        val path = "users/$userId/notifications"
        emitAll(
            db.collection("users").document(userId).collection("notifications")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(VaultNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.dueDateEpochMs }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createNotification(notification: VaultNotification): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("notifications")
        val docRef = if (notification.id.isNotBlank()) colRef.document(notification.id) else colRef.document()
        try {
            val payload = notification.copy(
                id = docRef.id,
                userId = uid,
                title = notification.title.trim().take(150),
                message = notification.message.trim().take(500),
                type = notification.type.takeIf { it in CineVaultConstants.NOTIFICATION_TYPES } ?: "PaymentReminder",
                relatedEntityId = notification.relatedEntityId.trim().take(128),
                createdAt = null,
                updatedAt = null
            )
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun purgeUserAccountData(): Result<Unit> = runCatching {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val subcollections = listOf("studios", "shoots", "withdrawals", "expenses", "notifications", "settings")
        try {
            for (sub in subcollections) {
                val snap = userRef.collection(sub).get().await()
                for (doc in snap.documents) {
                    doc.reference.delete().await()
                }
            }
            userRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, userRef.path)
            throw e
        }
    }
}
