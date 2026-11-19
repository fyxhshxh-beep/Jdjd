package com.friendcall.app

import com.friendcall.app.call.RoomCodes
import com.friendcall.app.data.MessageType
import com.friendcall.app.data.friendshipIdFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreLogicTest {
    @Test
    fun friendshipId_isSymmetric() {
        assertEquals(friendshipIdFor("bbb", "aaa"), friendshipIdFor("aaa", "bbb"))
        assertEquals("aaa_bbb", friendshipIdFor("bbb", "aaa"))
    }

    @Test
    fun generatedRoomCodes_areValidAndUnique() {
        val a = RoomCodes.generate()
        val b = RoomCodes.generate()
        assertEquals(RoomCodes.GENERATED_LENGTH, a.length)
        assertTrue(RoomCodes.isValid(a))
        assertNotEquals(a, b)
    }

    @Test
    fun sanitize_keepsOnlyUppercaseAsciiAlphanumerics() {
        assertEquals("ABC123", RoomCodes.sanitize("abc-12 3!"))
        assertEquals(RoomCodes.MAX_LENGTH, RoomCodes.sanitize("a".repeat(100)).length)
    }

    @Test
    fun shortOrInvalidCodes_areRejected() {
        assertFalse(RoomCodes.isValid("ABC"))
        assertFalse(RoomCodes.isValid("abcdefgh"))
        assertTrue(RoomCodes.isValid("ABCDEFGH"))
    }

    @Test
    fun messageType_fallsBackToText() {
        assertEquals(MessageType.CALL, MessageType.fromWire("call"))
        assertEquals(MessageType.TEXT, MessageType.fromWire("???"))
        assertEquals(MessageType.TEXT, MessageType.fromWire(null))
    }
}
