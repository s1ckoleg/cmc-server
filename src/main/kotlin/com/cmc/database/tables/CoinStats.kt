package com.cmc.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.javatime.date
import java.time.LocalDateTime
import java.time.LocalDate

object CoinStats : Table("coinstats") {
    val id = integer("id").autoIncrement()
    val coinId = integer("coin_id").references(Coins.id)
    val currentPrice = decimal("current_price", 20, 8)
    val marketCap = decimal("market_cap", 30, 2).nullable()
    val volume24h = decimal("volume_24h", 30, 2).nullable()
    val date = datetime("date").default(LocalDateTime.now())
    
    override val primaryKey = PrimaryKey(id)
    
    // Create a composite index on coinId and date to efficiently query historical data
    init {
        index(isUnique = true, coinId, date)
    }
} 