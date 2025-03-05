package com.cmc.models

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Int = 0,
    val username: String,
    val email: String,
    val passwordHash: String? = null // Not included in responses
) 