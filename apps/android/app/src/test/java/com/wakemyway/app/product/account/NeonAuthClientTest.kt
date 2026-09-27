package com.wakemyway.app.product.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NeonAuthClientTest {
    @Test
    fun extractsManagedAuthSessionCookieFromSetCookieHeaders() {
        val cookie = NeonAuthClient.extractSessionCookie(
            listOf(
                "__Secure-neon-auth.session_data=cache; Path=/; Secure; HttpOnly",
                "__Secure-neon-auth.session_token=signed.value; Path=/; Secure; HttpOnly; SameSite=Lax",
            ),
        )

        assertEquals("__Secure-neon-auth.session_token=signed.value", cookie)
    }

    @Test
    fun ignoresSessionDataAndClearedSessionCookies() {
        assertNull(
            NeonAuthClient.extractSessionCookie(
                listOf(
                    "__Secure-neon-auth.session_data=cache; Path=/; Secure; HttpOnly",
                    "__Secure-neon-auth.session_token=; Path=/; Max-Age=0; Secure; HttpOnly",
                ),
            ),
        )
    }

    @Test
    fun handlesFoldedSetCookieHeaderWithoutTreatingExpiresAsCookie() {
        val cookie = NeonAuthClient.extractSessionCookie(
            listOf(
                "other=value; Expires=Wed, 21 Oct 2026 07:28:00 GMT, " +
                    "__Secure-neon-auth.session_token=abc123; Path=/; Secure; HttpOnly",
            ),
        )

        assertEquals("__Secure-neon-auth.session_token=abc123", cookie)
    }
}
