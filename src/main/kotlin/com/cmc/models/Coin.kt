package com.cmc.models

import kotlinx.serialization.Serializable

@Serializable
data class Coin(
    val id: Int = 0,
    val ticker: String,
    val name: String
) 