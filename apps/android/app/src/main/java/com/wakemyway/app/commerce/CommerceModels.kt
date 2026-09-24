package com.wakemyway.app.commerce

import android.app.Activity

enum class CommerceAvailability {
    UNAVAILABLE,
    UNCONFIGURED,
    CONNECTING,
    READY,
    ERROR,
}

enum class CommercePurchaseState {
    NONE,
    PENDING,
    PURCHASED,
    SUSPENDED,
}

enum class EntitlementState {
    FREE,
    ACTIVE,
    CANCELLED_ENTITLED,
    GRACE_PERIOD,
    ON_HOLD,
    PAUSED,
    EXPIRED,
    UNKNOWN,
}

data class CommerceOffer(
    val productId: String,
    val basePlanId: String,
    val offerId: String?,
    val offerToken: String,
    val formattedPrice: String,
    val billingPeriod: String?,
)

data class CommerceSnapshot(
    val availability: CommerceAvailability,
    val offers: List<CommerceOffer> = emptyList(),
    val purchaseState: CommercePurchaseState = CommercePurchaseState.NONE,
    val entitlement: EntitlementState = EntitlementState.FREE,
    val verificationAvailable: Boolean = false,
    val lastFailureCode: String? = null,
) {
    val hasPaidEntitlement: Boolean
        get() = entitlement in setOf(
            EntitlementState.ACTIVE,
            EntitlementState.CANCELLED_ENTITLED,
            EntitlementState.GRACE_PERIOD,
        )

    val canStartPurchase: Boolean
        get() = availability == CommerceAvailability.READY &&
            offers.isNotEmpty() &&
            verificationAvailable &&
            !hasPaidEntitlement &&
            purchaseState != CommercePurchaseState.PENDING
}

enum class CommerceLaunchResult {
    LAUNCHED,
    NOT_READY,
    OFFER_STALE,
    UNSUPPORTED,
    FAILED,
}

/**
 * Non-authoritative commerce boundary.
 *
 * Commerce may decide whether a non-critical premium experience is available. It must never be
 * consulted by AlarmKernel, WakeRuntime, Stop/Snooze handling or committed local wake delivery.
 */
interface CommerceGateway : AutoCloseable {
    fun start(onSnapshot: (CommerceSnapshot) -> Unit)

    fun refresh()

    fun launchPurchase(
        activity: Activity,
        offerToken: String,
    ): CommerceLaunchResult

    override fun close()
}

class PurchaseVerificationRequest(
    val productId: String,
    val purchaseToken: String,
    val acknowledged: Boolean,
) {
    override fun toString(): String =
        "PurchaseVerificationRequest(productId=$productId, purchaseToken=<redacted>, acknowledged=$acknowledged)"
}

sealed interface PurchaseVerificationResult {
    data class Verified(
        val entitlement: EntitlementState,
    ) : PurchaseVerificationResult

    data object RetryLater : PurchaseVerificationResult

    data object Rejected : PurchaseVerificationResult
}

interface PurchaseVerifier {
    val available: Boolean

    fun verify(
        request: PurchaseVerificationRequest,
        onResult: (PurchaseVerificationResult) -> Unit,
    )

    companion object {
        val UNAVAILABLE = object : PurchaseVerifier {
            override val available: Boolean = false

            override fun verify(
                request: PurchaseVerificationRequest,
                onResult: (PurchaseVerificationResult) -> Unit,
            ) {
                onResult(PurchaseVerificationResult.RetryLater)
            }
        }
    }
}
