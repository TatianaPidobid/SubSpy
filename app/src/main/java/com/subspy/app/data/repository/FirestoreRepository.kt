package com.subspy.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.subspy.app.data.model.Subscription
import com.subspy.app.data.model.UserProfile
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val userId: String
        get() = auth.currentUser?.uid ?: ""

    private fun subscriptionsCollection() =
        firestore.collection("users").document(userId).collection("subscriptions")

    private fun userDocument() =
        firestore.collection("users").document(userId)

    suspend fun saveSubscriptions(subscriptions: List<Subscription>) {
        val batch = firestore.batch()
        subscriptions.forEach { sub ->
            val docRef = subscriptionsCollection().document(sub.id.ifBlank { sub.serviceName.hashCode().toString() })
            batch.set(docRef, sub.copy(userId = userId))
        }
        batch.commit().await()
    }

    suspend fun getSubscriptions(): List<Subscription> {
        return try {
            subscriptionsCollection()
                .whereEqualTo("active", true)
                .get()
                .await()
                .toObjects(Subscription::class.java)
        } catch (e: Exception) {
            subscriptionsCollection()
                .get()
                .await()
                .toObjects(Subscription::class.java)
        }
    }

    suspend fun deleteSubscription(subscriptionId: String) {
        subscriptionsCollection().document(subscriptionId).delete().await()
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        userDocument().set(profile).await()
    }

    suspend fun getUserProfile(): UserProfile? {
        return try {
            userDocument().get().await().toObject(UserProfile::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateNotificationSettings(
        threeDaysBefore: Boolean,
        sevenDaysBefore: Boolean
    ) {
        userDocument().update(
            mapOf(
                "notifyThreeDaysBefore" to threeDaysBefore,
                "notifySevenDaysBefore" to sevenDaysBefore
            )
        ).await()
    }

    suspend fun updatePremiumStatus(isPremium: Boolean) {
        userDocument().update("isPremium", isPremium).await()
    }
}
