package com.friendcall.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friendcall.app.call.RoomCodes
import com.friendcall.app.data.AuthRepository
import com.friendcall.app.data.ChatMessage
import com.friendcall.app.data.ChatRepository
import com.friendcall.app.data.Friend
import com.friendcall.app.data.FriendRepository
import com.friendcall.app.data.FriendRequest
import com.friendcall.app.data.OutgoingRequest
import com.friendcall.app.data.SessionUser
import com.friendcall.app.data.friendlyMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update

enum class HomeTab(val label: String) { FRIENDS("Friends"), CALL("Call") }

data class AppUiState(
    val authLoading: Boolean = true,
    val user: SessionUser? = null,
    val busy: Boolean = false,
    val tab: HomeTab = HomeTab.FRIENDS,
    val friends: List<Friend> = emptyList(),
    val incoming: List<FriendRequest> = emptyList(),
    val outgoing: List<OutgoingRequest> = emptyList(),
    val openChat: Friend? = null,
    val messages: List<ChatMessage> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val authRepo: AuthRepository = AuthRepository(),
    private val friendRepo: FriendRepository = FriendRepository(),
    private val chatRepo: ChatRepository = ChatRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 16)

    /** One-off messages for a snackbar. */
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val userFlow: Flow<SessionUser?> = _state.map { it.user }.distinctUntilChanged()

    init {
        viewModelScope.launch {
            authRepo.sessionUser.collect { user ->
                _state.update {
                    if (user == null) {
                        AppUiState(authLoading = false)
                    } else {
                        it.copy(authLoading = false, user = user)
                    }
                }
            }
        }

        collectPerUser(
            source = { friendRepo.friends(it.uid) },
            onValue = { list ->
                _state.update { s ->
                    // If the friend you were chatting with was removed, leave the chat.
                    val stillFriends = s.openChat == null || list.any { it.uid == s.openChat.uid }
                    s.copy(
                        friends = list,
                        openChat = if (stillFriends) s.openChat else null,
                        messages = if (stillFriends) s.messages else emptyList(),
                    )
                }
            },
        )
        collectPerUser(
            source = { friendRepo.incomingRequests(it.uid) },
            onValue = { list -> _state.update { it.copy(incoming = list) } },
        )
        collectPerUser(
            source = { friendRepo.outgoingRequests(it.uid) },
            onValue = { list -> _state.update { it.copy(outgoing = list) } },
        )

        viewModelScope.launch {
            _state.map { it.user to it.openChat?.friendshipId }
                .distinctUntilChanged()
                .flatMapLatest { (user, chatId) ->
                    if (user == null || chatId == null) {
                        emptyFlow<List<ChatMessage>>()
                    } else {
                        chatRepo.messages(chatId)
                            .onStart { _state.update { it.copy(messages = emptyList()) } }
                            .catch { reportFlowError(it) }
                    }
                }
                .collect { list -> _state.update { it.copy(messages = list) } }
        }
    }

    /** Collects [source] for whichever user is signed in, restarting on user change. */
    private fun <T> collectPerUser(
        source: (SessionUser) -> Flow<List<T>>,
        onValue: (List<T>) -> Unit,
    ) {
        viewModelScope.launch {
            userFlow.flatMapLatest { user ->
                if (user == null) {
                    emptyFlow<List<T>>()
                } else {
                    source(user).catch { reportFlowError(it) }
                }
            }.collect { onValue(it) }
        }
    }

    private fun reportFlowError(error: Throwable) {
        // Listeners fail with "permission denied" as a sign-out races in; stay quiet then.
        if (_state.value.user != null) _events.tryEmit(error.friendlyMessage())
    }

    // ------------------------------------------------------------ navigation

    fun selectTab(tab: HomeTab) = _state.update { it.copy(tab = tab) }

    fun openChat(friend: Friend) = _state.update { it.copy(openChat = friend) }

    fun closeChat() = _state.update { it.copy(openChat = null, messages = emptyList()) }

    fun showMessage(message: String) {
        _events.tryEmit(message)
    }

    // ------------------------------------------------------------------ auth

    fun signIn(email: String, password: String) =
        runAction { authRepo.signIn(email, password) }

    fun signUp(email: String, password: String) = runAction(
        successMessage = "Account created. Check your inbox to verify your email.",
    ) { authRepo.signUp(email, password) }

    fun resetPassword(email: String) = runAction(
        successMessage = "If an account exists for that email, a reset link is on its way.",
    ) { authRepo.sendPasswordReset(email) }

    fun signOut() = authRepo.signOut()

    // --------------------------------------------------------------- friends

    fun sendFriendRequest(email: String, onSuccess: () -> Unit = {}) {
        val me = _state.value.user ?: return
        val target = email.trim().lowercase()
        val s = _state.value
        when {
            s.friends.any { it.email == target } -> showMessage("You're already friends.")
            s.incoming.any { it.fromEmail == target } ->
                showMessage("They already sent you a request. Accept it below.")
            s.outgoing.any { it.toEmail == target } -> showMessage("Request already sent.")
            else -> runAction(successMessage = "Friend request sent.", onSuccess = onSuccess) {
                friendRepo.sendRequest(me, target)
            }
        }
    }

    fun acceptRequest(request: FriendRequest) {
        val me = _state.value.user ?: return
        runAction(successMessage = "You and ${request.fromEmail} are now friends.") {
            friendRepo.accept(me, request)
        }
    }

    fun declineRequest(request: FriendRequest) = runAction { friendRepo.deleteRequest(request.id) }

    fun cancelRequest(request: OutgoingRequest) = runAction { friendRepo.deleteRequest(request.id) }

    fun removeFriend(friend: Friend) =
        runAction(successMessage = "Removed ${friend.email}.") { friendRepo.removeFriend(friend) }

    // ------------------------------------------------------------------ chat

    fun sendMessage(text: String) {
        val s = _state.value
        val me = s.user ?: return
        val chat = s.openChat ?: return
        if (text.isBlank()) return
        chatRepo.sendText(me, chat.friendshipId, text)
            .addOnFailureListener { _events.tryEmit("Message not sent: ${it.friendlyMessage()}") }
    }

    /** Posts a call invite in the open chat and returns the room code to join right away. */
    fun startCallInChat(): String? {
        val s = _state.value
        val me = s.user ?: return null
        val chat = s.openChat ?: return null
        val room = RoomCodes.generate()
        chatRepo.sendCallInvite(me, chat.friendshipId, room)
            .addOnFailureListener { _events.tryEmit("Couldn't send the call invite: ${it.friendlyMessage()}") }
        return room
    }

    // --------------------------------------------------------------- helpers

    private fun runAction(
        successMessage: String? = null,
        onSuccess: () -> Unit = {},
        block: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                block()
                onSuccess()
                successMessage?.let { _events.tryEmit(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.tryEmit(e.friendlyMessage())
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }
}
