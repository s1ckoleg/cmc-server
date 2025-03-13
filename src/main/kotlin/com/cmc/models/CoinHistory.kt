package com.cmc.models

import com.cmc.utils.BigDecimalSerializer
import com.cmc.utils.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDateTime

@Serializable
data class CoinHistoryEntry(
    val date: @Serializable(with = LocalDateTimeSerializer::class) LocalDateTime,
    val price: @Serializable(with = BigDecimalSerializer::class) BigDecimal?,
    val marketCap: @Serializable(with = BigDecimalSerializer::class) BigDecimal?,
    val volume: @Serializable(with = BigDecimalSerializer::class) BigDecimal?
)

@Serializable
data class CoinHistoryResponse(
    val coin: Coin,
    val history: List<CoinHistoryEntry>
) 