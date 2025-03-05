package com.cmc.models

import com.cmc.utils.BigDecimalSerializer
import com.cmc.utils.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDateTime

@Serializable
data class CoinWithStats(
    val id: Int,
    val ticker: String,
    val name: String,
    @Serializable(with = BigDecimalSerializer::class)
    val currentPrice: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val marketCap: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val volume24h: BigDecimal? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val date: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun fromCoinAndStats(coin: Coin, stats: CoinStats?): CoinWithStats {
            return CoinWithStats(
                id = coin.id,
                ticker = coin.ticker,
                name = coin.name,
                currentPrice = stats?.currentPrice ?: BigDecimal.ZERO,
                marketCap = stats?.marketCap,
                volume24h = stats?.volume24h,
                date = stats?.date ?: LocalDateTime.now()
            )
        }
    }
} 