package com.friendcall.app.data

/** The signed-in user, reduced to what the app needs. */
data class SessionUser(val uid: String, val email: String)

data class Friend(val uid: String, val email: String, val friendshipId: String)

data class FriendRequest(val id: String, val fromUid: String, val fromEmail: String)

data class OutgoingRequest(val id: String, val toEmail: String)

enum class MessageType(val wire: String) {
    TEXT("text"),
    CALL("call");

    companion object {
        fun fromWire(value: String?): MessageType = entries.firstOrNull { it.wire == value } ?: TEXT
    }
}

data class ChatMessage(
    val id: String,
    val senderUid: String,
    val senderEmail: String,
    val type: MessageType,
    val text: String,
    val room: String?,
    val createdAtMillis: Long,
)

/** Deterministic id shared by a friendship and its chat: the two uids, sorted. */
fun friendshipIdFor(a: String, b: String): String = listOf(a, b).sorted().joinToString("_")
