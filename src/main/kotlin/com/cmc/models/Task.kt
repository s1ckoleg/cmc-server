package com.cmc.models

import kotlinx.serialization.Serializable

@Serializable
data class Task(
    val id: Int = 0,
    val title: String,
    val description: String,
    val completed: Boolean = false,
    val userId: Int? = null
) 