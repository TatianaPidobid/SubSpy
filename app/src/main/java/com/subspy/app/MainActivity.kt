package com.subspy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.subspy.app.ui.navigation.SubSpyNavigation
import com.subspy.app.ui.theme.SubSpyTheme
import com.subspy.app.viewmodel.AuthViewModel
import com.subspy.app.viewmodel.SubscriptionViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SubSpyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val authViewModel: AuthViewModel = hiltViewModel()
                    val subscriptionViewModel: SubscriptionViewModel = hiltViewModel()
                    val authState by authViewModel.authState.collectAsState()

                    SubSpyNavigation(
                        navController = navController,
                        authViewModel = authViewModel,
                        subscriptionViewModel = subscriptionViewModel,
                        authState = authState
                    )
                }
            }
        }
    }
}
