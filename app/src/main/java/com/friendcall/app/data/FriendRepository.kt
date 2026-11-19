package com.friendcall.app.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FriendRepository(
    db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val firestore = db
    private val users = db.collection("users")
    private val requests = db.collection("friendRequests")
    private val friendships = db.collection("friendships")

    fun friends(uid: String): Flow<List<Friend>> =
        friendships.whereArrayContains("members", uid).snapshotFlow().map { snapshot ->
            snapshot.documents.mapNotNull { doc ->
                val members = (doc.get("members") as? List<*>)?.filterIsInstance<String>()
                val emails = (doc.get("emails") as? List<*>)?.filterIsInstance<String>()
                if (members == null || emails == null || members.size != 2 || emails.size != 2) {
                    return@mapNotNull null
                }
                val me = members.indexOf(uid)
                if (me < 0) return@mapNotNull null
                Friend(uid = members[1 - me], email = emails[1 - me], friendshipId = doc.id)
            }.sortedBy { it.email }
        }

    fun incomingRequests(uid: String): Flow<List<FriendRequest>> =
        requests.whereEqualTo("toUid", uid).snapshotFlow().map { snapshot ->
            snapshot.documents.mapNotNull { doc ->
                val fromUid = doc.getString("fromUid") ?: return@mapNotNull null
                val fromEmail = doc.getString("fromEmail") ?: return@mapNotNull null
                FriendRequest(doc.id, fromUid, fromEmail)
            }.sortedBy { it.fromEmail }
        }

    fun outgoingRequests(uid: String): Flow<List<OutgoingRequest>> =
        requests.whereEqualTo("fromUid", uid).snapshotFlow().map { snapshot ->
            snapshot.documents.mapNotNull { doc ->
                val toEmail = doc.getString("toEmail") ?: return@mapNotNull null
                OutgoingRequest(doc.id, toEmail)
            }.sortedBy { it.toEmail }
        }

    /** Looks up an account by email and sends it a friend request. */
    suspend fun sendRequest(me: SessionUser, rawEmail: String) {
        val targetEmail = rawEmail.trim().lowercase()
        if (targetEmail == me.email) throw FriendCallException("That's your own email address.")

        val targetUid = users.whereEqualTo("email", targetEmail).limit(1).get()
            .awaitWithTimeout().documents.firstOrNull()?.id
            ?: throw FriendCallException("No FriendCall account found for that email yet.")

        try {
            requests.document("${me.uid}_$targetUid").set(
                mapOf(
                    "fromUid" to me.uid,
                    "fromEmail" to me.email,
                    "toUid" to targetUid,
                    "toEmail" to targetEmail,
                    "status" to "pending",
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            ).awaitWithTimeout()
        } catch (e: FirebaseFirestoreException) {
            // Re-sending over an existing request is an update, which the rules deny.
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                throw FriendCallException("You already have a pending request to this person.", e)
            }
            throw e
        }
    }

    /** Accepting creates the friendship and removes the request in one atomic batch. */
    suspend fun accept(me: SessionUser, request: FriendRequest) {
        val members = listOf(me.uid, request.fromUid).sorted()
        val emails = members.map { if (it == me.uid) me.email else request.fromEmail }
        val batch = firestore.batch()
        batch.set(
            friendships.document(friendshipIdFor(me.uid, request.fromUid)),
            mapOf(
                "members" to members,
                "emails" to emails,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        )
        batch.delete(requests.document(request.id))
        batch.commit().awaitWithTimeout()
    }

    /** Used both to decline an incoming request and to cancel one you sent. */
    suspend fun deleteRequest(requestId: String) {
        requests.document(requestId).delete().awaitWithTimeout()
    }

    suspend fun removeFriend(friend: Friend) {
        friendships.document(friend.friendshipId).delete().awaitWithTimeout()
    }
}
