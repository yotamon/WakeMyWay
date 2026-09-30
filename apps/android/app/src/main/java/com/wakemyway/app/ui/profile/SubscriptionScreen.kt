package com.wakemyway.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.wakemyway.app.commerce.CommerceAvailability
import com.wakemyway.app.commerce.CommerceLaunchResult
import com.wakemyway.app.commerce.CommerceOffer
import com.wakemyway.app.commerce.CommercePurchaseState
import com.wakemyway.app.commerce.CommerceSnapshot
import com.wakemyway.app.commerce.EntitlementState
import com.wakemyway.app.ui.components.WmwCard
import com.wakemyway.app.ui.components.WmwCircadianStage
import com.wakemyway.app.ui.components.WmwCircadianSurface
import com.wakemyway.app.ui.theme.WmwColors
import com.wakemyway.app.ui.theme.WmwSpacing

@Composable
fun SubscriptionScreen(
    snapshot: CommerceSnapshot,
    onPurchase: (CommerceOffer) -> CommerceLaunchResult,
    onRestore: () -> Unit,
    onManage: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var actionMessage by remember { mutableStateOf<String?>(null) }
    val launchOffers = remember(snapshot.offers) {
        snapshot.offers.filter { offer -> offer.offerId == null }
    }

    WmwCircadianSurface(WmwCircadianStage.PLANNING, modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = WmwSpacing.Lg)
                .padding(top = WmwSpacing.Md, bottom = WmwSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(WmwSpacing.Md),
        ) {
            BackHeader("WakeMyWay Pro", onBack)
            Text(
                text = "Billing is handled by Google Play. Subscription state never controls whether a committed alarm rings.",
                style = MaterialTheme.typography.bodyLarge,
                color = WmwColors.LightQuietText,
            )

            SubscriptionStateCard(snapshot)

            if (snapshot.canStartPurchase) {
                launchOffers.forEach { offer ->
                    WmwCard(onLightSurface = true) {
                        Column(verticalArrangement = Arrangement.spacedBy(WmwSpacing.Xs)) {
                            Text(
                                text = offer.title?.takeIf(String::isNotBlank) ?: "WakeMyWay Pro",
                                style = MaterialTheme.typography.titleMedium,
                                color = WmwColors.Midnight,
                            )
                            offer.description?.takeIf(String::isNotBlank)?.let { description ->
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = WmwColors.LightQuietText,
                                )
                            }
                            Text(
                                text = priceLabel(offer),
                                style = MaterialTheme.typography.titleLarge,
                                color = WmwColors.Midnight,
                            )
                            Button(
                                onClick = {
                                    actionMessage = purchaseMessage(onPurchase(offer))
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Continue with Google Play")
                            }
                        }
                    }
                }
            }

            if (
                snapshot.availability == CommerceAvailability.READY &&
                launchOffers.isEmpty() &&
                snapshot.purchaseState == CommercePurchaseState.NONE &&
                !snapshot.hasPaidEntitlement
            ) {
                Text(
                    text = "No eligible Google Play plan is available for this account or region right now.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WmwColors.LightQuietText,
                )
            }

            if (snapshot.purchaseState == CommercePurchaseState.PENDING) {
                Text(
                    text = "Your purchase is pending in Google Play. WakeMyWay will unlock paid access only after Play completes and verifies it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WmwColors.LightQuietText,
                )
            }

            TextButton(
                onClick = {
                    actionMessage = null
                    onRestore()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Restore purchases")
            }

            if (snapshot.subscriptionProductId != null && snapshot.purchaseState != CommercePurchaseState.NONE) {
                TextButton(
                    onClick = onManage,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Manage or cancel in Google Play")
                }
            }

            actionMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = WmwColors.LightQuietText,
                )
            }
        }
    }
}

@Composable
private fun SubscriptionStateCard(snapshot: CommerceSnapshot) {
    val (title, body) = when {
        snapshot.hasPaidEntitlement -> "Pro is active" to entitlementCopy(snapshot.entitlement)
        snapshot.purchaseState == CommercePurchaseState.PENDING ->
            "Purchase pending" to "Google Play has not completed this purchase yet."
        snapshot.purchaseState == CommercePurchaseState.PURCHASED ->
            "Verifying purchase" to "WakeMyWay is waiting for Google Play verification before granting paid access."
        snapshot.purchaseState == CommercePurchaseState.SUSPENDED ->
            "Subscription needs attention" to entitlementCopy(snapshot.entitlement)
        snapshot.lastFailureCode == "PRODUCT_NOT_AVAILABLE" ->
            "Plan unavailable" to "Google Play is not returning a launch-ready base plan for this account or region."
        snapshot.availability == CommerceAvailability.CONNECTING ->
            "Checking Google Play" to "Loading your current plan and eligible offers."
        snapshot.availability == CommerceAvailability.ERROR ->
            "Billing temporarily unavailable" to "Your alarms are unaffected. Try restore again later."
        else -> "Free plan" to "Your reliable local alarm remains available without a subscription."
    }

    WmwCard(onLightSurface = true) {
        Column(verticalArrangement = Arrangement.spacedBy(WmwSpacing.Xs)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = WmwColors.Midnight)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = WmwColors.LightQuietText)
        }
    }
}

private fun entitlementCopy(state: EntitlementState): String = when (state) {
    EntitlementState.ACTIVE -> "Verified by Google Play."
    EntitlementState.CANCELLED_ENTITLED -> "Canceled, with access retained through the paid period."
    EntitlementState.GRACE_PERIOD -> "Google Play is retrying payment and access remains active."
    EntitlementState.ON_HOLD -> "Payment is on hold. Manage the subscription in Google Play."
    EntitlementState.PAUSED -> "The subscription is paused in Google Play."
    EntitlementState.EXPIRED -> "The subscription has expired."
    EntitlementState.UNKNOWN -> "WakeMyWay is waiting for verified subscription state."
    EntitlementState.FREE -> "No verified paid subscription is active."
}

private fun priceLabel(offer: CommerceOffer): String {
    val period = offer.billingPeriod?.let(::billingPeriodLabel)
    return if (period == null) offer.formattedPrice else "${offer.formattedPrice} / $period"
}

private fun billingPeriodLabel(raw: String): String? {
    val match = Regex("^P(\\d+)([DWMY])$").matchEntire(raw) ?: return null
    val count = match.groupValues[1].toIntOrNull() ?: return null
    if (count <= 0) return null
    val unit = when (match.groupValues[2]) {
        "D" -> "day"
        "W" -> "week"
        "M" -> "month"
        "Y" -> "year"
        else -> return null
    }
    return if (count == 1) unit else "$count ${unit}s"
}

private fun purchaseMessage(result: CommerceLaunchResult): String? = when (result) {
    CommerceLaunchResult.LAUNCHED -> null
    CommerceLaunchResult.NOT_READY -> "Google Play is still loading this offer. Try again in a moment."
    CommerceLaunchResult.OFFER_STALE -> "This offer changed in Google Play. Restore or reopen this screen."
    CommerceLaunchResult.UNSUPPORTED -> "Purchases are not available in this build."
    CommerceLaunchResult.FAILED -> "Google Play could not start the purchase. Your alarms are unaffected."
}
