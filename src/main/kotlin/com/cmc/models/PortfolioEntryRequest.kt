package com.cmc.models

import com.cmc.utils.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class PortfolioEntryRequest(
    val cryptoId: Int,
    @Serializable(with = BigDecimalSerializer::class)
    val quantity: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val entryPrice: BigDecimal,
    val notes: String? = null
) 