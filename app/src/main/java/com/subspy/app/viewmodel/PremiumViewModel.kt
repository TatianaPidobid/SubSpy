package com.subspy.app.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.subspy.app.billing.BillingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val billingManager: BillingManager
) : ViewModel() {

    val isPremium: StateFlow<Boolean> = billingManager.isPremium

    fun purchase(activity: Activity) {
        billingManager.launchPurchaseFlow(activity)
    }

    fun restore() {
        billingManager.restorePurchases()
    }
}
