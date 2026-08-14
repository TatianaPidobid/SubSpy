package com.subspy.app.data.model

/**
 * Everything needed to walk a user through cancelling one service:
 * a direct cancellation page, what to tap on that page, and where to
 * complain if the page did not work.
 */
data class CancellationInfo(
    val serviceName: String,
    val steps: List<String>,
    val cancellationUrl: String,
    /** What to tap once the cancellation page is open. */
    val tapHint: String = "",
    /** Support email, when the service publishes one. */
    val supportEmail: String = "",
    /** Support/contact page, used when there is no support email. */
    val supportUrl: String = ""
)

object CancellationDatabase {
    private val cancellationData = mapOf(
        "netflix" to CancellationInfo(
            serviceName = "Netflix",
            steps = listOf(
                "Go to netflix.com and sign in",
                "Click your profile icon in the top right",
                "Select 'Account'",
                "Click 'Cancel Membership'",
                "Confirm cancellation"
            ),
            cancellationUrl = "https://www.netflix.com/cancelplan",
            tapHint = "\"Finish Cancellation\"",
            supportUrl = "https://help.netflix.com/contactus"
        ),
        "spotify" to CancellationInfo(
            serviceName = "Spotify",
            steps = listOf(
                "Go to spotify.com/account",
                "Sign in to your account",
                "Scroll to 'Your plan'",
                "Click 'Change plan'",
                "Select 'Cancel Premium'",
                "Confirm cancellation"
            ),
            cancellationUrl = "https://www.spotify.com/account/subscription/",
            tapHint = "\"Cancel Premium\"",
            supportUrl = "https://support.spotify.com/contact-spotify-anonymous/"
        ),
        "amazon prime" to CancellationInfo(
            serviceName = "Amazon Prime",
            steps = listOf(
                "Go to amazon.com and sign in",
                "Go to 'Account & Lists' > 'Your Prime Membership'",
                "Click 'Update, cancel and more'",
                "Click 'End membership'",
                "Confirm cancellation"
            ),
            cancellationUrl = "https://www.amazon.com/gp/primecentral",
            tapHint = "\"End membership\"",
            supportUrl = "https://www.amazon.com/gp/help/customer/contact-us"
        ),
        "adobe" to CancellationInfo(
            serviceName = "Adobe",
            steps = listOf(
                "Go to account.adobe.com/plans",
                "Sign in to your Adobe account",
                "Open your plan",
                "Click 'Cancel your plan'",
                "Confirm cancellation"
            ),
            cancellationUrl = "https://account.adobe.com/plans",
            tapHint = "\"Cancel your plan\"",
            supportUrl = "https://helpx.adobe.com/contact.html"
        ),
        "youtube" to CancellationInfo(
            serviceName = "YouTube Premium",
            steps = listOf(
                "Go to youtube.com/paid_memberships",
                "Sign in to your account",
                "Click 'Manage membership'",
                "Click 'Deactivate'",
                "Select 'Cancel' and confirm"
            ),
            cancellationUrl = "https://www.youtube.com/paid_memberships",
            tapHint = "\"Deactivate\"",
            supportUrl = "https://support.google.com/youtube"
        ),
        "apple tv" to CancellationInfo(
            serviceName = "Apple TV+",
            steps = listOf(
                "Open apps.apple.com/account/subscriptions",
                "Sign in with your Apple ID",
                "Select the Apple TV+ subscription",
                "Click 'Cancel Subscription'",
                "Confirm cancellation"
            ),
            cancellationUrl = "https://apps.apple.com/account/subscriptions",
            tapHint = "\"Cancel Subscription\"",
            supportUrl = "https://getsupport.apple.com"
        ),
        "apple" to CancellationInfo(
            serviceName = "Apple",
            steps = listOf(
                "Open apps.apple.com/account/subscriptions",
                "Sign in with your Apple ID",
                "Select the subscription to cancel",
                "Click 'Cancel Subscription'",
                "Confirm cancellation"
            ),
            cancellationUrl = "https://apps.apple.com/account/subscriptions",
            tapHint = "\"Cancel Subscription\"",
            supportUrl = "https://getsupport.apple.com"
        ),
        "disney" to CancellationInfo(
            serviceName = "Disney+",
            steps = listOf(
                "Go to disneyplus.com and sign in",
                "Click your profile avatar",
                "Select 'Account'",
                "Click your subscription",
                "Click 'Cancel Subscription' and confirm"
            ),
            cancellationUrl = "https://www.disneyplus.com/account/subscription",
            tapHint = "\"Cancel Subscription\"",
            supportUrl = "https://help.disneyplus.com"
        ),
        "hulu" to CancellationInfo(
            serviceName = "Hulu",
            steps = listOf(
                "Go to hulu.com and sign in",
                "Click your profile name",
                "Select 'Account'",
                "Click 'Cancel Your Subscription'",
                "Confirm cancellation"
            ),
            cancellationUrl = "https://secure.hulu.com/account",
            tapHint = "\"Cancel Your Subscription\"",
            supportUrl = "https://help.hulu.com"
        ),
        "duolingo" to CancellationInfo(
            serviceName = "Duolingo",
            steps = listOf(
                "Go to duolingo.com/settings/duolingo-plus",
                "Sign in to your account",
                "Click 'Manage subscription'",
                "Click 'Cancel subscription'",
                "Confirm cancellation"
            ),
            cancellationUrl = "https://www.duolingo.com/settings/duolingo-plus",
            tapHint = "\"Cancel subscription\"",
            supportUrl = "https://support.duolingo.com"
        ),
        "dropbox" to CancellationInfo(
            serviceName = "Dropbox",
            steps = listOf(
                "Go to dropbox.com/account/plan",
                "Sign in to your account",
                "Click 'Cancel plan'",
                "Choose 'Cancel plan' again",
                "Confirm cancellation"
            ),
            cancellationUrl = "https://www.dropbox.com/account/plan",
            tapHint = "\"Cancel plan\"",
            supportUrl = "https://www.dropbox.com/support"
        ),
        "google one" to CancellationInfo(
            serviceName = "Google One",
            steps = listOf(
                "Go to one.google.com/settings",
                "Sign in with your Google account",
                "Click 'Cancel subscription'",
                "Confirm cancellation"
            ),
            cancellationUrl = "https://one.google.com/settings",
            tapHint = "\"Cancel subscription\"",
            supportUrl = "https://support.google.com/googleone"
        ),
        "microsoft" to CancellationInfo(
            serviceName = "Microsoft 365",
            steps = listOf(
                "Go to account.microsoft.com/services",
                "Sign in with your Microsoft account",
                "Find Microsoft 365 and click 'Manage'",
                "Click 'Cancel subscription'",
                "Confirm cancellation"
            ),
            cancellationUrl = "https://account.microsoft.com/services",
            tapHint = "\"Cancel subscription\"",
            supportUrl = "https://support.microsoft.com/contactus"
        )
    )

    fun getCancellationInfo(serviceName: String): CancellationInfo {
        val key = serviceName.lowercase()
        return cancellationData.entries.firstOrNull { key.contains(it.key) }?.value
            ?: CancellationInfo(
                serviceName = serviceName,
                steps = listOf(
                    "Go to the service's website",
                    "Log in to your account",
                    "Navigate to Account Settings or Billing",
                    "Find the cancellation or unsubscribe option",
                    "Confirm cancellation"
                ),
                cancellationUrl = ""
            )
    }
}
