package com.tzuhsien.immediat.data.model

data class Invitation (
    val id: String = "",
    val note: Note = Note(),
    val inviterId: String = "",
    val inviteeId: String = "",
    val time: Long = 0L,
) {
    var inviter: UserInfo? = null
}