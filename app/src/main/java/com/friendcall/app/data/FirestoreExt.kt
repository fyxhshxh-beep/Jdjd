package com.friendcall.app.data

import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/** An error whose message is safe and useful to show to the user as-is. */
class FriendCallException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Live query results; the flow fails with the Firestore error if the listener is rejected. */
fun Query.snapshotFlow(): Flow<QuerySnapshot> = callbackFlow {
    val registration = this@snapshotFlow.addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(error)
        } else if (snapshot != null) {
            trySend(snapshot)
        }
    }
    awaitClose { registration.remove() }
}

/** Awaits a Firebase task but gives up with a friendly error instead of hanging offline. */
suspend fun <T> Task<T>.awaitWithTimeout(timeoutMs: Long = 20_000): T =
    try {
        withTimeout(timeoutMs) { await() }
    } catch (e: TimeoutCancellationException) {
        throw FriendCallException("The request timed out. Check your connection and try again.", e)
    }

/** Maps technical exceptions to short messages for the UI. */
fun Throwable.friendlyMessage(): String = when (this) {
    is FriendCallException -> message ?: "Something went wrong."
    is FirebaseAuthException -> when (errorCode) {
        "ERROR_INVALID_CREDENTIAL",
        "ERROR_INVALID_LOGIN_CREDENTIALS",
        "ERROR_WRONG_PASSWORD",
        "ERROR_USER_NOT_FOUND" -> "Incorrect email or password."
        "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with this email already exists."
        "ERROR_WEAK_PASSWORD" -> "Choose a stronger password (at least 6 characters)."
        "ERROR_INVALID_EMAIL" -> "That email address doesn't look right."
        "ERROR_USER_DISABLED" -> "This account has been disabled."
        "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Please wait a bit and try again."
        else -> localizedMessage ?: "Authentication failed."
    }
    is FirebaseNetworkException -> "No internet connection."
    is FirebaseFirestoreException -> when (code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> "You don't have permission to do that."
        FirebaseFirestoreException.Code.UNAVAILABLE -> "Can't reach the server. Check your connection."
        else -> localizedMessage ?: "Something went wrong."
    }
    else -> localizedMessage ?: "Something went wrong."
}
