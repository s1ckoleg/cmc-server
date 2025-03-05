package com.cmc.models

import com.cmc.utils.BigDecimalSerializer
import com.cmc.utils.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDateTime

@Serializable
data class CoinStats(
    val id: Int = 0,
    val coinId: Int,
    @Serializable(with = BigDecimalSerializer::class)
    val currentPrice: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val marketCap: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val volume24h: BigDecimal? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val date: LocalDateTime = LocalDateTime.now()
) 