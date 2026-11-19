package com.friendcall.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.friendcall.app.call.CallLauncher
import com.friendcall.app.ui.screens.AuthScreen
import com.friendcall.app.ui.screens.ChatScreen
import com.friendcall.app.ui.screens.HomeScreen

/** Root composable: picks the screen from the current state. */
@Composable
fun FriendCallApp(vm: MainViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(vm) {
        vm.events.collect { message ->
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message)
        }
    }

    val joinCall: (String) -> Unit = { room ->
        try {
            CallLauncher.launch(context, room, state.user?.email)
        } catch (e: Exception) {
            vm.showMessage("Couldn't start the call. Please try again.")
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        val openChat = state.openChat
        when {
            state.authLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.user == null -> AuthScreen(
                busy = state.busy,
                snackbar = snackbar,
                onSignIn = vm::signIn,
                onSignUp = vm::signUp,
                onResetPassword = vm::resetPassword,
            )
            openChat != null -> ChatScreen(
                state = state,
                friend = openChat,
                snackbar = snackbar,
                onBack = vm::closeChat,
                onSend = vm::sendMessage,
                onStartCall = { vm.startCallInChat()?.let(joinCall) },
                onJoinCall = joinCall,
            )
            else -> HomeScreen(
                state = state,
                vm = vm,
                snackbar = snackbar,
                onJoinCall = joinCall,
            )
        }
    }
}
