package com.jyotishapp.jyotishi.Models

import com.jyotishapp.jyotishi.Common.Constant
import com.jyotishapp.jyotishi.Common.Constant.DEFAULT_USER_URL

data class ChatUser(
    val username: String = "",
    val message: String = "",
    val profileUrl: String = DEFAULT_USER_URL,
    val time: String = ""
)