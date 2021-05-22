package com.jyotishapp.jyotishi.Models

import com.jyotishapp.jyotishi.Common.Constant
import com.jyotishapp.jyotishi.Common.Constant.DEFAULT_USER_URL

data class ActiveUser(
    val username: String = "",
    val profileUrl: String = DEFAULT_USER_URL
)