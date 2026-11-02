package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.domain.model.Expense
import com.example.domain.model.Shoot
import com.example.domain.model.Studio
import com.example.domain.model.Withdrawal
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class CineVaultRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveStudioShootWithdrawalExpense_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser("alice_${UUID.randomUUID()}@cinevault.app")
        val repository = CineVaultRepository(firestore)

        // 1. Initialize user profile & settings (NPR)
        val profileRes = withTimeout(5000L) {
            repository.ensureUserInitialized("Alice Filmmaker", "alice@cinevault.app", "")
        }
        assertTrue("Profile init should succeed: ${profileRes.exceptionOrNull()}", profileRes.isSuccess)
        assertEquals("NPR", profileRes.getOrThrow().currency)

        // 2. Create Studio
        val studioRes = withTimeout(5000L) {
            repository.createStudio(
                Studio(
                    name = "Everest Cinema Studio",
                    contactPerson = "Bikash Gurung",
                    phone = "9801234567",
                    email = "info@everestcinema.np",
                    address = "Kathmandu"
                )
            )
        }
        assertTrue("Create studio should succeed: ${studioRes.exceptionOrNull()}", studioRes.isSuccess)
        val studioId = studioRes.getOrThrow()

        // 3. Create Completed Shoot
        val shootRes = withTimeout(5000L) {
            repository.createShoot(
                Shoot(
                    eventTitle = "Sita & Ram Wedding Film",
                    studioId = studioId,
                    studioName = "Everest Cinema Studio",
                    shootDateEpochMs = 1730000000000L,
                    category = "Wedding",
                    location = "Pokhara",
                    durationValue = 2L,
                    durationUnit = "Days",
                    confirmedEarningsPaisa = 5000000L, // NPR 50,000
                    expectedPaymentDateEpochMs = 1731000000000L,
                    paymentStatus = "Collectible",
                    projectStatus = "Completed"
                )
            )
        }
        assertTrue("Create shoot should succeed: ${shootRes.exceptionOrNull()}", shootRes.isSuccess)
        val shootId = shootRes.getOrThrow()

        // 4. Create Withdrawal (eSewa)
        val wdRes = withTimeout(5000L) {
            repository.createWithdrawal(
                Withdrawal(
                    studioId = studioId,
                    studioName = "Everest Cinema Studio",
                    amountPaisa = 2000000L, // NPR 20,000
                    dateEpochMs = 1730200000000L,
                    paymentMethod = "eSewa",
                    referenceNumber = "ESW-${UUID.randomUUID()}"
                )
            )
        }
        assertTrue("Create withdrawal should succeed: ${wdRes.exceptionOrNull()}", wdRes.isSuccess)

        // 5. Create Expense (Khalti)
        val expRes = withTimeout(5000L) {
            repository.createExpense(
                Expense(
                    expenseName = "Gimbal Assistant Fee",
                    category = "Assistant Payments",
                    amountPaisa = 500000L, // NPR 5,000
                    dateEpochMs = 1730000000000L,
                    associatedShootId = shootId,
                    associatedStudioId = studioId,
                    paymentMethod = "Khalti"
                )
            )
        }
        assertTrue("Create expense should succeed: ${expRes.exceptionOrNull()}", expRes.isSuccess)

        // 6. Verify studio deletion safeguard blocks deleting studio with financial records
        val deleteStudioRes = withTimeout(5000L) {
            repository.deleteStudioIfNoRecords(studioId)
        }
        assertTrue("Deleting studio with shoots/withdrawals should be blocked", deleteStudioRes.isFailure)

        // 7. Observe Studios realtime stream
        val studios = withTimeout(4000L) {
            repository.observeStudios(aliceUid).first { list -> list.any { it.id == studioId } }
        }
        assertTrue(studios.any { it.name == "Everest Cinema Studio" })
    }

    @Test
    fun crossUserRead_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_iso_${UUID.randomUUID()}@cinevault.app")
        val repository = CineVaultRepository(firestore)
        val studioId = withTimeout(5000L) {
            repository.createStudio(Studio(name = "Alice Private Studio")).getOrThrow()
        }

        // Switch to Bob
        signInTestUser("bob_iso_${UUID.randomUUID()}@cinevault.app")
        var caughtException: Exception? = null
        try {
            withTimeout(4000L) {
                repository.observeStudios(aliceUid).first()
            }
        } catch (e: Exception) {
            caughtException = e
        }
        assertNotNull("Cross-user studio observation must fail", caughtException)
        val fsEx = caughtException?.unwrapFirestoreException()
        assertNotNull("Expected wrapped FirebaseFirestoreException", fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        assertTrue(studioId.isNotBlank())
    }

    @Test
    fun unauthenticatedRead_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_unauth_${UUID.randomUUID()}@cinevault.app")
        auth.signOut()
        val repository = CineVaultRepository(firestore)

        var caughtException: Exception? = null
        try {
            withTimeout(4000L) {
                repository.observeStudios(aliceUid).first()
            }
        } catch (e: Exception) {
            caughtException = e
        }
        assertNotNull("Unauthenticated studio observation must fail", caughtException)
        val fsEx = caughtException?.unwrapFirestoreException()
        assertNotNull("Expected wrapped FirebaseFirestoreException", fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }
}
