package com.jyotishapp.jyotishi.Common

object Constant {
    val listOfTimeSlots = listOf(
                                 "12 pm","12:30 pm","1 pm",
                                 "1:30 pm","4 pm","4:30 pm",
                                 "5 pm","5:30 pm","6 pm",
                                 "6:30 pm","7 pm","7:30 pm"
                                 )

    const val BROADCAST_NOTIFICATION_MESSAGE = "Jyotish ji is streaming live!"
    const val ADMIN_ID = "cAHmmp4bzyS9wPvm4QU2q6qthII2"

    private const val MAX_RANDOM_USER_ID_POSSIBLE = 101000
    const val ADMIN_UNIQUE_AGORA_ID = MAX_RANDOM_USER_ID_POSSIBLE.times(2)

    val DEFAULT_USER_URL = "https://firebasestorage.googleapis.com/v0/b/jyotishi-84a7a.appspot.com/o/default_user.png?alt=media&token=8d83cb0d-b8e8-44a0-9583-d83feb264be1"
}