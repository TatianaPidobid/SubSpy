package com.subspy.app.viewmodel

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.gmail.GmailScopes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subspy.app.billing.BillingManager
import com.subspy.app.data.detection.SubscriptionDetector
import com.subspy.app.data.detection.SubscriptionSource
import com.subspy.app.data.model.BillingFrequency
import com.subspy.app.data.model.Subscription
import com.subspy.app.data.model.SubscriptionCategory
import com.subspy.app.data.model.UsageStatus
import com.subspy.app.data.repository.FirestoreRepository
import android.app.Activity
import com.subspy.app.data.repository.GmailRepository
import com.subspy.app.data.repository.NotificationRepository
import com.subspy.app.data.repository.OutlookRepository
import com.subspy.app.data.repository.SmsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gmailRepository: GmailRepository,
    private val smsRepository: SmsRepository,
    private val notificationRepository: NotificationRepository,
    private val outlookRepository: OutlookRepository,
    private val firestoreRepository: FirestoreRepository,
    private val billingManager: BillingManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<SubscriptionUiState>(SubscriptionUiState.Loading)
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    private val _subscriptions = MutableStateFlow<List<Subscription>>(emptyList())
    val subscriptions: StateFlow<List<Subscription>> = _subscriptions.asStateFlow()

    private val _totalMonthlySpend = MutableStateFlow(0.0)
    val totalMonthlySpend: StateFlow<Double> = _totalMonthlySpend.asStateFlow()

    val isPremium: StateFlow<Boolean> = billingManager.isPremium

    companion object {
        const val FREE_SUBSCRIPTION_LIMIT = 3
    }

    init {
        loadCachedSubscriptions()
    }

    private fun loadCachedSubscriptions() {
        viewModelScope.launch {
            try {
                val cached = firestoreRepository.getSubscriptions()
                if (cached.isNotEmpty()) {
                    updateSubscriptionData(cached)
                    _uiState.value = SubscriptionUiState.Success
                } else {
                    _uiState.value = SubscriptionUiState.Empty
                }
            } catch (e: Exception) {
                _uiState.value = SubscriptionUiState.Empty
            }
        }
    }

    fun scanEmails() {
        viewModelScope.launch {
            try {
                _uiState.value = SubscriptionUiState.Scanning

                val account = GoogleSignIn.getLastSignedInAccount(context)
                if (account?.account == null) {
                    _uiState.value = SubscriptionUiState.Error("Please sign in again")
                    return@launch
                }

                val credential = GoogleAccountCredential.usingOAuth2(
                    context, listOf(GmailScopes.GMAIL_READONLY)
                )
                credential.selectedAccount = account.account

                val scanned = gmailRepository.scanEmails(credential)
                mergeAndPersist(scanned)
            } catch (e: Exception) {
                _uiState.value = SubscriptionUiState.Error(
                    e.message ?: "Failed to scan emails"
                )
            }
        }
    }

    /** Scans the device SMS inbox. Caller must ensure READ_SMS is granted. */
    fun scanSms() {
        viewModelScope.launch {
            try {
                _uiState.value = SubscriptionUiState.Scanning
                val scanned = smsRepository.scanSms()
                mergeAndPersist(scanned)
            } catch (e: SecurityException) {
                _uiState.value = SubscriptionUiState.Error("SMS permission is required to scan messages")
            } catch (e: Exception) {
                _uiState.value = SubscriptionUiState.Error(e.message ?: "Failed to scan SMS")
            }
        }
    }

    /** Detects subscriptions from bank push notifications captured in the background. */
    fun scanNotifications() {
        viewModelScope.launch {
            try {
                _uiState.value = SubscriptionUiState.Scanning
                val scanned = notificationRepository.scanNotifications()
                mergeAndPersist(scanned)
            } catch (e: Exception) {
                _uiState.value = SubscriptionUiState.Error(e.message ?: "Failed to scan notifications")
            }
        }
    }

    /** Signs into a Microsoft/Outlook account and scans its mail for subscriptions. */
    fun scanOutlook(activity: Activity) {
        viewModelScope.launch {
            try {
                _uiState.value = SubscriptionUiState.Scanning
                val scanned = outlookRepository.signInAndScan(activity)
                mergeAndPersist(scanned)
            } catch (e: OutlookRepository.NotConfiguredException) {
                _uiState.value = SubscriptionUiState.Error("Outlook is not set up yet")
            } catch (e: Exception) {
                _uiState.value = SubscriptionUiState.Error(e.message ?: "Failed to scan Outlook")
            }
        }
    }

    fun addManualSubscription(
        serviceName: String,
        amount: Double,
        currency: String,
        frequency: BillingFrequency,
        nextBillingDate: String,
        websiteUrl: String = "",
        category: SubscriptionCategory? = null
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = SubscriptionUiState.Scanning
                val name = serviceName.trim()
                val subscription = Subscription(
                    id = "manual_${System.currentTimeMillis()}",
                    serviceName = name,
                    amount = amount,
                    currency = currency,
                    frequency = frequency,
                    nextBillingDate = nextBillingDate.ifBlank {
                        LocalDate.now().plusMonths(1).toString()
                    },
                    firstPaymentDate = LocalDate.now().toString(),
                    lastPaymentDate = LocalDate.now().toString(),
                    websiteUrl = websiteUrl.trim(),
                    isForgotten = false,
                    isActive = true,
                    source = SubscriptionSource.MANUAL,
                    category = category ?: SubscriptionDetector.categorize(name)
                )
                mergeAndPersist(listOf(subscription))
            } catch (e: Exception) {
                _uiState.value = SubscriptionUiState.Error(e.message ?: "Failed to add subscription")
            }
        }
    }

    /** Marks how actively the user uses a subscription and persists the change. */
    fun setUsage(id: String, status: UsageStatus) {
        val updated = _subscriptions.value.map {
            if (it.id == id) it.copy(usage = status) else it
        }
        _subscriptions.value = updated
        viewModelScope.launch {
            updated.find { it.id == id }?.let { sub ->
                try {
                    firestoreRepository.updateSubscription(sub)
                } catch (_: Exception) {
                }
            }
        }
    }

    /** Marks a subscription as cancelled: it stops counting towards spending. */
    fun markCancelled(id: String) {
        val target = _subscriptions.value.find { it.id == id } ?: return
        val cancelled = target.copy(isActive = false, usage = UsageStatus.NOT_USED)
        _subscriptions.value = _subscriptions.value.map {
            if (it.id == id) cancelled else it
        }
        _totalMonthlySpend.value =
            (_totalMonthlySpend.value - target.monthlyAmount).coerceAtLeast(0.0)
        viewModelScope.launch {
            try {
                firestoreRepository.updateSubscription(cancelled)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Merges freshly detected subscriptions with what is already stored (keyed by
     * id, new entries win), persists the union, and refreshes the UI. This lets
     * multiple sources (Gmail, SMS, manual) contribute without overwriting each
     * other.
     */
    private suspend fun mergeAndPersist(newSubs: List<Subscription>) {
        val existing = try {
            firestoreRepository.getSubscriptions()
        } catch (e: Exception) {
            emptyList()
        }

        val merged = (existing + newSubs)
            .associateBy { it.id }
            .values
            .toList()
            .sortedByDescending { it.amount }

        if (merged.isEmpty()) {
            _uiState.value = SubscriptionUiState.Empty
            return
        }

        try {
            firestoreRepository.saveSubscriptions(merged)
        } catch (_: Exception) {
        }
        updateSubscriptionData(merged)
        _uiState.value = SubscriptionUiState.Success
    }

    private fun updateSubscriptionData(subs: List<Subscription>) {
        val displaySubs = if (isPremium.value) {
            subs
        } else {
            subs.take(FREE_SUBSCRIPTION_LIMIT)
        }
        _subscriptions.value = displaySubs
        _totalMonthlySpend.value = subs.sumOf { it.monthlyAmount }
    }

    fun getSubscriptionById(id: String): Subscription? {
        return _subscriptions.value.find { it.id == id }
    }

    fun refreshSubscriptions() {
        scanEmails()
    }
}

sealed class SubscriptionUiState {
    data object Loading : SubscriptionUiState()
    data object Scanning : SubscriptionUiState()
    data object Success : SubscriptionUiState()
    data object Empty : SubscriptionUiState()
    data class Error(val message: String) : SubscriptionUiState()
}
