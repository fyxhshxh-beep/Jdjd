package com.friendcall.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    /** Emits the current user (or null) now and whenever the sign-in state changes. */
    val sessionUser: Flow<SessionUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.map { it?.toSessionUser() }.distinctUntilChanged()

    suspend fun signIn(email: String, password: String) {
        val user = auth.signInWithEmailAndPassword(email.trim(), password).awaitWithTimeout().user
        if (user != null) ensureProfile(user)
    }

    suspend fun signUp(email: String, password: String) {
        val user = auth.createUserWithEmailAndPassword(email.trim(), password).awaitWithTimeout().user
        if (user != null) {
            ensureProfile(user)
            // Best effort; the account works without a verified email.
            runCatching { user.sendEmailVerification() }
        }
    }

    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email.trim()).awaitWithTimeout()
    }

    fun signOut() = auth.signOut()

    /** Keeps the public lookup record (uid + lowercase email) in sync. Failure is non-fatal. */
    private suspend fun ensureProfile(user: FirebaseUser) {
        runCatching {
            db.collection("users").document(user.uid).set(
                mapOf("uid" to user.uid, "email" to user.email.orEmpty().lowercase()),
                SetOptions.merge(),
            ).awaitWithTimeout()
        }
    }

    private fun FirebaseUser.toSessionUser() = SessionUser(uid, email.orEmpty().lowercase())
}
