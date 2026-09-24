package com.wakemyway.app.commerce

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommerceModelsTest {
    private val offer = CommerceOffer(
        productId = "wakemyway_pro",
        basePlanId = "monthly",
        offerId = null,
        offerToken = "offer-token",
        formattedPrice = "€4.99",
        billingPeriod = "P1M",
    )

    @Test
    fun `purchase cannot start without server verification readiness`() {
        val snapshot = CommerceSnapshot(
            availability = CommerceAvailability.READY,
            offers = listOf(offer),
            verificationAvailable = false,
        )

        assertFalse(snapshot.canStartPurchase)
    }

    @Test
    fun `pending and unverified purchased state never imply paid entitlement`() {
        val pending = CommerceSnapshot(
            availability = CommerceAvailability.READY,
            offers = listOf(offer),
            purchaseState = CommercePurchaseState.PENDING,
            entitlement = EntitlementState.FREE,
            verificationAvailable = true,
        )
        val purchasedButUnverified = pending.copy(
            purchaseState = CommercePurchaseState.PURCHASED,
            entitlement = EntitlementState.UNKNOWN,
        )

        assertFalse(pending.hasPaidEntitlement)
        assertFalse(pending.canStartPurchase)
        assertFalse(purchasedButUnverified.hasPaidEntitlement)
    }

    @Test
    fun `only verified entitled lifecycle states count as paid access`() {
        listOf(
            EntitlementState.ACTIVE,
            EntitlementState.CANCELLED_ENTITLED,
            EntitlementState.GRACE_PERIOD,
        ).forEach { state ->
            assertTrue(
                "Expected $state to retain paid entitlement",
                CommerceSnapshot(
                    availability = CommerceAvailability.READY,
                    entitlement = state,
                ).hasPaidEntitlement,
            )
        }

        listOf(
            EntitlementState.FREE,
            EntitlementState.ON_HOLD,
            EntitlementState.PAUSED,
            EntitlementState.EXPIRED,
            EntitlementState.UNKNOWN,
        ).forEach { state ->
            assertFalse(
                "Expected $state to have no paid entitlement",
                CommerceSnapshot(
                    availability = CommerceAvailability.READY,
                    entitlement = state,
                ).hasPaidEntitlement,
            )
        }
    }

    @Test
    fun `purchase verification request never prints the token`() {
        val request = PurchaseVerificationRequest(
            productId = "wakemyway_pro",
            purchaseToken = "secret-purchase-token",
            acknowledged = false,
        )

        assertFalse(request.toString().contains("secret-purchase-token"))
        assertTrue(request.toString().contains("<redacted>"))
    }
}
