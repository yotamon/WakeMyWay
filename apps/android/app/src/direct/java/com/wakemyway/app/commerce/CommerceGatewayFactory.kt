package com.wakemyway.app.commerce

import android.app.Activity
import android.content.Context

object CommerceGatewayFactory {
    fun create(
        context: Context,
        verifier: PurchaseVerifier? = null,
    ): CommerceGateway = DirectCommerceGateway
}

private object DirectCommerceGateway : CommerceGateway {
    override fun start(onSnapshot: (CommerceSnapshot) -> Unit) {
        onSnapshot(
            CommerceSnapshot(
                availability = CommerceAvailability.UNAVAILABLE,
                entitlement = EntitlementState.FREE,
            ),
        )
    }

    override fun refresh() = Unit

    override fun launchPurchase(
        activity: Activity,
        offerToken: String,
    ): CommerceLaunchResult = CommerceLaunchResult.UNSUPPORTED

    override fun close() = Unit
}
