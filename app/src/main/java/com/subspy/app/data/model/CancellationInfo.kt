package com.subspy.app.data.model

data class CancellationInfo(
    val serviceName: String,
    val steps: List<String>,
    val cancellationUrl: String
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
            cancellationUrl = "https://www.netflix.com/cancelplan"
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
            cancellationUrl = "https://www.spotify.com/account/subscription/"
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
            cancellationUrl = "https://www.amazon.com/mc/pipelines/cancel"
        ),
        "apple" to CancellationInfo(
            serviceName = "Apple",
            steps = listOf(
                "Open Settings on your iPhone/iPad",
                "Tap your name at the top",
                "Tap 'Subscriptions'",
                "Select the subscription to cancel",
                "Tap 'Cancel Subscription'"
            ),
            cancellationUrl = "https://support.apple.com/en-us/HT202039"
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
            cancellationUrl = "https://www.youtube.com/paid_memberships"
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
            cancellationUrl = "https://secure.hulu.com/account"
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
            cancellationUrl = "https://www.disneyplus.com/account/subscription"
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
