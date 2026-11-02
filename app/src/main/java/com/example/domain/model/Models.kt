package com.example.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

object CineVaultConstants {
    const val CURRENCY_NPR = "NPR"

    val SHOOT_CATEGORIES = listOf(
        "Wedding",
        "Commercial",
        "Music Video",
        "Corporate",
        "Pre-Wedding",
        "Documentary",
        "Other"
    )

    val DURATION_UNITS = listOf("Hours", "Days")

    val PAYMENT_STATUSES = listOf("Pending", "Collectible", "Overdue", "Settled")

    val PROJECT_STATUSES = listOf("Planned", "Completed")

    val PAYMENT_METHODS = listOf(
        "Bank Transfer",
        "eSewa",
        "Cash",
        "Khalti",
        "Other"
    )

    val EXPENSE_CATEGORIES = listOf(
        "Camera Equipment",
        "Lens Rental",
        "Lighting",
        "Assistant Payments",
        "Travel",
        "Accommodation",
        "Food",
        "Editing",
        "Equipment Maintenance",
        "Software Subscriptions",
        "Other"
    )

    val NOTIFICATION_TYPES = listOf(
        "PaymentReminder",
        "ScheduleConflict",
        "ShootUpcoming",
        "System"
    )
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val currency: String = CineVaultConstants.CURRENCY_NPR,
    val onboardingCompleted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserSettings(
    @DocumentId val id: String = "",
    val userId: String = "",
    val currency: String = CineVaultConstants.CURRENCY_NPR,
    val aiEnabled: Boolean = true,
    val appLockEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val paymentReminderDays: Long = 7L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Studio(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val contactPerson: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val notes: String = "",
    val archived: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Shoot(
    @DocumentId val id: String = "",
    val userId: String = "",
    val eventTitle: String = "",
    val studioId: String = "",
    val studioName: String = "",
    val shootDateEpochMs: Long = 0L,
    val category: String = "Wedding",
    val location: String = "",
    val durationValue: Long = 1L,
    val durationUnit: String = "Days",
    val confirmedEarningsPaisa: Long = 0L,
    val expectedPaymentDateEpochMs: Long = 0L,
    val paymentStatus: String = "Collectible",
    val projectStatus: String = "Completed",
    val notes: String = "",
    val attachmentUri: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Withdrawal(
    @DocumentId val id: String = "",
    val userId: String = "",
    val studioId: String = "",
    val studioName: String = "",
    val amountPaisa: Long = 0L,
    val dateEpochMs: Long = 0L,
    val paymentMethod: String = "Bank Transfer",
    val referenceNumber: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Expense(
    @DocumentId val id: String = "",
    val userId: String = "",
    val expenseName: String = "",
    val category: String = "Camera Equipment",
    val amountPaisa: Long = 0L,
    val dateEpochMs: Long = 0L,
    val associatedShootId: String = "",
    val associatedStudioId: String = "",
    val paymentMethod: String = "Bank Transfer",
    val receiptUri: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class VaultNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "PaymentReminder",
    val relatedEntityId: String = "",
    val dueDateEpochMs: Long = 0L,
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
