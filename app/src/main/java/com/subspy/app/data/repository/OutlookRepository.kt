package com.subspy.app.data.repository

import android.app.Activity
import android.content.Context
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SignInParameters
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException
import com.subspy.app.R
import com.subspy.app.data.detection.PaymentEvent
import com.subspy.app.data.detection.SubscriptionDetector
import com.subspy.app.data.detection.SubscriptionSource
import com.subspy.app.data.model.Subscription
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Reads billing/receipt emails from Outlook / Microsoft accounts through the
 * Microsoft Graph API. Sign-in uses MSAL; requires an Azure app registration
 * whose client id is placed in res/raw/msal_config.json.
 */
@Singleton
class OutlookRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scopes = listOf("Mail.Read")
    private val authority = "https://login.microsoftonline.com/common"

    class NotConfiguredException :
        Exception("Outlook is not configured yet. Add your Azure client id to msal_config.json.")

    private suspend fun createApp(): ISingleAccountPublicClientApplication =
        suspendCancellableCoroutine { cont ->
            PublicClientApplication.createSingleAccountPublicClientApplication(
                context,
                R.raw.msal_config,
                object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                    override fun onCreated(application: ISingleAccountPublicClientApplication) {
                        cont.resume(application)
                    }

                    override fun onError(exception: MsalException) {
                        cont.resumeWithException(exception)
                    }
                }
            )
        }

    private suspend fun currentAccount(app: ISingleAccountPublicClientApplication): IAccount? =
        suspendCancellableCoroutine { cont ->
            app.getCurrentAccountAsync(object :
                ISingleAccountPublicClientApplication.CurrentAccountCallback {
                override fun onAccountLoaded(activeAccount: IAccount?) {
                    cont.resume(activeAccount)
                }

                override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
                    // no-op
                }

                override fun onError(exception: MsalException) {
                    cont.resumeWithException(exception)
                }
            })
        }

    private suspend fun signIn(
        app: ISingleAccountPublicClientApplication,
        activity: Activity
    ): IAuthenticationResult = suspendCancellableCoroutine { cont ->
        val params = SignInParameters.builder()
            .withActivity(activity)
            .withScopes(scopes)
            .withCallback(object : AuthenticationCallback {
                override fun onSuccess(authenticationResult: IAuthenticationResult) {
                    cont.resume(authenticationResult)
                }

                override fun onError(exception: MsalException) {
                    cont.resumeWithException(exception)
                }

                override fun onCancel() {
                    cont.resumeWithException(Exception("Sign-in cancelled"))
                }
            })
            .build()
        app.signIn(params)
    }

    private suspend fun acquireSilent(
        app: ISingleAccountPublicClientApplication,
        account: IAccount
    ): IAuthenticationResult = suspendCancellableCoroutine { cont ->
        val params = com.microsoft.identity.client.AcquireTokenSilentParameters.Builder()
            .forAccount(account)
            .fromAuthority(authority)
            .withScopes(scopes)
            .withCallback(object : SilentAuthenticationCallback {
                override fun onSuccess(authenticationResult: IAuthenticationResult) {
                    cont.resume(authenticationResult)
                }

                override fun onError(exception: MsalException) {
                    cont.resumeWithException(exception)
                }
            })
            .build()
        app.acquireTokenSilentAsync(params)
    }

    /** Signs in (or reuses an account) and scans Outlook mail for subscriptions. */
    suspend fun signInAndScan(activity: Activity): List<Subscription> {
        val app = try {
            createApp()
        } catch (e: Exception) {
            throw NotConfiguredException()
        }

        val existing = try {
            currentAccount(app)
        } catch (e: Exception) {
            null
        }

        val result = if (existing != null) {
            try {
                acquireSilent(app, existing)
            } catch (e: Exception) {
                signIn(app, activity)
            }
        } else {
            signIn(app, activity)
        }

        val token = result.accessToken
        return withContext(Dispatchers.IO) { scanMailWithToken(token) }
    }

    private fun scanMailWithToken(accessToken: String): List<Subscription> {
        val search = URLEncoder.encode("\"payment OR receipt OR subscription OR invoice OR renewal\"", "UTF-8")
        val select = URLEncoder.encode("subject,bodyPreview,receivedDateTime,from", "UTF-8")
        val url = URL(
            "https://graph.microsoft.com/v1.0/me/messages?\$top=100&\$select=$select&\$search=$search"
        )

        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Accept", "application/json")
            connectTimeout = 30000
            readTimeout = 30000
        }

        val body = try {
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                return emptyList()
            }
        } finally {
            conn.disconnect()
        }

        val events = mutableListOf<PaymentEvent>()
        val messages = JSONObject(body).optJSONArray("value") ?: return emptyList()
        for (i in 0 until messages.length()) {
            val msg = messages.optJSONObject(i) ?: continue
            val subject = msg.optString("subject")
            val preview = msg.optString("bodyPreview")
            val text = "$subject $preview".trim()
            if (text.isBlank()) continue

            val (amount, currency) = SubscriptionDetector.extractAmountAndCurrency(text) ?: continue

            val fromName = msg.optJSONObject("from")
                ?.optJSONObject("emailAddress")
                ?.let { it.optString("name").ifBlank { it.optString("address") } }
                ?: ""
            val merchant = SubscriptionDetector.extractMerchant(text, fromName) ?: continue

            val received = msg.optString("receivedDateTime")
            val date = try {
                LocalDate.parse(received.take(10))
            } catch (e: Exception) {
                LocalDate.now()
            }

            events.add(
                PaymentEvent(
                    merchantKey = merchant.lowercase(),
                    displayName = merchant.replaceFirstChar { it.uppercase() },
                    amount = amount,
                    currency = currency,
                    date = date,
                    source = SubscriptionSource.OUTLOOK,
                    rawSender = fromName
                )
            )
        }

        return SubscriptionDetector.detectSubscriptions(events)
    }
}
