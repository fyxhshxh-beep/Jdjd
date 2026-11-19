package com.friendcall.app.call

import android.content.Context
import android.os.Bundle
import com.friendcall.app.BuildConfig
import org.jitsi.meet.sdk.JitsiMeet
import org.jitsi.meet.sdk.JitsiMeetActivity
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions
import org.jitsi.meet.sdk.JitsiMeetUserInfo
import java.net.URL

/** Opens the embedded Jitsi conference UI (voice, video and screen sharing). */
object CallLauncher {
    private const val ROOM_PREFIX = "FriendCall-"

    fun launch(context: Context, roomCode: String, userEmail: String?) {
        require(RoomCodes.isValid(roomCode)) { "Invalid room code" }

        JitsiMeet.setDefaultConferenceOptions(
            JitsiMeetConferenceOptions.Builder()
                .setServerURL(URL(BuildConfig.JITSI_SERVER_URL))
                .setFeatureFlag("welcomepage.enabled", false)
                .build(),
        )

        val displayName = userEmail?.substringBefore('@')?.takeIf { it.isNotBlank() }
        val options = JitsiMeetConferenceOptions.Builder()
            .setRoom(ROOM_PREFIX + roomCode)
            .setAudioOnly(false)
            .setAudioMuted(false)
            .setVideoMuted(false)
            .apply {
                if (displayName != null) {
                    setUserInfo(JitsiMeetUserInfo(Bundle().apply { putString("displayName", displayName) }))
                }
            }
            .build()

        JitsiMeetActivity.launch(context, options)
    }
}
