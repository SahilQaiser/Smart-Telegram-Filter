package com.invictus.smarttelegramfilter

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.invictus.smarttelegramfilter.telegram.TelegramService
import com.invictus.smarttelegramfilter.ui.auth.AuthScreen
import com.invictus.smarttelegramfilter.ui.auth.AuthViewModel
import com.invictus.smarttelegramfilter.ui.feed.FeedScreen
import com.invictus.smarttelegramfilter.ui.feed.FeedViewModel
import com.invictus.smarttelegramfilter.ui.channelpicker.ChannelPickerScreen
import com.invictus.smarttelegramfilter.ui.channelpicker.ChannelPickerViewModel
import com.invictus.smarttelegramfilter.ui.filters.FiltersScreen
import com.invictus.smarttelegramfilter.ui.filters.FiltersViewModel
import com.invictus.smarttelegramfilter.ui.theme.SmartFilterTheme
import dagger.hilt.android.AndroidEntryPoint
import org.drinkless.tdlib.TdApi

private const val ROUTE_AUTH           = "auth"
private const val ROUTE_FEED           = "feed"
private const val ROUTE_FILTERS        = "filters"
private const val ROUTE_CHANNEL_PICKER = "channel_picker"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request POST_NOTIFICATIONS on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        TelegramService.start(this)

        setContent {
            SmartFilterTheme {
                val navController = rememberNavController()
                val authState by TelegramService.authState.collectAsStateWithLifecycle()

                // Route based on TDLib auth state
                LaunchedEffect(authState) {
                    when (authState) {
                        is TdApi.AuthorizationStateReady -> {
                            navController.navigate(ROUTE_FEED) {
                                popUpTo(ROUTE_AUTH) { inclusive = true }
                            }
                        }
                        is TdApi.AuthorizationStateWaitPhoneNumber,
                        is TdApi.AuthorizationStateWaitCode,
                        is TdApi.AuthorizationStateWaitPassword -> {
                            if (navController.currentDestination?.route != ROUTE_AUTH) {
                                navController.navigate(ROUTE_AUTH) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                        else -> Unit // WaitTdlibParameters / null — TDLib initializing
                    }
                }

                NavHost(
                    navController  = navController,
                    startDestination = ROUTE_AUTH,
                ) {
                    composable(ROUTE_AUTH) {
                        AuthScreen(viewModel = hiltViewModel<AuthViewModel>())
                    }
                    composable(ROUTE_FEED) {
                        FeedScreen(
                            viewModel           = hiltViewModel<FeedViewModel>(),
                            onNavigateToFilters = { navController.navigate(ROUTE_FILTERS) },
                        )
                    }
                    composable(ROUTE_FILTERS) {
                        FiltersScreen(
                            viewModel        = hiltViewModel<FiltersViewModel>(),
                            onBack           = { navController.popBackStack() },
                            onBrowseChannels = { navController.navigate(ROUTE_CHANNEL_PICKER) },
                        )
                    }
                    composable(ROUTE_CHANNEL_PICKER) {
                        ChannelPickerScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}
