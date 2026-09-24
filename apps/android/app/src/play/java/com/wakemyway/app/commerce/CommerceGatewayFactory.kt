package com.wakemyway.app.commerce

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.wakemyway.app.BuildConfig

object CommerceGatewayFactory {
    fun create(
        context: Context,
        verifier: PurchaseVerifier? = null,
    ): CommerceGateway {
        val resolvedVerifier = verifier ?: PlayPurchaseVerifier.create(
            enabled = BuildConfig.PLAY_VERIFICATION_ENABLED,
            apiBaseUrl = BuildConfig.COMMERCE_API_BASE_URL,
        )
        return PlayBillingGateway(
            context = context.applicationContext,
            productId = BuildConfig.PRO_SUBSCRIPTION_PRODUCT_ID,
            verifier = resolvedVerifier,
        )
    }
}

private class PlayBillingGateway(
    context: Context,
    private val productId: String,
    private val verifier: PurchaseVerifier,
) : CommerceGateway, PurchasesUpdatedListener {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var listener: ((CommerceSnapshot) -> Unit)? = null
    private var started = false
    private var closed = false
    private var productDetails: ProductDetails? = null
    private var verificationTokenInFlight: String? = null
    private var snapshot = CommerceSnapshot(
        availability = if (productId.isBlank()) {
            CommerceAvailability.UNCONFIGURED
        } else {
            CommerceAvailability.CONNECTING
        },
        verificationAvailable = verifier.available,
    )

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        .enableAutoServiceReconnection()
        .build()

    override fun start(onSnapshot: (CommerceSnapshot) -> Unit) {
        listener = onSnapshot
        publish()

        if (closed || productId.isBlank() || started) return
        started = true
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (closed) return
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    updateSnapshot(
                        snapshot.copy(
                            availability = CommerceAvailability.READY,
                            lastFailureCode = null,
                        ),
                    )
                    refresh()
                } else {
                    updateFailure(billingResult)
                }
            }

            override fun onBillingServiceDisconnected() {
                if (closed) return
                updateSnapshot(
                    snapshot.copy(
                        availability = CommerceAvailability.CONNECTING,
                        lastFailureCode = "SERVICE_DISCONNECTED",
                    ),
                )
            }
        })
    }

    override fun refresh() {
        if (closed || productId.isBlank() || !billingClient.isReady) return
        queryProductDetails()
        queryPurchases()
    }

    override fun launchPurchase(
        activity: Activity,
        offerToken: String,
    ): CommerceLaunchResult {
        if (closed || productId.isBlank()) return CommerceLaunchResult.UNSUPPORTED
        if (!snapshot.canStartPurchase || !billingClient.isReady) {
            return CommerceLaunchResult.NOT_READY
        }

        val details = productDetails ?: return CommerceLaunchResult.OFFER_STALE
        val validOffer = details.subscriptionOfferDetails
            .orEmpty()
            .firstOrNull { it.offerToken == offerToken }
            ?: return CommerceLaunchResult.OFFER_STALE

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(validOffer.offerToken)
                        .build(),
                ),
            )
            .build()

        val result = billingClient.launchBillingFlow(activity, params)
        return if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            CommerceLaunchResult.LAUNCHED
        } else {
            updateFailure(result)
            CommerceLaunchResult.FAILED
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: List<Purchase>?,
    ) {
        if (closed) return
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> processPurchases(purchases.orEmpty())
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> updateFailure(billingResult)
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        listener = null
        verificationTokenInFlight = null
        billingClient.endConnection()
    }

    private fun queryProductDetails() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                ),
            )
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, result ->
            if (closed) return@queryProductDetailsAsync
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                updateFailure(billingResult)
                return@queryProductDetailsAsync
            }

            productDetails = result.productDetailsList
                .firstOrNull { it.productId == productId }

            val offers = productDetails
                ?.subscriptionOfferDetails
                .orEmpty()
                .mapNotNull { offer ->
                    val recurring = offer.pricingPhases.pricingPhaseList.lastOrNull()
                        ?: return@mapNotNull null
                    CommerceOffer(
                        productId = productId,
                        basePlanId = offer.basePlanId,
                        offerId = offer.offerId,
                        offerToken = offer.offerToken,
                        formattedPrice = recurring.formattedPrice,
                        billingPeriod = recurring.billingPeriod,
                    )
                }
                .distinctBy { it.offerToken }

            updateSnapshot(
                snapshot.copy(
                    availability = CommerceAvailability.READY,
                    offers = offers,
                    lastFailureCode = if (productDetails == null) {
                        "PRODUCT_NOT_AVAILABLE"
                    } else {
                        null
                    },
                ),
            )
        }
    }

    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .includeSuspendedSubscriptions(true)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (closed) return@queryPurchasesAsync
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                updateFailure(billingResult)
                return@queryPurchasesAsync
            }
            processPurchases(purchases)
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        val relevant = purchases.filter { productId in it.products }

        val purchase = relevant.firstOrNull { it.isSuspended }
            ?: relevant.firstOrNull { it.purchaseState == Purchase.PurchaseState.PENDING }
            ?: relevant.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }

        if (purchase == null) {
            verificationTokenInFlight = null
            updateSnapshot(
                snapshot.copy(
                    purchaseState = CommercePurchaseState.NONE,
                    entitlement = EntitlementState.FREE,
                    lastFailureCode = null,
                ),
            )
            return
        }

        val clientState = when {
            purchase.isSuspended -> CommercePurchaseState.SUSPENDED
            purchase.purchaseState == Purchase.PurchaseState.PENDING -> CommercePurchaseState.PENDING
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED -> CommercePurchaseState.PURCHASED
            else -> CommercePurchaseState.NONE
        }

        updateSnapshot(
            snapshot.copy(
                purchaseState = clientState,
                entitlement = if (clientState == CommercePurchaseState.PENDING) {
                    EntitlementState.FREE
                } else {
                    EntitlementState.UNKNOWN
                },
                lastFailureCode = null,
            ),
        )

        if (
            clientState == CommercePurchaseState.PURCHASED ||
            clientState == CommercePurchaseState.SUSPENDED
        ) {
            verify(purchase)
        }
    }

    private fun verify(purchase: Purchase) {
        if (!verifier.available || verificationTokenInFlight == purchase.purchaseToken) return
        verificationTokenInFlight = purchase.purchaseToken

        verifier.verify(
            PurchaseVerificationRequest(
                productId = productId,
                purchaseToken = purchase.purchaseToken,
                acknowledged = purchase.isAcknowledged,
            ),
        ) { verification ->
            mainHandler.post {
                if (closed || verificationTokenInFlight != purchase.purchaseToken) {
                    return@post
                }
                verificationTokenInFlight = null
                when (verification) {
                    is PurchaseVerificationResult.Verified -> {
                        updateSnapshot(
                            snapshot.copy(
                                entitlement = verification.entitlement,
                                lastFailureCode = null,
                            ),
                        )
                        if (
                            snapshot.hasPaidEntitlement &&
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                            !purchase.isAcknowledged
                        ) {
                            acknowledge(purchase)
                        }
                    }

                    PurchaseVerificationResult.Rejected -> {
                        updateSnapshot(
                            snapshot.copy(
                                entitlement = EntitlementState.FREE,
                                lastFailureCode = "VERIFICATION_REJECTED",
                            ),
                        )
                    }

                    PurchaseVerificationResult.RetryLater -> {
                        updateSnapshot(
                            snapshot.copy(
                                entitlement = EntitlementState.UNKNOWN,
                                lastFailureCode = "VERIFICATION_UNAVAILABLE",
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun acknowledge(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { billingResult ->
            if (closed) return@acknowledgePurchase
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                updateFailure(billingResult, prefix = "ACK")
            }
        }
    }

    private fun updateFailure(
        result: BillingResult,
        prefix: String = "BILLING",
    ) {
        val code = prefix + "_" + result.responseCode
        updateSnapshot(
            snapshot.copy(
                availability = if (billingClient.isReady) {
                    CommerceAvailability.READY
                } else {
                    CommerceAvailability.ERROR
                },
                lastFailureCode = code,
            ),
        )
    }

    private fun updateSnapshot(next: CommerceSnapshot) {
        snapshot = next.copy(verificationAvailable = verifier.available)
        publish()
    }

    private fun publish() {
        listener?.invoke(snapshot)
    }
}
