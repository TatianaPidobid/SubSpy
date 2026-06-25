package com.subspy.app.viewmodel

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.gmail.GmailScopes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subspy.app.billing.BillingManager
import com.subspy.app.data.model.Subscription
import com.subspy.app.data.repository.FirestoreRepository
import com.subspy.app.data.repository.GmailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gmailRepository: GmailRepository,
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

                val scannedSubscriptions = gmailRepository.scanEmails(credential)

                if (scannedSubscriptions.isNotEmpty()) {
                    firestoreRepository.saveSubscriptions(scannedSubscriptions)
                    updateSubscriptionData(scannedSubscriptions)
                    _uiState.value = SubscriptionUiState.Success
                } else {
                    _uiState.value = SubscriptionUiState.Empty
                }
            } catch (e: Exception) {
                _uiState.value = SubscriptionUiState.Error(
                    e.message ?: "Failed to scan emails"
                )
            }
        }
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
