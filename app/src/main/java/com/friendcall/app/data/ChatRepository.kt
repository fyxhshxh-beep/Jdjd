package com.friendcall.app.data

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private fun messagesOf(friendshipId: String) =
        db.collection("chats").document(friendshipId).collection("messages")

    /** The most recent messages, oldest first. */
    fun messages(friendshipId: String): Flow<List<ChatMessage>> =
        messagesOf(friendshipId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limitToLast(PAGE_SIZE)
            .snapshotFlow()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    ChatMessage(
                        id = doc.id,
                        senderUid = doc.getString("senderUid") ?: return@mapNotNull null,
                        senderEmail = doc.getString("senderEmail").orEmpty(),
                        type = MessageType.fromWire(doc.getString("type")),
                        text = doc.getString("text").orEmpty(),
                        room = doc.getString("room"),
                        // A just-sent message has no server time yet; estimate it locally.
                        createdAtMillis = doc.getDate(
                            "createdAt",
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                        )?.time ?: System.currentTimeMillis(),
                    )
                }
            }

    /** Queued locally right away (works offline); the returned task reports server failures. */
    fun sendText(me: SessionUser, friendshipId: String, text: String): Task<DocumentReference> =
        messagesOf(friendshipId).add(
            baseMessage(me, MessageType.TEXT, text.trim().take(MAX_MESSAGE_LENGTH)),
        )

    fun sendCallInvite(me: SessionUser, friendshipId: String, room: String): Task<DocumentReference> =
        messagesOf(friendshipId).add(
            baseMessage(me, MessageType.CALL, "Started a call") + ("room" to room),
        )

    private fun baseMessage(me: SessionUser, type: MessageType, text: String): Map<String, Any> = mapOf(
        "senderUid" to me.uid,
        "senderEmail" to me.email,
        "type" to type.wire,
        "text" to text,
        "createdAt" to FieldValue.serverTimestamp(),
    )

    companion object {
        const val MAX_MESSAGE_LENGTH = 2000
        private const val PAGE_SIZE = 300L
    }
}
