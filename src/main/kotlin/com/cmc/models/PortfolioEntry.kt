package com.cmc.models

import com.cmc.utils.BigDecimalSerializer
import com.cmc.utils.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDateTime

@Serializable
data class PortfolioEntry(
    val id: Int = 0,
    val userId: Int,
    val cryptoId: Int,
    @Serializable(with = BigDecimalSerializer::class)
    val quantity: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val entryPrice: BigDecimal,
    @Serializable(with = LocalDateTimeSerializer::class)
    val entryDate: LocalDateTime = LocalDateTime.now(),
    val notes: String? = null,
    
    // These fields are not stored but calculated
    val cryptoSymbol: String? = null,
    val cryptoName: String? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val currentPrice: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val currentValue: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val profitLoss: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val profitLossPercentage: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val totalValue: BigDecimal
) 