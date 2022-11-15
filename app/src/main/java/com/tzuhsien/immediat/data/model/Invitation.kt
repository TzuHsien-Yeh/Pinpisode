package com.tzuhsien.immediat.data.model

data class Invitation (
    val id: String,
    val note: Note,
    val inviterId: String,
    val inviteeId: String,
    val time: Long,
) {
    var inviter: UserInfo? = null
}