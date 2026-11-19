package com.friendcall.app.call

import java.security.SecureRandom

/** Room codes are the only thing protecting a public Jitsi room, so keep them long and random. */
object RoomCodes {
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // no look-alikes (0/O, 1/I)
    const val GENERATED_LENGTH = 16
    const val MIN_LENGTH = 8
    const val MAX_LENGTH = 32

    private val random = SecureRandom()

    fun generate(): String = buildString(GENERATED_LENGTH) {
        repeat(GENERATED_LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
    }

    /** Uppercases and keeps only A-Z / 0-9, capped at [MAX_LENGTH]. */
    fun sanitize(input: String): String =
        input.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }.take(MAX_LENGTH)

    fun isValid(code: String): Boolean =
        code.length in MIN_LENGTH..MAX_LENGTH && code.all { it in 'A'..'Z' || it in '0'..'9' }
}
