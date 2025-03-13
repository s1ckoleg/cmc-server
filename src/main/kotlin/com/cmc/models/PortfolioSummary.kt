package com.cmc.models

import com.cmc.utils.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class PortfolioSummary(
    val totalInvestment: @Serializable(with = BigDecimalSerializer::class) BigDecimal,
    val totalCurrentValue: @Serializable(with = BigDecimalSerializer::class) BigDecimal,
    val totalProfitLoss: @Serializable(with = BigDecimalSerializer::class) BigDecimal,
    val totalProfitLossPercentage: @Serializable(with = BigDecimalSerializer::class) BigDecimal,
    val entries: List<PortfolioEntry>
) 