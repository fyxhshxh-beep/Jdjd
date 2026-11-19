@file:OptIn(ExperimentalMaterial3Api::class)

package com.friendcall.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.friendcall.app.call.RoomCodes
import com.friendcall.app.data.Friend
import com.friendcall.app.data.FriendRequest
import com.friendcall.app.data.OutgoingRequest
import com.friendcall.app.ui.AppUiState
import com.friendcall.app.ui.HomeTab
import com.friendcall.app.ui.MainViewModel

@Composable
fun HomeScreen(
    state: AppUiState,
    vm: MainViewModel,
    snackbar: SnackbarHostState,
    onJoinCall: (String) -> Unit,
) {
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("FriendCall", fontWeight = FontWeight.Bold)
                        Text(
                            state.user?.email.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { confirmSignOut = true }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            NavigationBar {
                HomeTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = state.tab == tab,
                        onClick = { vm.selectTab(tab) },
                        icon = { Icon(tab.icon(), contentDescription = null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            when (state.tab) {
                HomeTab.FRIENDS -> FriendsTab(state, vm)
                HomeTab.CALL -> CallTab(onJoinCall)
            }
        }
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out?") },
            text = { Text("You'll need to sign in again to see your friends and chats.") },
            confirmButton = {
                TextButton(onClick = { confirmSignOut = false; vm.signOut() }) { Text("Sign out") }
            },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") } },
        )
    }
}

private fun HomeTab.icon(): ImageVector = when (this) {
    HomeTab.FRIENDS -> Icons.Filled.People
    HomeTab.CALL -> Icons.Filled.Call
}

// ---------------------------------------------------------------- friends tab

@Composable
private fun FriendsTab(state: AppUiState, vm: MainViewModel) {
    var friendToRemove by remember { mutableStateOf<Friend?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { AddFriendCard(busy = state.busy, onSend = vm::sendFriendRequest) }

        if (state.incoming.isNotEmpty()) {
            item { SectionTitle("Friend requests") }
            items(state.incoming, key = { "in_" + it.id }) { request ->
                IncomingRequestRow(
                    request = request,
                    onAccept = { vm.acceptRequest(request) },
                    onDecline = { vm.declineRequest(request) },
                )
            }
        }

        if (state.outgoing.isNotEmpty()) {
            item { SectionTitle("Sent requests") }
            items(state.outgoing, key = { "out_" + it.id }) { request ->
                OutgoingRequestRow(request = request, onCancel = { vm.cancelRequest(request) })
            }
        }

        item { SectionTitle("Your friends") }
        if (state.friends.isEmpty()) {
            item {
                Text(
                    "No friends yet. Send a request using the email of someone who has a FriendCall account.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(state.friends, key = { it.friendshipId }) { friend ->
                FriendRow(
                    friend = friend,
                    onMessage = { vm.openChat(friend) },
                    onRemove = { friendToRemove = friend },
                )
            }
        }
    }

    friendToRemove?.let { friend ->
        AlertDialog(
            onDismissRequest = { friendToRemove = null },
            title = { Text("Remove friend?") },
            text = { Text("${friend.email} will be removed and your chat will no longer be accessible to either of you.") },
            confirmButton = {
                TextButton(onClick = { friendToRemove = null; vm.removeFriend(friend) }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { friendToRemove = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 6.dp),
    )
}

@Composable
private fun AddFriendCard(busy: Boolean, onSend: (email: String, onSuccess: () -> Unit) -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    val canSend = email.isNotBlank() && !busy
    val send = { if (canSend) onSend(email) { email = "" } }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Add a friend", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Friend's email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = { send() }, enabled = canSend, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null)
                Text("  Send friend request")
            }
        }
    }
}

@Composable
private fun IncomingRequestRow(request: FriendRequest, onAccept: () -> Unit, onDecline: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(request.fromEmail, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            IconButton(onClick = onDecline) { Icon(Icons.Filled.Close, contentDescription = "Decline") }
            IconButton(onClick = onAccept) {
                Icon(Icons.Filled.Check, contentDescription = "Accept", tint = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun OutgoingRequestRow(request: OutgoingRequest, onCancel: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(request.toEmail, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Waiting for a reply", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}

@Composable
private fun FriendRow(friend: Friend, onMessage: () -> Unit, onRemove: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(friend.email, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            IconButton(onClick = onMessage) { Icon(Icons.Filled.ChatBubble, contentDescription = "Message ${friend.email}") }
            IconButton(onClick = onRemove) { Icon(Icons.Filled.Delete, contentDescription = "Remove ${friend.email}") }
        }
    }
}

// ------------------------------------------------------------------ call tab

@Composable
private fun CallTab(onJoinCall: (String) -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val valid = RoomCodes.isValid(code)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Start or join a call", style = MaterialTheme.typography.titleLarge)
        Text(
            "Anyone with the same room code joins the same call. Generate a code, share it, then join. " +
                "You can also start a call straight from a chat.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = code,
            onValueChange = { code = RoomCodes.sanitize(it) },
            label = { Text("Room code") },
            singleLine = true,
            isError = code.isNotEmpty() && !valid,
            supportingText = {
                if (code.isNotEmpty() && !valid) Text("At least ${RoomCodes.MIN_LENGTH} letters or digits")
            },
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedButton(onClick = { code = RoomCodes.generate() }, modifier = Modifier.fillMaxWidth()) {
            Text("Generate a private room code")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { clipboard.setText(AnnotatedString(code)) },
                enabled = valid,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null)
                Text("  Copy")
            }
            OutlinedButton(
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Join my FriendCall call. Room code: $code")
                    }
                    context.startActivity(Intent.createChooser(send, "Share room code"))
                },
                enabled = valid,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Text("  Share")
            }
        }

        Button(onClick = { onJoinCall(code) }, enabled = valid, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Videocam, contentDescription = null)
            Text("  Join voice / video call")
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Screen sharing", style = MaterialTheme.typography.titleMedium)
                Text(
                    "During a call open More, then Share your screen. Android will ask for permission; " +
                        "choose Start now. Use the call controls to stop sharing.",
                )
                Text(
                    "For the clearest text and smoothest motion, use a stable Wi-Fi connection and keep your phone charged.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}
